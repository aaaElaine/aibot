package com.wok.supportbot.document.transform;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义基于 Token 的切词器
 * 优化：使用较大的 chunk size 减少片段数量，降低向量化调用次数
 */
@Component
public class MyTokenTextSplitter {

    /**
     * 使用优化参数分割文档。
     * chunkSize=800, minChunkSizeChars=350, minChunkLengthToEmbed=5, maxNumChunks=10000, keepSeparator=true
     * 相比默认值（500/350/5/10000），800 的 chunk size 能减少约 40% 的片段数，显著加快向量化
     */
    public List<Document> splitDocuments(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(800, 350, 5, 10000, true);
        return splitter.apply(documents);
    }

    /**
     * 使用自定义参数创建分割器
     */
    public List<Document> splitCustomized(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(200, 100, 10, 5000, true);
        return splitter.apply(documents);
    }
}
