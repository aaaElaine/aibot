package com.wok.supportbot.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.wok.supportbot.handler.PostgresJsonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识库质量检测实体
 */
@Data
@TableName(value = "kb_quality_check", autoResultMap = true)
public class KbQualityCheck {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long kbId;

    private String checkType;

    @TableField(typeHandler = PostgresJsonTypeHandler.class)
    private Map<String, Object> checkResult;

    private BigDecimal score;

    private String suggestions;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime checkTime;

    @TableLogic
    private Boolean isDelete;
}