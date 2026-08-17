package com.wok.supportbot.config;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wok.supportbot.dto.request.KnowledgeBaseCreateRequest;
import com.wok.supportbot.dto.request.KnowledgeBaseUpdateRequest;
import com.wok.supportbot.dto.request.LoginRequest;
import com.wok.supportbot.dto.response.DocumentVO;
import com.wok.supportbot.dto.response.KnowledgeBaseVO;
import com.wok.supportbot.dto.response.UserVO;
import com.wok.supportbot.entity.DocumentVersion;
import com.wok.supportbot.entity.KbQualityCheck;
import com.wok.supportbot.entity.SysUser;
import com.wok.supportbot.service.AuthService;
import com.wok.supportbot.service.DocumentService;
import com.wok.supportbot.service.KnowledgeBaseService;
import com.wok.supportbot.service.QualityCheckService;
import com.wok.supportbot.service.ProductService;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

/**
 * 无数据库模式配置
 * 当 rag.enabled=false 时激活，提供 Mock 实现
 */
@Configuration
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoDatabaseConfig {

    @Bean
    @Primary
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    @Primary
    public ProductService productService() {
        return ProductService.empty();
    }

    @Bean
    @Primary
    public AuthService authService() {
        return new AuthService() {
            @Override
            public String login(LoginRequest request) {
                throw new RuntimeException("系统未连接数据库，登录功能不可用");
            }

            @Override
            public void logout(String token) {
            }

            @Override
            public SysUser validateToken(String token) {
                return null;
            }

            @Override
            public UserVO getCurrentUser(String token) {
                return null;
            }
        };
    }

    @Bean
    @Primary
    public KnowledgeBaseService knowledgeBaseService() {
        return new KnowledgeBaseService() {
            @Override
            public KnowledgeBaseVO create(KnowledgeBaseCreateRequest request) {
                throw new RuntimeException("系统未连接数据库，知识库功能不可用");
            }

            @Override
            public KnowledgeBaseVO update(KnowledgeBaseUpdateRequest request) {
                throw new RuntimeException("系统未连接数据库，知识库功能不可用");
            }

            @Override
            public void delete(Long id) {
                throw new RuntimeException("系统未连接数据库，知识库功能不可用");
            }

            @Override
            public KnowledgeBaseVO getById(Long id) {
                return null;
            }

            @Override
            public Page<KnowledgeBaseVO> page(Integer pageNum, Integer pageSize, String keyword, String status) {
                return new Page<>(pageNum, pageSize);
            }

            @Override
            public Page<KnowledgeBaseVO> page(Integer pageNum, Integer pageSize, String keyword) {
                return new Page<>(pageNum, pageSize);
            }

            @Override
            public List<KnowledgeBaseVO> listAll() {
                return Collections.emptyList();
            }

            @Override
            public void updateDocumentCount(Long kbId, int delta) {
            }
        };
    }

    @Bean
    @Primary
    public DocumentService documentService() {
        return new DocumentService() {
            @Override
            public DocumentVO uploadDocument(Long kbId, Long categoryId, MultipartFile file, Long userId) {
                throw new RuntimeException("系统未连接数据库，文档上传功能不可用");
            }

            @Override
            public void revectorizeDocument(Long id) {
                throw new RuntimeException("系统未连接数据库");
            }

            @Override
            public void revectorizeAllDocuments() {
                throw new RuntimeException("系统未连接数据库");
            }

            @Override
            public void deleteDocument(Long id) {
                throw new RuntimeException("系统未连接数据库");
            }

            @Override
            public Page<DocumentVO> page(Integer pageNum, Integer pageSize, Long kbId, Long categoryId, String name, String status) {
                return new Page<>(pageNum, pageSize);
            }

            @Override
            public DocumentVO getById(Long id) {
                return null;
            }

            @Override
            public void createVersion(Long documentId, String changeSummary, Long userId) {
            }

            @Override
            public List<DocumentVersion> getVersionHistory(Long documentId) {
                return Collections.emptyList();
            }
        };
    }

    @Bean
    @Primary
    public QualityCheckService qualityCheckService() {
        return new QualityCheckService() {
            @Override
            public KbQualityCheck performCheck(Long kbId, String checkType) {
                throw new RuntimeException("系统未连接数据库，质量检测功能不可用");
            }

            @Override
            public List<KbQualityCheck> getCheckHistory(Long kbId) {
                return Collections.emptyList();
            }

            @Override
            public KbQualityCheck getLatestCheck(Long kbId, String checkType) {
                return null;
            }
        };
    }
}
