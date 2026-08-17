package com.wok.supportbot;

import com.wok.supportbot.config.VectorStoreStartupInitializer;
import com.wok.supportbot.document.transform.MyTokenTextSplitter;
import com.wok.supportbot.entity.Document;
import com.wok.supportbot.repository.DocumentRepository;
import com.wok.supportbot.service.VectorStorePersistenceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorStoreStartupInitializerTest {

    @Mock
    private VectorStorePersistenceService persistenceService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private MyTokenTextSplitter tokenTextSplitter;
    @Mock
    private VectorStore vectorStore;
    @InjectMocks
    private VectorStoreStartupInitializer initializer;

    @Test
    void restoresCompletedDocumentsWhenPersistedIndexIsMissing() {
        Document document = new Document();
        document.setId(67L);
        document.setKbId(33L);
        document.setTitle("知识库文档.pdf");
        document.setContent("知识库正文");
        org.springframework.ai.document.Document chunk =
                new org.springframework.ai.document.Document("知识库正文");

        when(persistenceService.isLoadedFromDisk()).thenReturn(false);
        when(documentRepository.selectList(any())).thenReturn(List.of(document));
        when(tokenTextSplitter.splitDocuments(any())).thenReturn(List.of(chunk));

        initializer.restoreWhenNecessary();

        verify(vectorStore).add(List.of(chunk));
        verify(persistenceService).save();
    }

    @Test
    void skipsRebuildWhenPersistedIndexWasLoaded() {
        when(persistenceService.isLoadedFromDisk()).thenReturn(true);

        initializer.restoreWhenNecessary();

        verify(documentRepository, never()).selectList(any());
        verify(vectorStore, never()).add(any());
        verify(persistenceService, never()).save();
    }
}
