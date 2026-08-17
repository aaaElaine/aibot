package com.wok.supportbot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wok.supportbot.entity.DocumentVersion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文档版本Mapper
 */
@Mapper
public interface DocumentVersionRepository extends BaseMapper<DocumentVersion> {
}