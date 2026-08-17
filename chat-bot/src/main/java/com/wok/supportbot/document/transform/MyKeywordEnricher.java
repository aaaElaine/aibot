package com.wok.supportbot.document.transform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 AI 的文档元信息增强器（为文档补充关键词元信息）
 * 注：会为每个文档片段调用一次 AI，上传大文件时会较慢
 */
@Slf4j
@Component
public class MyKeywordEnricher {

    @Autowired(required = false)
    private ChatModel dashscopeChatModel;

    /**
     * 使用 AI 提取关键词并添加到元数据
     */
    public List<Document> enrichDocuments(List<Document> documents) {
        if (dashscopeChatModel == null) {
            log.warn("ChatModel 未配置，跳过关键词提取");
            return documents;
        }
        if (documents == null || documents.isEmpty()) {
            return documents;
        }
        try {
            log.info("开始 AI 关键词提取，文档片段数: {}", documents.size());
            long start = System.currentTimeMillis();
            KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(dashscopeChatModel, 5);
            List<Document> enriched = keywordMetadataEnricher.apply(documents);
            log.info("AI 关键词提取完成，耗时: {}ms", System.currentTimeMillis() - start);
            return enriched;
        } catch (Exception e) {
            log.warn("关键词提取失败，使用原始文档", e);
            return documents;
        }
    }
}
