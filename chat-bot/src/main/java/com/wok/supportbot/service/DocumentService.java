package com.wok.supportbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wok.supportbot.document.extract.TikaDocumentReader;
import com.wok.supportbot.document.transform.MyKeywordEnricher;
import com.wok.supportbot.document.transform.MyTokenTextSplitter;
import com.wok.supportbot.dto.response.DocumentVO;
import com.wok.supportbot.entity.Document;
import com.wok.supportbot.entity.DocumentVersion;
import com.wok.supportbot.repository.DocumentCategoryRepository;
import com.wok.supportbot.repository.DocumentRepository;
import com.wok.supportbot.repository.DocumentVersionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 文档服务
 */
@Slf4j
@Service
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class DocumentService {

    @Autowired(required = false)
    private DocumentRepository documentRepository;

    @Autowired(required = false)
    private DocumentVersionRepository documentVersionRepository;

    @Autowired(required = false)
    private DocumentCategoryRepository documentCategoryRepository;

    @Autowired(required = false)
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired(required = false)
    private TikaDocumentReader tikaDocumentReader;

    @Autowired(required = false)
    private MyTokenTextSplitter myTokenTextSplitter;

    @Autowired(required = false)
    private MyKeywordEnricher myKeywordEnricher;

    @Autowired(required = false)
    private VectorStore pgVectorVectorStore;

    @Autowired(required = false)
    private VectorStorePersistenceService vectorStorePersistenceService;

    // 文件存储路径（使用绝对路径）
    private static final String UPLOAD_DIR = System.getProperty("user.dir") + "/uploads";

    /**
     * 上传文档（同步保存，异步向量化）
     */
    @Transactional
    public DocumentVO uploadDocument(Long kbId, Long categoryId, MultipartFile file, Long userId) {
        try {
            // 创建上传目录
            Path uploadPath = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("创建上传目录: {}", uploadPath);
            }

            // 保存文件
            String originalFilename = file.getOriginalFilename();
            String safeFileName = originalFilename != null ? originalFilename.replaceAll("[^a-zA-Z0-9_\\.\\-]", "_") : "unknown";
            String fileName = System.currentTimeMillis() + "_" + safeFileName;
            Path filePath = uploadPath.resolve(fileName);

            // 使用 InputStream 保存文件（比 transferTo 更可靠）
            try (java.io.InputStream is = file.getInputStream()) {
                Files.copy(is, filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            log.info("文件保存成功: {}", filePath);

            // 创建文档记录
            Document document = new Document();
            document.setKbId(kbId);
            document.setCategoryId(categoryId);
            document.setTitle(file.getOriginalFilename());
            document.setFileType(getFileExtension(file.getOriginalFilename()));
            document.setFileSize(file.getSize());
            document.setFilePath(filePath.toString());
            document.setVectorStatus("PENDING");
            document.setVersion(1);
            document.setCreateBy(userId);

            documentRepository.insert(document);

            // 更新知识库文档数
            knowledgeBaseService.updateDocumentCount(kbId, 1);

            // 异步进行向量化处理
            asyncVectorizeDocument(document.getId(), filePath);

            return convertToVO(document);
        } catch (Exception e) {
            log.error("文档上传失败", e);
            throw new RuntimeException("文档上传失败：" + e.getMessage());
        }
    }

    /**
     * 异步向量化文档
     */
    @Async("documentVectorizeExecutor")
    public CompletableFuture<Void> asyncVectorizeDocument(Long documentId, Path filePath) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.info("开始异步向量化文档: id={}, path={}", documentId, filePath);

                // 读取文档内容
                List<org.springframework.ai.document.Document> aiDocuments = tikaDocumentReader.readFromPath(filePath);
                
                // 获取文档并更新内容
                Document document = documentRepository.selectById(documentId);
                if (document != null && !aiDocuments.isEmpty()) {
                    document.setContent(aiDocuments.get(0).getText());
                    documentRepository.updateById(document);
                }

                // 处理向量化
                processVectorization(documentId, aiDocuments);

                log.info("文档异步向量化完成: id={}", documentId);
            } catch (Exception e) {
                log.error("文档异步向量化失败: id={}", documentId, e);
                // 更新状态为失败
                try {
                    Document document = documentRepository.selectById(documentId);
                    if (document != null) {
                        document.setVectorStatus("FAILED");
                        documentRepository.updateById(document);
                    }
                } catch (Exception ex) {
                    log.error("更新文档状态失败", ex);
                }
            }
        });
    }

    /**
     * 处理向量化（异步）
     */
    private void processVectorization(Long documentId, List<org.springframework.ai.document.Document> aiDocuments) {
        if (pgVectorVectorStore == null) {
            log.warn("VectorStore 未配置，跳过向量化处理");
            Document document = documentRepository.selectById(documentId);
            if (document != null) {
                document.setVectorStatus("FAILED");
                documentRepository.updateById(document);
            }
            return;
        }
        try {
            // 更新状态为处理中
            Document document = documentRepository.selectById(documentId);
            if (document == null) {
                return;
            }
            document.setVectorStatus("PROCESSING");
            documentRepository.updateById(document);

            // 拆分文档
            List<org.springframework.ai.document.Document> splitDocuments = myTokenTextSplitter.splitDocuments(aiDocuments);

            // 添加元数据
            List<org.springframework.ai.document.Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(splitDocuments);

            // 转成向量并存入数据库
            pgVectorVectorStore.add(enrichedDocuments);

            // SimpleVectorStore 是内存存储，新增向量后立即落盘，避免服务重启后知识库丢失
            if (vectorStorePersistenceService != null) {
                vectorStorePersistenceService.save();
            }

            // 更新状态为完成
            document.setVectorStatus("COMPLETED");
            documentRepository.updateById(document);
            log.info("文档向量化完成: {}", document.getTitle());
        } catch (Exception e) {
            // 更新状态为失败
            log.error("向量化处理失败: {}", e.getMessage(), e);
            Document document = documentRepository.selectById(documentId);
            if (document != null) {
                document.setVectorStatus("FAILED");
                documentRepository.updateById(document);
            }
        }
    }

    /**
     * 重新向量化文档（用于更换向量模型后重新生成向量）
     */
    @Transactional
    public void revectorizeDocument(Long id) {
        Document document = documentRepository.selectById(id);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        if (document.getFilePath() == null) {
            throw new RuntimeException("文档文件路径为空");
        }

        Path filePath = Path.of(document.getFilePath());
        if (!Files.exists(filePath)) {
            throw new RuntimeException("文档文件不存在: " + filePath);
        }

        log.info("开始重新向量化文档: {}", document.getTitle());

        // 重新读取文档内容
        List<org.springframework.ai.document.Document> aiDocuments = tikaDocumentReader.readFromPath(filePath);
        if (aiDocuments.isEmpty()) {
            throw new RuntimeException("文档内容为空，无法向量化");
        }

        // 更新文档内容
        document.setContent(aiDocuments.get(0).getText());

        // 重新向量化
        processVectorization(document.getId(), aiDocuments);

        log.info("文档重新向量化完成: {}", document.getTitle());
    }

    /**
     * 重新向量化所有文档
     */
    public void revectorizeAllDocuments() {
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getIsDelete, false);
        List<Document> documents = documentRepository.selectList(wrapper);

        log.info("开始重新向量化所有文档，共 {} 个", documents.size());

        int success = 0;
        int failed = 0;
        for (Document document : documents) {
            try {
                revectorizeDocument(document.getId());
                success++;
            } catch (Exception e) {
                log.error("文档重新向量化失败: {}, 错误: {}", document.getTitle(), e.getMessage());
                failed++;
            }
        }

        log.info("重新向量化完成，成功: {}, 失败: {}", success, failed);
    }

    /**
     * 删除文档
     */
    @Transactional
    public void deleteDocument(Long id) {
        Document document = documentRepository.selectById(id);
        if (document == null) throw new RuntimeException("文档不存在");

        documentRepository.deleteById(id);

        knowledgeBaseService.updateDocumentCount(document.getKbId(), -1);

        if (document.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(document.getFilePath()));
            } catch (IOException e) {
            }
        }
    }

    /**
     * 分页查询
     */
    public Page<DocumentVO> page(Integer pageNum, Integer pageSize, Long kbId, Long categoryId, String name, String status) {
        Page<Document> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(kbId != null, Document::getKbId, kbId);
        wrapper.eq(categoryId != null, Document::getCategoryId, categoryId);
        wrapper.like(name != null && !name.isEmpty(), Document::getTitle, name);
        wrapper.eq(status != null && !status.isEmpty(), Document::getVectorStatus, status);
        wrapper.orderByDesc(Document::getCreateTime);

        Page<Document> result = documentRepository.selectPage(page, wrapper);

        // 批量查询分类名称
        List<Long> categoryIds = result.getRecords().stream()
                .map(Document::getCategoryId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        Map<Long, String> categoryNameMap = Map.of();
        if (!categoryIds.isEmpty()) {
            categoryNameMap = documentCategoryRepository.selectBatchIds(categoryIds).stream()
                    .collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));
        }

        Map<Long, String> finalCategoryNameMap = categoryNameMap;

        Page<DocumentVO> voPage = new Page<>();
        voPage.setCurrent(result.getCurrent());
        voPage.setSize(result.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(doc -> convertToVO(doc, finalCategoryNameMap))
                .collect(Collectors.toList()));

        return voPage;
    }

    /**
     * 根据ID查询
     */
    public DocumentVO getById(Long id) {
        Document document = documentRepository.selectById(id);
        if (document == null) {
            return null;
        }
        
        // 查询分类名称
        Map<Long, String> categoryNameMap = Map.of();
        if (document.getCategoryId() != null) {
            categoryNameMap = documentCategoryRepository.selectBatchIds(List.of(document.getCategoryId())).stream()
                    .collect(Collectors.toMap(c -> c.getId(), c -> c.getName()));
        }
        
        return convertToVO(document, categoryNameMap);
    }

    /**
     * 创建文档版本
     */
    @Transactional
    public void createVersion(Long documentId, String changeSummary, Long userId) {
        Document document = documentRepository.selectById(documentId);
        if (document == null) {
            throw new RuntimeException("文档不存在");
        }

        // 创建版本记录
        DocumentVersion version = new DocumentVersion();
        version.setDocumentId(documentId);
        version.setVersion(document.getVersion());
        version.setContent(document.getContent());
        version.setFilePath(document.getFilePath());
        version.setChangeSummary(changeSummary);
        version.setCreateBy(userId);

        documentVersionRepository.insert(version);

        // 更新文档版本号
        document.setVersion(document.getVersion() + 1);
        documentRepository.updateById(document);
    }

    /**
     * 获取文档版本历史
     */
    public List<DocumentVersion> getVersionHistory(Long documentId) {
        LambdaQueryWrapper<DocumentVersion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocumentVersion::getDocumentId, documentId);
        wrapper.orderByDesc(DocumentVersion::getCreateTime);

        return documentVersionRepository.selectList(wrapper);
    }

    private DocumentVO convertToVO(Document document, Map<Long, String> categoryNameMap) {
        DocumentVO vo = new DocumentVO();
        BeanUtils.copyProperties(document, vo);
        if (document.getCategoryId() != null && categoryNameMap.containsKey(document.getCategoryId())) {
            vo.setCategoryName(categoryNameMap.get(document.getCategoryId()));
        }
        return vo;
    }

    private DocumentVO convertToVO(Document document) {
        return convertToVO(document, Map.of());
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
