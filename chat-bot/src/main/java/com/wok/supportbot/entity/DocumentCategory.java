package com.wok.supportbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档分类实体
 */
@Data
@TableName("document_category")
public class DocumentCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private Long parentId;

    private String name;

    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Boolean isDelete;
}