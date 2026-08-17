package com.wok.supportbot.dto.request;

import lombok.Data;

/**
 * 更新知识库请求
 */
@Data
public class KnowledgeBaseUpdateRequest {
    
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String status;
}