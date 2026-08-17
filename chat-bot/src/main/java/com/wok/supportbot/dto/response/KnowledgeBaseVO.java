package com.wok.supportbot.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识库视图对象
 */
@Data
public class KnowledgeBaseVO {
    
    private Long id;
    private String name;
    private String description;
    private String icon;
    private String status;
    private Integer documentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}