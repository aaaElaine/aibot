package com.wok.supportbot.controller;

import com.wok.supportbot.config.FreeModelConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 模型管理控制器
 * 提供查看当前模型配置和可用免费模型列表的接口
 */
@RestController
@RequestMapping("/api/models")
@Slf4j
public class ModelController {

    private final FreeModelConfig freeModelConfig;

    @Value("${spring.ai.dashscope.chat.options.model}")
    private String currentChatModel;

    @Value("${spring.ai.dashscope.embedding.options.model}")
    private String currentEmbeddingModel;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    public ModelController(FreeModelConfig freeModelConfig) {
        this.freeModelConfig = freeModelConfig;
    }

    /**
     * 获取完整模型状态（用于快速排查欠费问题）
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> result = new HashMap<>();

        result.put("status", "ok");
        result.put("apiKeyConfigured", freeModelConfig.isApiKeyConfigured());

        Map<String, Object> chatInfo = new HashMap<>();
        chatInfo.put("current", currentChatModel);
        chatInfo.put("isFree", freeModelConfig.isFreeChatModel(currentChatModel));
        chatInfo.put("recommended", freeModelConfig.getRecommendedChatModel());
        result.put("chatModel", chatInfo);

        Map<String, Object> embeddingInfo = new HashMap<>();
        embeddingInfo.put("current", currentEmbeddingModel);
        embeddingInfo.put("isFree", freeModelConfig.isFreeEmbeddingModel(currentEmbeddingModel));
        embeddingInfo.put("recommended", freeModelConfig.getRecommendedEmbeddingModel());
        result.put("embeddingModel", embeddingInfo);

        result.put("freeChatModels", freeModelConfig.getChat());
        result.put("freeEmbeddingModels", freeModelConfig.getEmbedding());

        boolean allFree = freeModelConfig.isFreeChatModel(currentChatModel) 
                && freeModelConfig.isFreeEmbeddingModel(currentEmbeddingModel);
        result.put("allModelsFree", allFree);

        if (!allFree) {
            result.put("warning", "⚠️ 当前使用的模型不在免费白名单中，可能产生费用！");
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前模型配置和可用免费模型列表
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getModelConfig() {
        Map<String, Object> result = new HashMap<>();

        Map<String, Object> currentModels = new HashMap<>();
        currentModels.put("chat", currentChatModel);
        currentModels.put("embedding", currentEmbeddingModel);

        Map<String, Object> freeModels = new HashMap<>();
        freeModels.put("chat", freeModelConfig.getChat());
        freeModels.put("embedding", freeModelConfig.getEmbedding());

        result.put("current", currentModels);
        result.put("free", freeModels);
        result.put("isFreeChatModel", freeModelConfig.isFreeChatModel(currentChatModel));
        result.put("isFreeEmbeddingModel", freeModelConfig.isFreeEmbeddingModel(currentEmbeddingModel));

        return ResponseEntity.ok(result);
    }

    /**
     * 获取免费对话模型列表
     */
    @GetMapping("/free-chat-models")
    public ResponseEntity<List<String>> getFreeChatModels() {
        return ResponseEntity.ok(freeModelConfig.getChat());
    }

    /**
     * 获取免费向量模型列表
     */
    @GetMapping("/free-embedding-models")
    public ResponseEntity<List<String>> getFreeEmbeddingModels() {
        return ResponseEntity.ok(freeModelConfig.getEmbedding());
    }

    /**
     * 验证模型是否在免费白名单中
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateModel(
            @RequestParam String model,
            @RequestParam(defaultValue = "chat") String type) {

        Map<String, Object> result = new HashMap<>();
        boolean isFree;

        if ("embedding".equals(type)) {
            isFree = freeModelConfig.isFreeEmbeddingModel(model);
        } else {
            isFree = freeModelConfig.isFreeChatModel(model);
        }

        result.put("model", model);
        result.put("type", type);
        result.put("isFree", isFree);
        result.put("message", isFree ? "✅ 该模型在免费白名单中" : "❌ 该模型不在免费白名单中，可能产生费用");

        return ResponseEntity.ok(result);
    }
}
