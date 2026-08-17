package com.wok.supportbot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wok.supportbot.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库Mapper
 */
@Mapper
public interface KnowledgeBaseRepository extends BaseMapper<KnowledgeBase> {
}