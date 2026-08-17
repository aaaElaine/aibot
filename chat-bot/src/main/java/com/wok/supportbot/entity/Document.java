package com.wok.supportbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Data
@TableName("document")
public class Document {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 归属机构ID（冗余字段，便于按机构查询） */
    private Long orgId;

    private Long kbId;

    private Long categoryId;

    private String title;

    private String content;

    private String fileType;

    private Long fileSize;

    private String filePath;

    private String vectorStatus;

    private BigDecimal qualityScore;

    private Integer version;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean isDelete;
}