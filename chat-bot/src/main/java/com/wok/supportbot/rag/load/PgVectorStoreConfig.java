package com.wok.supportbot.rag.load;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 向量数据库配置
 * - rag.enabled=true: 使用 SimpleVectorStore（内存向量存储，无需 PostgreSQL）
 * - rag.enabled=false: 使用 NoOpVectorStore（空实现）
 */
@Configuration
public class PgVectorStoreConfig {

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
    public VectorStore vectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        return SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
    }

    @Bean
    @Primary
    @ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "false", matchIfMissing = true)
    public VectorStore noOpVectorStore() {
        return new NoOpVectorStore();
    }
}
