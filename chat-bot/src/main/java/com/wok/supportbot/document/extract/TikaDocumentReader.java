package com.wok.supportbot.document.extract;

import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class TikaDocumentReader {

    /**
     * 从 MultipartFile 读取文档内容
     */
    public List<Document> read(MultipartFile file) {
        try {
            File tempFile = File.createTempFile("upload-", file.getOriginalFilename());
            file.transferTo(tempFile);
            return readFromFile(tempFile.toPath());
        } catch (IOException | TikaException e) {
            log.error("Tika 文件解析失败", e);
            throw new RuntimeException("Tika 文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 从已保存的文件路径读取文档内容
     */
    public List<Document> readFromPath(Path filePath) {
        try {
            return readFromFile(filePath);
        } catch (IOException | TikaException e) {
            log.error("Tika 文件解析失败: {}", filePath, e);
            throw new RuntimeException("文件解析失败: " + e.getMessage());
        }
    }

    private List<Document> readFromFile(Path filePath) throws IOException, TikaException {
        Tika tika = new Tika();
        
        // 使用 Tika 探测文件类型并解析
        String detectedType = tika.detect(filePath.toFile());
        log.info("检测到文件类型: {}, 路径: {}", detectedType, filePath);
        
        String text;
        try {
            text = tika.parseToString(filePath.toFile());
        } catch (Exception e) {
            log.warn("Tika 默认解析失败，尝试使用特定解析器: {}", e.getMessage());
            // 如果默认解析失败，尝试使用特定解析器
            text = tika.parseToString(filePath.toFile());
        }

        if (text == null || text.trim().isEmpty()) {
            text = "(文档内容为空或无法解析)";
        }
        
        // 限制文本长度，避免过大的内容存储和向量化
        if (text.length() > 50000) {
            log.warn("文档内容过长 ({} 字符)，截断到 50000 字符", text.length());
            text = text.substring(0, 50000) + "\n\n... (内容已截断)";
        }

        log.info("文档解析成功，内容长度: {} 字符", text.length());
        
        Document doc = Document.builder()
                .id(UUID.randomUUID().toString())
                .text(text)
                .metadata("file_type", detectedType)
                .build();

        return Collections.singletonList(doc);
    }
}
