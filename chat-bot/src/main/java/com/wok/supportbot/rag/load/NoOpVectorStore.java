package com.wok.supportbot.rag.load;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.Collections;
import java.util.List;

/**
 * 空实现的向量存储，用于 RAG 禁用时的降级处理
 */
public class NoOpVectorStore implements VectorStore {

    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        return Collections.emptyList();
    }

    @Override
    public void add(List<Document> documents) {
        // 无操作
    }

    @Override
    public void delete(List<String> idList) {
        // 无操作
    }

    @Override
    public void delete(Filter.Expression filterExpression) {
        // 无操作
    }
}
