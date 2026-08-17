package com.wok.supportbot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wok.supportbot.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统用户Mapper
 */
@Mapper
public interface SysUserRepository extends BaseMapper<SysUser> {
}