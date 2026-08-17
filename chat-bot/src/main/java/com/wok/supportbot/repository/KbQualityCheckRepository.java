package com.wok.supportbot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wok.supportbot.entity.KbQualityCheck;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库质量检测Mapper
 */
@Mapper
public interface KbQualityCheckRepository extends BaseMapper<KbQualityCheck> {
}