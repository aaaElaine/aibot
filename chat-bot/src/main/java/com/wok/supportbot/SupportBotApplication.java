package com.wok.supportbot;

import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;

import com.wok.supportbot.service.AuthService;
import com.wok.supportbot.service.DocumentService;
import com.wok.supportbot.service.KnowledgeBaseService;
import com.wok.supportbot.service.QualityCheckService;
import com.wok.supportbot.repository.SysUserRepository;
import com.wok.supportbot.repository.ProductRepository;
import com.wok.supportbot.repository.DocumentRepository;
import com.wok.supportbot.repository.DocumentVersionRepository;
import com.wok.supportbot.repository.DocumentCategoryRepository;
import com.wok.supportbot.repository.KnowledgeBaseRepository;
import com.wok.supportbot.repository.KbQualityCheckRepository;
import com.wok.supportbot.repository.ChatMessageRepository;

/**
 * 苏福万家智能客服后端服务启动类
 *
 * 自动配置排除说明：
 * - PgVectorStoreAutoConfiguration: 已由自定义 PgVectorStoreConfig 替代
 *
 * 组件扫描说明：
 * - 默认排除数据库相关包（repository、dao）和依赖数据库的服务
 * - 仅在 rag.enabled=true 时由 DatabaseConfig 重新扫描
 */
@SpringBootApplication(exclude = {
        PgVectorStoreAutoConfiguration.class
})
@EnableAsync
@ComponentScan(
        basePackages = "com.wok.supportbot",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                        AuthService.class,
                        DocumentService.class,
                        KnowledgeBaseService.class,
                        QualityCheckService.class,
                        SysUserRepository.class,
                        ProductRepository.class,
                        DocumentRepository.class,
                        DocumentVersionRepository.class,
                        DocumentCategoryRepository.class,
                        KnowledgeBaseRepository.class,
                        KbQualityCheckRepository.class,
                        ChatMessageRepository.class
                }),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.wok\\.supportbot\\.dao\\..*")
        }
)
public class SupportBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportBotApplication.class, args);
    }
}
