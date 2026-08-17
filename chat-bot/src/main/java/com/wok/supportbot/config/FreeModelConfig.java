package com.wok.supportbot.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 免费模型白名单配置
 * 限制只能使用阿里云免费额度内的模型，避免产生费用
 */
@Configuration
@ConfigurationProperties(prefix = "free-models")
@Slf4j
public class FreeModelConfig {

    private List<String> chat;
    private List<String> embedding;

    @Value("${spring.ai.dashscope.chat.options.model}")
    private String currentChatModel;

    @Value("${spring.ai.dashscope.embedding.options.model}")
    private String currentEmbeddingModel;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    public List<String> getChat() {
        return chat;
    }

    public void setChat(List<String> chat) {
        this.chat = chat;
    }

    public List<String> getEmbedding() {
        return embedding;
    }

    public void setEmbedding(List<String> embedding) {
        this.embedding = embedding;
    }

    public String getCurrentChatModel() {
        return currentChatModel;
    }

    public String getCurrentEmbeddingModel() {
        return currentEmbeddingModel;
    }

    public boolean isApiKeyConfigured() {
        return apiKey != null && !apiKey.isEmpty();
    }

    /**
     * 启动时校验当前配置的模型是否在白名单中
     */
    @PostConstruct
    public void validate() {
        log.info("================================================");
        log.info("  苏福万家 AI 模型配置校验");
        log.info("================================================");
        log.info("当前对话模型: {}", currentChatModel);
        log.info("当前向量模型: {}", currentEmbeddingModel);
        log.info("API Key 状态: {}", isApiKeyConfigured() ? "✅ 已配置" : "❌ 未配置");
        log.info("允许的对话模型: {}", chat);
        log.info("允许的向量模型: {}", embedding);

        if (!isApiKeyConfigured()) {
            log.error("❌ 阿里云 API Key 未配置！请设置 DASHSCOPE_API_KEY 环境变量");
            throw new IllegalStateException("阿里云 API Key 未配置");
        }

        boolean allValid = true;

        if (chat != null && !chat.isEmpty()) {
            if (!chat.contains(currentChatModel)) {
                log.error("❌ 对话模型 [{}] 不在免费白名单中！", currentChatModel);
                log.error("允许的对话模型: {}", chat);
                allValid = false;
            } else {
                log.info("✅ 对话模型 [{}] 校验通过", currentChatModel);
            }
        }

        if (embedding != null && !embedding.isEmpty()) {
            if (!embedding.contains(currentEmbeddingModel)) {
                log.error("❌ 向量模型 [{}] 不在免费白名单中！", currentEmbeddingModel);
                log.error("允许的向量模型: {}", embedding);
                allValid = false;
            } else {
                log.info("✅ 向量模型 [{}] 校验通过", currentEmbeddingModel);
            }
        }

        if (!allValid) {
            log.error("================================================");
            log.error("❌ 模型校验失败！应用已阻止启动！");
            log.error("请修改 DASHSCOPE_MODEL 和 DASHSCOPE_EMBEDDING_MODEL 环境变量");
            log.error("================================================");
            throw new IllegalStateException(
                    String.format("模型校验失败！对话模型=[%s](允许=%s), 向量模型=[%s](允许=%s)",
                            currentChatModel, chat, currentEmbeddingModel, embedding)
            );
        }

        log.info("================================================");
        log.info("  ✅ 所有模型校验通过，免费模型白名单已生效");
        log.info("  💡 提示: 如需更换模型，请设置 DASHSCOPE_MODEL 环境变量");
        log.info("================================================");
    }

    /**
     * 检查模型是否在免费白名单中
     */
    public boolean isFreeChatModel(String model) {
        return chat != null && chat.contains(model);
    }

    public boolean isFreeEmbeddingModel(String model) {
        return embedding != null && embedding.contains(model);
    }

    /**
     * 获取推荐的免费对话模型（白名单中的第一个）
     */
    public String getRecommendedChatModel() {
        return (chat != null && !chat.isEmpty()) ? chat.get(0) : currentChatModel;
    }

    public String getRecommendedEmbeddingModel() {
        return (embedding != null && !embedding.isEmpty()) ? embedding.get(0) : currentEmbeddingModel;
    }
}
