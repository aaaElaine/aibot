package com.wok.supportbot.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wok.supportbot.document.transform.MyTokenTextSplitter;
import com.wok.supportbot.entity.Document;
import com.wok.supportbot.repository.DocumentRepository;
import com.wok.supportbot.service.VectorStorePersistenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 兼容升级前已经上传的文档：当磁盘上还没有向量索引时，从 H2 中保存的文档正文重建索引。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class VectorStoreStartupInitializer {

    private final VectorStorePersistenceService persistenceService;
    private final DocumentRepository documentRepository;
    private final MyTokenTextSplitter tokenTextSplitter;
    private final VectorStore vectorStore;

    @EventListener(ApplicationReadyEvent.class)
    public void restoreWhenNecessary() {
        if (persistenceService.isLoadedFromDisk()) {
            return;
        }

        LambdaQueryWrapper<Document> query = new LambdaQueryWrapper<>();
        query.eq(Document::getIsDelete, false)
                .eq(Document::getVectorStatus, "COMPLETED")
                .isNotNull(Document::getContent)
                .orderByAsc(Document::getId);
        List<Document> documents = documentRepository.selectList(query);

        if (documents.isEmpty()) {
            log.info("数据库中没有可恢复的已向量化文档");
            return;
        }

        log.info("磁盘向量索引不存在，开始从数据库恢复 {} 份文档", documents.size());
        int restoredDocuments = 0;
        int restoredChunks = 0;

        for (Document document : documents) {
            if (!StringUtils.hasText(document.getContent())) {
                continue;
            }
            try {
                Map<String, Object> metadata = new LinkedHashMap<>();
                metadata.put("documentId", document.getId());
                metadata.put("kbId", document.getKbId());
                metadata.put("title", document.getTitle());

                org.springframework.ai.document.Document aiDocument =
                        new org.springframework.ai.document.Document(document.getContent(), metadata);
                List<org.springframework.ai.document.Document> chunks =
                        tokenTextSplitter.splitDocuments(List.of(aiDocument));
                vectorStore.add(chunks);
                restoredDocuments++;
                restoredChunks += chunks.size();
            } catch (Exception e) {
                log.error("恢复文档向量失败: id={}, title={}", document.getId(), document.getTitle(), e);
            }
        }

        if (restoredDocuments > 0) {
            persistenceService.save();
        }
        log.info("知识库向量恢复完成，文档数: {}，片段数: {}", restoredDocuments, restoredChunks);
    }
}
