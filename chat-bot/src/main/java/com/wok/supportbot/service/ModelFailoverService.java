package com.wok.supportbot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 模型故障转移服务
 * 当API额度用完或服务不可用时，自动切换到下一个可用模型
 */
@Service
@Slf4j
public class ModelFailoverService {

    @Value("${model-failover.enabled:true}")
    private boolean failoverEnabled;

    @Value("${model-failover.chat-models:}")
    private List<String> chatModels;

    @Value("${spring.ai.dashscope.api-key:}")
    private String apiKey;

    @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String baseUrl;

    // 当前模型索引
    private final AtomicInteger currentModelIndex = new AtomicInteger(0);

    // 故障转移状态文件路径
    private static final String STATE_FILE = "data/model_failover_state.json";

    public ModelFailoverService() {
        loadState();
    }

    /**
     * 加载故障转移状态
     */
    private void loadState() {
        try {
            Path statePath = Paths.get(STATE_FILE);
            if (Files.exists(statePath)) {
                String content = Files.readString(statePath);
                // 简化解析：只需要当前索引
                int idx = Integer.parseInt(content.trim());
                currentModelIndex.set(idx);
                log.info("加载模型故障转移状态：当前索引={}", idx);
            }
        } catch (Exception e) {
            log.warn("加载模型故障转移状态失败：{}", e.getMessage());
        }
    }

    /**
     * 保存故障转移状态
     */
    private void saveState() {
        try {
            Files.createDirectories(Paths.get(STATE_FILE).getParent());
            Files.writeString(Paths.get(STATE_FILE), String.valueOf(currentModelIndex.get()));
        } catch (Exception e) {
            log.warn("保存模型故障转移状态失败：{}", e.getMessage());
        }
    }

    /**
     * 获取当前使用的模型名
     */
    public String getCurrentModel() {
        if (chatModels == null || chatModels.isEmpty()) {
            return "qwen-max";
        }
        int idx = Math.min(currentModelIndex.get(), chatModels.size() - 1);
        return chatModels.get(idx);
    }

    /**
     * 获取下一个模型
     */
    public String getNextModel() {
        if (chatModels == null || chatModels.isEmpty()) {
            return "qwen-max";
        }
        int nextIdx = (currentModelIndex.get() + 1) % chatModels.size();
        currentModelIndex.set(nextIdx);
        saveState();
        log.info("切换到下一个模型：{}", chatModels.get(nextIdx));
        return chatModels.get(nextIdx);
    }

    /**
     * 检查是否需要故障转移
     * 根据异常类型判断是否为额度用完或服务不可用
     */
    public boolean shouldFailover(Exception e) {
        if (e == null) return false;
        String message = e.getMessage();
        if (message == null) return false;

        // 429: 额度用完 / 请求过多
        // 403: 访问被拒（可能额度用完）
        // 500/502/503: 服务不可用
        return message.contains("429") ||
               message.contains("403") ||
               message.contains("quota") ||
               message.contains("rate limit") ||
               message.contains("500") ||
               message.contains("502") ||
               message.contains("503") ||
               message.contains("504") ||
               message.toLowerCase().contains("unavailable");
    }

    /**
     * 执行故障转移
     * @return 故障转移后的提示消息
     */
    public String performFailover() {
        if (!failoverEnabled) {
            return "抱歉，AI对话暂时不可用，晚点试试吧～";
        }

        if (chatModels == null || chatModels.size() <= 1) {
            return "抱歉，AI对话暂时不可用，晚点试试吧～";
        }

        String nextModel = getNextModel();
        log.info("故障转移：已切换到模型 {}", nextModel);

        return "抱歉，当前AI服务繁忙，已自动切换到备用服务，正在为您处理～";
    }

    /**
     * 重置到第一个模型
     */
    public void reset() {
        currentModelIndex.set(0);
        saveState();
        log.info("重置模型到第一个：{}", chatModels != null && !chatModels.isEmpty() ? chatModels.get(0) : "qwen-max");
    }

    /**
     * 获取所有可用模型列表
     */
    public List<String> getAllModels() {
        return chatModels != null ? chatModels : Collections.emptyList();
    }

    /**
     * 获取剩余可用模型数量
     */
    public int getRemainingModels() {
        if (chatModels == null) return 0;
        return Math.max(0, chatModels.size() - currentModelIndex.get() - 1);
    }

    /**
     * 格式化专业的AI服务不可用消息
     */
    public String formatServiceUnavailableMessage(Exception e) {
        if (e != null && shouldFailover(e)) {
            return performFailover();
        }

        // 通用错误
        return "抱歉，AI对话暂时不可用，晚点试试吧～";
    }
}
