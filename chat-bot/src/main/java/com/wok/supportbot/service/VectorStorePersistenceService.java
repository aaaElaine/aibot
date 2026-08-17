package com.wok.supportbot.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * SimpleVectorStore 持久化服务。
 *
 * <p>业务数据保存在 H2 中，而 SimpleVectorStore 默认只存在于内存。该服务负责在启动时
 * 加载索引，并在文档向量化完成后将索引原子写入磁盘。</p>
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class VectorStorePersistenceService {

    private final VectorStore vectorStore;
    private final Path storePath;
    private volatile boolean loadedFromDisk;

    public VectorStorePersistenceService(
            VectorStore vectorStore,
            @Value("${rag.vector-store-file:./data/vector-store.json}") String storeFile) {
        this.vectorStore = vectorStore;
        this.storePath = Path.of(storeFile).toAbsolutePath().normalize();
    }

    @PostConstruct
    public synchronized void load() {
        if (!(vectorStore instanceof SimpleVectorStore)) {
            log.info("当前 VectorStore 不需要本地持久化: {}", vectorStore.getClass().getSimpleName());
            return;
        }
        SimpleVectorStore simpleVectorStore = (SimpleVectorStore) vectorStore;
        if (!Files.isRegularFile(storePath)) {
            log.info("未找到持久化向量索引，将尝试从数据库恢复: {}", storePath);
            return;
        }

        try {
            simpleVectorStore.load(storePath.toFile());
            loadedFromDisk = true;
            log.info("已加载持久化向量索引: {}", storePath);
        } catch (Exception e) {
            loadedFromDisk = false;
            log.error("加载持久化向量索引失败，将尝试从数据库重建: {}", storePath, e);
        }
    }

    public synchronized void save() {
        if (!(vectorStore instanceof SimpleVectorStore)) {
            return;
        }
        SimpleVectorStore simpleVectorStore = (SimpleVectorStore) vectorStore;

        Path parent = storePath.getParent();
        Path temporaryPath = storePath.resolveSibling(storePath.getFileName() + ".tmp");
        try {
            if (parent != null) {
                Files.createDirectories(parent);
            }
            simpleVectorStore.save(temporaryPath.toFile());
            moveIntoPlace(temporaryPath);
            loadedFromDisk = true;
            log.info("向量索引已持久化: {}", storePath);
        } catch (Exception e) {
            log.error("持久化向量索引失败: {}", storePath, e);
            throw new IllegalStateException("持久化向量索引失败", e);
        }
    }

    private void moveIntoPlace(Path temporaryPath) throws IOException {
        try {
            Files.move(temporaryPath, storePath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporaryPath, storePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public boolean isLoadedFromDisk() {
        return loadedFromDisk;
    }
}
