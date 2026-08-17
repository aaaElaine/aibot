package com.wok.supportbot.app;

import cn.hutool.json.JSONUtil;
import com.wok.supportbot.advisor.MyLoggerAdvisor;
import com.wok.supportbot.advisor.ReReadingAdvisor;
import com.wok.supportbot.dto.response.ChatResponseDTO;
import com.wok.supportbot.entity.Product;
import com.wok.supportbot.rag.preretrieval.CompressionQueryRewriter;
import com.wok.supportbot.rag.preretrieval.MultiQueryExpanderRewriter;
import com.wok.supportbot.rag.preretrieval.RewriteQueryRewriter;
import com.wok.supportbot.rag.preretrieval.TranslationQueryRewriter;
import com.wok.supportbot.service.ModelFailoverService;
import com.wok.supportbot.service.NewsService;
import com.wok.supportbot.service.ProductService;
import com.wok.supportbot.service.WeatherService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

/**
 * @Classname AssistantApp
 * @Description
 * @Version 1.0.0
 * @Date 2025/06/27 14:11
 * @Author lyx
 */
@Component
@Slf4j
public class AssistantApp {

    // 对话上下文缓存：chatId -> 最近的工具查询信息
    private final Map<String, ConversationContext> conversationContextCache = new HashMap<>();

    @Resource
    private VectorStore pgVectorVectorStore;

    @Resource
    private ProductService productService;

    @Resource
    private WeatherService weatherService;

    @Resource
    private NewsService newsService;

    @Resource
    private ModelFailoverService modelFailoverService;

    private final ChatClient chatClient;
    private final boolean ragEnabled;

    private static final String SYSTEM_PROMPT = "你是姜姜，这个平台的AI助手。" +
            "你能回答的问题取决于平台维护的知识库内容。" +
            "此外，天气查询和新闻资讯是固定功能，不受知识库限制。" +
            "" +
            "【自我介绍】" +
            "当用户问你是谁、你叫什么等问题时，请这样回答：" +
            "我是姜姜😊 这个平台的AI助手~ 我能回答的问题取决于平台维护的知识库内容，有什么想了解的尽管问我吧！" +
            "" +
            "【日常问候】" +
            "当用户只是说你好、哈喽、hi等简单问候时，请友好回应：" +
            "你好呀😊 有什么可以帮你的吗？" +
            "不要在日常问候时自动介绍自己的身份和功能。" +
            "" +
            "【回答原则】" +
            "1. 知识库有的内容就回答，没有的就说不知道" +
            "2. 不知道时说：「不好意思，暂时还没这块知识呢😅」" +
            "3. 语气自然，像朋友聊天" +
            "" +
            "【说话风格】" +
            "1. 口语化：好的呀、稍等、我看看哈" +
            "2. 简洁明了，不要啰嗦" +
            "3. 适当用😊✨等emoji，每段最多1-2个" +
            "4. 避免「很高兴为您服务」等生硬套话";

    private final QuestionAnswerAdvisor questionAnswerAdvisor;

    /**
     * 初始化 ChatClient
     *
     * @param dashscopeChatModel
     */
    public AssistantApp(ChatModel dashscopeChatModel, ChatMemory chatMemory, VectorStore vectorStore,
                        @org.springframework.beans.factory.annotation.Value("${rag.enabled:false}") boolean ragEnabled) {
        this.ragEnabled = ragEnabled;
        log.info("RAG 知识库功能: {}", ragEnabled ? "已启用" : "已禁用（使用纯对话模式）");

        // 初始化 RAG 知识库问答 Advisor
        // topK=3：只取最相关的 3 个片段，平衡召回率和速度
        // similarityThreshold=0.5：提高阈值，过滤不相关内容
        this.questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().similarityThreshold(0.5).topK(3).build())
                .build();

        // 根据 RAG 开关配置默认 advisors
        List<Advisor> advisorsList = new ArrayList<>();
        advisorsList.add(new MessageChatMemoryAdvisor(chatMemory));
        if (ragEnabled) {
            advisorsList.add(questionAnswerAdvisor);
        }
        advisorsList.add(new MyLoggerAdvisor());

        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(advisorsList.toArray(new Advisor[0]))
                .build();
    }

    // 欢迎语关键词列表（用于检测 AI 是否返回了无关的欢迎语）
    private static final List<String> GREETING_PATTERNS = Arrays.asList(
            "很高兴为您服务", "有什么可以帮您", "可以向我咨询", "也可以问我",
            "智能客服", "苏福万家", "为您提供", "竭诚为您"
    );

    // 自我介绍问题关键词（用户问这类问题时，AI可以介绍自己）
    private static final List<String> SELF_INTRO_PATTERNS = Arrays.asList(
            "你是谁", "你叫什么", "你是什么", "你是哪个",
            "介绍一下你自己", "自我介绍", "你是干嘛的", "你是做什么的",
            "who are you", "what are you"
    );

    // 日常问候关键词（用户只是打招呼，AI应友好回应）
    private static final List<String> SIMPLE_GREETING_PATTERNS = Arrays.asList(
            "你好", "您好", "哈喽", "hello", "hi", "嘿", "喂", "在吗", "在么",
            "早", "早上好", "晚安", "午安"
    );

    /**
     * AI 基础对话（支持多轮对话记忆 + RAG 知识库检索）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        String conversationId = (chatId != null && !chatId.isEmpty()) ? chatId : java.util.UUID.randomUUID().toString();
        log.info("开始对话，message={}, chatId={}", message, conversationId);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .call()
                .chatResponse();
        String response = chatResponse.getResult().getOutput().getText();
        log.info("对话完成，response={}", response);

        // 后处理：检测 AI 是否返回了无关的欢迎语
        if (isGreetingResponse(response, message)) {
            log.warn("检测到 AI 返回欢迎语，替换为知识库无相关提示。message={}, response={}", message, response);
            return buildNoKnowledgeResponse(message);
        }

        return response;
    }

    /**
     * AI 基础对话（支持多轮对话记忆 + RAG 知识库检索，SSE 流式传输）
     *
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        String conversationId = (chatId != null && !chatId.isEmpty()) ? chatId : java.util.UUID.randomUUID().toString();
        log.info("开始流式对话，message={}, chatId={}", message, conversationId);
        return chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                .stream()
                .content();
    }

    /**
     * 检测 AI 回复是否为欢迎语（与用户问题无关的通用回复）
     *
     * @param response AI 返回的回复
     * @param message  用户原始消息
     * @return 是否为欢迎语
     */
    private boolean isGreetingResponse(String response, String message) {
        if (response == null || response.trim().isEmpty()) {
            return true;
        }

        // 如果用户的问题是关于自我介绍的，不触发欢迎语替换
        if (isSelfIntroductionQuestion(message)) {
            return false;
        }

        String lowerResponse = response.toLowerCase();

        // 计算欢迎语关键词命中数
        int greetingHits = 0;
        for (String pattern : GREETING_PATTERNS) {
            if (lowerResponse.contains(pattern)) {
                greetingHits++;
            }
        }

        // 如果命中 3 个及以上欢迎语关键词，直接判定为欢迎语（典型的欢迎语特征）
        if (greetingHits >= 3) {
            return true;
        }

        // 如果命中 2 个欢迎语关键词，进一步判断
        if (greetingHits >= 2) {
            // 检查是否包含典型的欢迎语模式：如"可以向我咨询...也可以问我"
            if (lowerResponse.contains("可以向我咨询") && lowerResponse.contains("也可以问我")) {
                return true;
            }
            // 如果回复很短（< 100 字）且主要是欢迎语内容，直接判定
            if (response.length() < 100) {
                return true;
            }
        }

        return false;
    }

    /**
     * 检测用户是否在问自我介绍类问题
     */
    private boolean isSelfIntroductionQuestion(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String trimmedMessage = message.trim().toLowerCase();

        // 检查完整的自我介绍关键词
        for (String pattern : SELF_INTRO_PATTERNS) {
            if (trimmedMessage.equals(pattern) || trimmedMessage.contains(pattern)) {
                return true;
            }
        }

        // 对于很短的消息（<= 5个字符），检查是否是简单问候
        if (trimmedMessage.length() <= 5) {
            for (String pattern : SIMPLE_GREETING_PATTERNS) {
                if (trimmedMessage.equals(pattern) || trimmedMessage.contains(pattern)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 构建知识库无相关内容的友好提示
     *
     * @param message 用户原始消息
     * @return 友好提示文本
     */
    private String buildNoKnowledgeResponse(String message) {
        return "不好意思，暂时还没这块知识呢😅 你可以试试换个问题问我，或者联系人工客服哦～";
    }

    // AI 恋爱知识库问答功能
    @Resource
    RewriteQueryRewriter rewriteQueryRewriter;
    @Resource
    CompressionQueryRewriter compressionQueryRewriter;
    @Resource
    MultiQueryExpanderRewriter multiQueryExpanderRewriter;
    @Resource
    TranslationQueryRewriter translationQueryRewriter;


    /**
     * 和 RAG 知识库进行对话（使用查询重写优化）
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRag(String message, String chatId) {
        // 在预检索阶段，系统接收用户的原始查询，通过查询转换和查询扩展等方法对其进行优化
        String rewrittenMessage = rewriteQueryRewriter.doQueryRewrite(message);

        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 应用 RAG 知识库问答
                .advisors(questionAnswerAdvisor)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }


    @Autowired
    private List<QueryTransformer> queryTransformers;
    @Autowired
    private MultiQueryExpander multiQueryExpander;

    /**
     * 和 RAG 知识库进行对话(另外一种使用方式)
     *
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithRagEnhance(String message, String chatId) {
        Advisor retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
                // todo 不生效
                //.queryTransformers(queryTransformers)
                //.queryExpander(multiQueryExpander)
                .documentRetriever(VectorStoreDocumentRetriever.builder()
                        .vectorStore(pgVectorVectorStore)
                        .similarityThreshold(0.5)
                        .topK(4)
                        .build())
                .queryAugmenter(ContextualQueryAugmenter.builder()
                        .allowEmptyContext(false) // 不允许模型在没有找到相关文档的情况下也生成回答
                        .build())
                .build();

        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, chatId)
                        .param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, 10))
                // 应用 RAG 知识库问答
                .advisors(retrievalAugmentationAdvisor)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    // 商品相关关键词
    private static final List<String> PRODUCT_KEYWORDS = Arrays.asList(
            "买", "购买", "商品", "产品", "推荐", "想买", "需要", "有没有", "推荐下",
            "适合", "好用", "热销", "便宜", "价格", "多少钱", "优惠", "活动",
            "扶手", "护理床", "助行器", "手环", "膳食", "按摩", "理疗仪", "马桶",
            "助听器", "血糖仪", "轮椅", "改造", "旅游", "课程", "服务", "上门"
    );

    /**
     * 带商品推荐的对话
     *
     * @param message 用户消息
     * @param chatId  会话ID
     * @return 包含文本和商品卡片的响应
     */
    public ChatResponseDTO doChatWithProduct(String message, String chatId) {
        // 先获取AI回复
        String aiResponse = doChat(message, chatId);

        // 检测是否包含商品相关意图
        List<ChatResponseDTO.ProductCard> productCards = new ArrayList<>();
        if (containsProductIntent(message)) {
            log.info("检测到商品相关意图，开始搜索商品，关键词：{}", message);
            List<Product> products = productService.searchProducts(extractSearchKeyword(message), 5);
            for (Product product : products) {
                ChatResponseDTO.ProductCard card = new ChatResponseDTO.ProductCard();
                card.setId(product.getId());
                card.setName(product.getName());
                card.setDescription(product.getDescription());
                card.setPrice(product.getPrice() != null ? product.getPrice().toString() : null);
                card.setOriginalPrice(product.getOriginalPrice() != null ? product.getOriginalPrice().toString() : null);
                card.setImageUrl(product.getImageUrl());
                card.setCategory(product.getCategory());
                card.setProductUrl(product.getProductUrl());
                card.setSalesCount(product.getSalesCount());
                productCards.add(card);
            }
        }

        ChatResponseDTO dto = new ChatResponseDTO();
        dto.setText(aiResponse);
        dto.setProducts(productCards);
        return dto;
    }

    /**
     * 检测用户消息是否包含商品相关意图
     */
    private boolean containsProductIntent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        int matchCount = 0;
        for (String keyword : PRODUCT_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                matchCount++;
                if (matchCount >= 1) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 从用户消息中提取搜索关键词
     */
    private String extractSearchKeyword(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "";
        }
        // 先移除更长的词组，再移除单个字
        String keyword = message
                .replaceAll("怎么|如何|什么|哪些|推荐下|推荐|请问|给我|想买|我想|看看|查看|一下|帮我", "")
                .replaceAll("呢|吗|的|了|啊|吧|要|去|买|想|看|找|给|我|你", "")
                .trim();
        // 如果太短，直接返回原消息
        if (keyword.length() < 2) {
            return message.trim();
        }
        return keyword;
    }

    /**
     * 带商品推荐的对话（返回JSON字符串）
     */
    public String doChatWithProductAsJson(String message, String chatId) {
        ChatResponseDTO dto = doChatWithProduct(message, chatId);
        return JSONUtil.toJsonStr(dto);
    }

    // 天气查询意图关键词（只有查询意图才触发天气卡片）
    private static final List<String> WEATHER_QUERY_KEYWORDS = Arrays.asList(
            "天气怎么样", "天气如何", "天气预报", "天气查询",
            "今天天气", "明天天气", "后天天气", "这里天气", "那边天气",
            "查天气", "看天气", "天气报", "天气情况",
            "气温多少", "温度多少", "多少度", "下雨吗", "下雪吗",
            "热不热", "冷不冷", "穿什么", "带伞", "适合出门", "适合旅游",
            "天气好吗", "天气咋样", "天气好不好", "天气怎么样啊"
    );

    // 天气评价/抱怨关键词（不触发天气卡片，走普通对话）
    private static final List<String> WEATHER_EVALUATION_KEYWORDS = Arrays.asList(
            "天气不好", "天气太差", "天气不好", "天气糟糕", "天气不行",
            "天气太热", "天气太冷", "天气真热", "天气真冷",
            "天气下雨", "天气刮风", "天气不好玩",
            "下雨了", "下雪了", "打雷了", "刮风了",
            "好天气", "天气不错", "天气真好", "天气真好啊",
            "天气不怎么样", "天气一般", "天气凑合",
            "下雨", "下雪", "刮风", "打雷", "出太阳"
    );

    // 纯天气关键词（用于兜底检测，只有搭配查询意图才触发）
    private static final List<String> WEATHER_GENERIC_KEYWORDS = Arrays.asList(
            "天气", "气温", "温度"
    );

    // 日期描述关键词（用于从消息中提取日期）
    private static final List<String> DATE_KEYWORDS = Arrays.asList(
            "今天", "今日", "明天", "明日", "后天", "大后天", "昨天", "昨日",
            "周末", "本周", "下周", "这周",
            "周一", "周二", "周三", "周四", "周五", "周六", "周日", "星期日",
            "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期天",
            "号", "日"
    );

    // 完整日期正则：2026年8月20日 / 2026-08-20 / 2026/8/20
    private static final Pattern PATTERN_FULL_DATE = Pattern.compile("(\\d{4})[年\\-/.](\\d{1,2})[月\\-/.](\\d{1,2})[日号]?");
    // 简短日期正则：8月20日 / 8/20 / 8.15 / 8、15
    private static final Pattern PATTERN_SHORT_DATE = Pattern.compile("(\\d{1,2})[月/、.](\\d{1,2})[日号]?");

    // 新闻相关关键词
    private static final List<String> NEWS_KEYWORDS = Arrays.asList(
            "新闻", "资讯", "头条", "热点", "时事", "最近发生", "最新消息",
            "新闻top", "新闻排行", "最新新闻", "看看新闻", "有什么新闻"
    );

    /**
     * 检测消息中的工具意图
     * 优化：区分"查询天气"和"评价天气"
     * - 明确查询意图（天气怎么样、查天气等）→ 触发天气工具
     * - 评价/抱怨（天气不好、下雨了等）→ 不触发，走普通对话
     * - 通用词"天气" + 城市名/日期 → 触发天气工具
     *
     * @return 工具类型：weather, news, 或 null
     */
    private String detectToolIntent(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        String lowerMessage = message.toLowerCase();

        // 1. 检测明确的天气查询意图
        for (String keyword : WEATHER_QUERY_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                log.info("天气查询意图命中: keyword={}", keyword);
                return "weather";
            }
        }

        // 2. 检测天气评价/抱怨 → 不触发天气工具
        for (String keyword : WEATHER_EVALUATION_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                log.info("天气评价/抱怨 → 不触发天气工具: keyword={}", keyword);
                return null;
            }
        }

        // 3. 通用"天气"关键词 + 城市名 → 视为天气查询
        for (String keyword : WEATHER_GENERIC_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                boolean hasCity = extractCity(message) != null;
                boolean hasDate = looksLikeDate(message);
                if (hasCity || hasDate) {
                    log.info("通用天气关键词 + 城市/日期 → 触发天气工具: keyword={}, city={}, date={}", keyword, hasCity, hasDate);
                    return "weather";
                }
                // 只有"天气"两个字，无城市无日期 → 可能是评价，不触发
                log.info("只有通用天气关键词，无城市无日期 → 不触发: message={}", message);
                return null;
            }
        }

        // 4. 检测新闻意图
        for (String keyword : NEWS_KEYWORDS) {
            if (lowerMessage.contains(keyword)) {
                return "news";
            }
        }

        return null;
    }

    /**
     * 从消息中提取城市名
     */
    private String extractCity(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }
        // 常见城市名列表
        List<String> cities = Arrays.asList(
                "北京", "上海", "广州", "深圳", "杭州", "成都", "武汉",
                "西安", "南京", "重庆", "天津", "苏州", "长沙", "郑州",
                "青岛", "厦门", "宁波", "无锡", "合肥", "福州", "济南",
                "沈阳", "大连", "昆明", "哈尔滨", "郑州", "石家庄"
        );

        for (String city : cities) {
            if (message.contains(city)) {
                return city;
            }
        }
        return null;
    }

    /**
     * 从消息中提取日期描述
     * 支持：今天、明天、后天、2026-08-20、8月20日、下周一 等
     */
    private String extractDateDesc(String message) {
        if (message == null || message.trim().isEmpty()) {
            return null;
        }

        // 优先尝试匹配完整日期格式
        Matcher fullDateMatcher = PATTERN_FULL_DATE.matcher(message);
        if (fullDateMatcher.find()) {
            return fullDateMatcher.group(0); // 返回匹配到的完整日期字符串
        }

        // 尝试匹配简短日期格式（如 "8月20日"）
        Matcher shortDateMatcher = PATTERN_SHORT_DATE.matcher(message);
        if (shortDateMatcher.find()) {
            return shortDateMatcher.group(0); // 返回匹配到的简短日期字符串
        }

        // 尝试匹配关键词
        for (String keyword : DATE_KEYWORDS) {
            if (message.contains(keyword)) {
                return keyword;
            }
        }

        return null;
    }

    /**
     * 检测消息是否包含日期（用于上下文补全）
     */
    private boolean looksLikeDate(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        // 匹配完整日期
        if (PATTERN_FULL_DATE.matcher(message).find()) {
            return true;
        }
        // 匹配简短日期（如 8月15日、8.15、8/15）
        if (PATTERN_SHORT_DATE.matcher(message).find()) {
            return true;
        }
        // 匹配日期关键词（今天、明天、后天等）
        for (String keyword : DATE_KEYWORDS) {
            if (message.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 带工具调用的对话（天气/新闻 + 商品推荐）
     *
     * @param message 用户消息
     * @param chatId  会话ID
     * @return 包含文本、商品卡片和工具结果的响应
     */
    public ChatResponseDTO doChatWithTools(String message, String chatId) {
        // 检测工具意图
        String toolType = detectToolIntent(message);
        log.info("检测到工具意图：{}, message={}", toolType, message);

        // 获取对话上下文
        ConversationContext ctx = getContext(chatId);

        // 上下文补全：如果当前消息没有明确的工具意图，但历史对话有
        if (toolType == null && ctx != null) {
            // 用户只说了城市名，但之前问过天气 → 视为天气查询
            if ("weather".equals(ctx.lastToolType) && looksLikeCityName(message)) {
                toolType = "weather";
                log.info("上下文补全：历史天气意图 + 城市名 → 天气查询");
            }
            // 用户只说了日期，但之前问过天气 → 视为天气查询
            else if ("weather".equals(ctx.lastToolType) && looksLikeDate(message)) {
                toolType = "weather";
                log.info("上下文补全：历史天气意图 + 日期 → 天气查询");
            }
            // 用户只说了城市名，但之前问过新闻 → 视为新闻查询
            else if ("news".equals(ctx.lastToolType) && looksLikeCityName(message)) {
                toolType = "news";
                log.info("上下文补全：历史新闻意图 + 城市名 → 新闻查询");
            }
        }

        ChatResponseDTO dto = new ChatResponseDTO();

        if ("weather".equals(toolType)) {
            // 处理天气查询
            String city = extractCity(message);
            // 上下文补全：如果当前消息没城市名，但之前查询过
            if (city == null && ctx != null && "weather".equals(ctx.lastToolType) && ctx.lastCity != null) {
                city = ctx.lastCity;
                log.info("上下文补全：使用历史城市 {}", city);
            }

            // 没有城市也没有上下文 → 礼貌询问
            if (city == null) {
                // 先保存上下文，让用户回复城市名时能触发天气查询
                updateContext(chatId, "weather", null);
                String askCityReply = "请问您想查询哪个城市的天气呢？😊";
                dto.setText(askCityReply);
                log.info("无城市信息，询问用户并保存天气上下文: message={}", message);
                return dto;
            }

            String dateDesc = extractDateDesc(message);
            Map<String, Object> weather;

            try {
                if (dateDesc != null) {
                    // 使用支持显式日期的新方法
                    weather = weatherService.getWeatherByDateDesc(city, dateDesc);
                    log.info("天气查询：城市={}, 日期描述={}", city, dateDesc);
                } else {
                    weather = weatherService.getWeather(city);
                }
            } catch (Exception e) {
                log.error("天气查询异常: {}", e.getMessage());
                weather = weatherService.getDefaultWeather(0);
            }

            // 更新上下文
            updateContext(chatId, "weather", weather.get("city").toString());

            // 构建简短AI回复
            String aiReply = buildWeatherBriefReply(message, weather);
            dto.setText(aiReply);

            // 添加工具结果
            Map<String, Object> toolResult = new HashMap<>();
            toolResult.put("type", "weather");
            toolResult.put("data", weather);
            dto.setToolResult(toolResult);

        } else if ("news".equals(toolType)) {
            // 处理新闻查询
            String city = extractCity(message);
            // 上下文补全
            if (city == null && ctx != null && "news".equals(ctx.lastToolType) && ctx.lastCity != null) {
                city = ctx.lastCity;
            }

            List<Map<String, Object>> news;
            String scope;
            try {
                if (city != null) {
                    news = newsService.getCityNews(city, 10);
                    scope = city;
                } else {
                    news = newsService.getNationalNews(10);
                    scope = "全国";
                }
            } catch (Exception e) {
                log.error("新闻查询异常: {}", e.getMessage());
                news = newsService.getNationalNews(10);
                scope = "全国";
            }

            // 更新上下文
            updateContext(chatId, "news", city);

            // 构建简短AI回复
            String aiReply = buildNewsBriefReply(scope);
            dto.setText(aiReply);

            // 添加工具结果
            Map<String, Object> toolResult = new HashMap<>();
            toolResult.put("type", "news");
            toolResult.put("scope", scope);
            toolResult.put("data", news);
            dto.setToolResult(toolResult);

        } else {
            // 普通对话
            try {
                String aiResponse = doChat(message, chatId);
                dto.setText(aiResponse);
            } catch (Exception e) {
                log.error("AI对话异常: {}", e.getMessage());
                // 使用专业的错误提示（区分故障类型）
                String errorMessage = modelFailoverService.formatServiceUnavailableMessage(e);
                dto.setText(errorMessage);
            }

            // 检测是否包含商品相关意图
            if (containsProductIntent(message)) {
                List<ChatResponseDTO.ProductCard> productCards = new ArrayList<>();
                List<Product> products = productService.searchProducts(extractSearchKeyword(message), 5);
                for (Product product : products) {
                    ChatResponseDTO.ProductCard card = new ChatResponseDTO.ProductCard();
                    card.setId(product.getId());
                    card.setName(product.getName());
                    card.setDescription(product.getDescription());
                    card.setPrice(product.getPrice() != null ? product.getPrice().toString() : null);
                    card.setOriginalPrice(product.getOriginalPrice());
                    card.setImageUrl(product.getImageUrl());
                    card.setCategory(product.getCategory());
                    card.setProductUrl(product.getProductUrl());
                    card.setSalesCount(product.getSalesCount());
                    productCards.add(card);
                }
                dto.setProducts(productCards);
            }
        }

        return dto;
    }

    /**
     * 构建天气简短回复（卡片展示详细信息，文字只做引导）
     */
    private String buildWeatherBriefReply(String message, Map<String, Object> weather) {
        // 检查是否为超出范围的结果
        Object outOfRange = weather.get("outOfRange");
        if (outOfRange != null && Boolean.TRUE.equals(outOfRange)) {
            Object msg = weather.get("message");
            if (msg != null) {
                return msg.toString();
            }
        }

        // 检查是否为未知城市
        Object weatherDesc = weather.get("weather");
        if ("未知".equals(weatherDesc)) {
            Object msg = weather.get("message");
            if (msg != null) {
                return msg.toString();
            }
        }

        StringBuilder sb = new StringBuilder();
        String cityName = weather.get("city").toString();
        String dateLabel = weather.get("dateLabel") != null ? weather.get("dateLabel").toString() : "今天";

        // 根据时间添加问候
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 6) {
            sb.append("凌晨好呀😊 ");
        } else if (hour < 12) {
            sb.append("早上好呀☀️ ");
        } else if (hour < 14) {
            sb.append("中午好呀😋 ");
        } else if (hour < 18) {
            sb.append("下午好呀😊 ");
        } else {
            sb.append("晚上好呀🌙 ");
        }

        sb.append("为您查询了").append(cityName).append(dateLabel).append("的天气情况~");
        return sb.toString();
    }

    /**
     * 构建新闻简短回复（卡片展示详细信息，文字只做引导）
     */
    private String buildNewsBriefReply(String scope) {
        StringBuilder sb = new StringBuilder();
        sb.append("您好呀😊 ");
        sb.append("为您整理了").append(scope != null && !scope.isEmpty() ? scope : "全国").append("最新资讯~");
        return sb.toString();
    }

    /**
     * 对话上下文信息
     */
    private static class ConversationContext {
        String lastToolType;    // 最近的工具类型: weather, news
        String lastCity;        // 最近查询的城市
        long lastUpdateTime;    // 最近更新时间

        ConversationContext(String toolType, String city) {
            this.lastToolType = toolType;
            this.lastCity = city;
            this.lastUpdateTime = System.currentTimeMillis();
        }

        boolean isExpired() {
            // 上下文有效期 10 分钟
            return System.currentTimeMillis() - lastUpdateTime > 10 * 60 * 1000;
        }
    }

    /**
     * 更新对话上下文
     */
    private void updateContext(String chatId, String toolType, String city) {
        if (chatId == null || chatId.isEmpty()) return;
        conversationContextCache.put(chatId, new ConversationContext(toolType, city));
        // 清理过期缓存
        conversationContextCache.entrySet().removeIf(e -> e.getValue().isExpired());
    }

    /**
     * 获取对话上下文
     */
    private ConversationContext getContext(String chatId) {
        if (chatId == null || chatId.isEmpty()) return null;
        ConversationContext ctx = conversationContextCache.get(chatId);
        if (ctx != null && ctx.isExpired()) {
            conversationContextCache.remove(chatId);
            return null;
        }
        return ctx;
    }

    /**
     * 判断消息是否像城市名（用于上下文补全）
     */
    private boolean looksLikeCityName(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        String trimmed = message.trim();
        // 去掉常见语气助词后缀：的、吧、啊、呀、哦、哈、呢等
        String stripped = trimmed.replaceAll("[的吧啊呀哦哈呢哦了哇啦]$", "").trim();
        if (stripped.isEmpty()) return false;
        // 直接匹配城市名（或简短的+后缀形式）
        if (extractCity(trimmed) != null) return true;
        if (extractCity(stripped) != null) return true;
        // 纯中文短文本（2-4字）也视为可能是城市名，交给extractCity去判定
        if (stripped.matches("[\\u4e00-\\u9fa5]{2,4}") && extractCity(stripped) != null) {
            return true;
        }
        return false;
    }
}
