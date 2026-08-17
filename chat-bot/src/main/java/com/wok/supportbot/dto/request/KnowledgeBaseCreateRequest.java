package com.wok.supportbot.dto.request;

import lombok.Data;

/**
 * 创建知识库请求
 */
@Data
public class KnowledgeBaseCreateRequest {
    
    private String name;
    private String description;
    private String icon;
}