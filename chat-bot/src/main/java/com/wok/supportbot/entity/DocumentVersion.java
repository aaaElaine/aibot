package com.wok.supportbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档版本历史实体
 */
@Data
@TableName("document_version")
public class DocumentVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Integer version;

    private String content;

    private String filePath;

    private String changeSummary;

    private Long createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Boolean isDelete;
}