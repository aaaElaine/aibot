package com.wok.supportbot.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文档视图对象
 */
@Data
public class DocumentVO {
    
    private Long id;
    private Long kbId;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String content;
    private String fileType;
    private Long fileSize;
    private String filePath;
    private String vectorStatus;
    private BigDecimal qualityScore;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}