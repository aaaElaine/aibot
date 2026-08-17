package com.wok.supportbot.rag.load;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 向量数据库配置（已合并到 PgVectorStoreConfig）
 * 保留此类避免编译错误，不再注册额外的 Bean
 */
public class InMemoryVectorStoreConfig {
}
