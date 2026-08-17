package com.wok.supportbot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wok.supportbot.entity.DocumentCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档分类Mapper
 */
@Mapper
public interface DocumentCategoryRepository extends BaseMapper<DocumentCategory> {
}