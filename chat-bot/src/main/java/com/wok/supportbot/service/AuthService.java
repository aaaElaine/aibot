package com.wok.supportbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wok.supportbot.dto.request.LoginRequest;
import com.wok.supportbot.dto.response.UserVO;
import com.wok.supportbot.entity.*;
import com.wok.supportbot.repository.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 认证服务
 */
@Service
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class AuthService {

    @Autowired(required = false)
    private SysUserRepository sysUserRepository;

    // 简单的Token存储（生产环境建议使用Redis）
    private static final Map<String, SysUser> tokenStore = new ConcurrentHashMap<>();

    /**
     * 登录
     */
    public String login(LoginRequest request) {
        // 查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = sysUserRepository.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证密码（简单示例，实际应使用BCrypt）
        String encryptedPassword = DigestUtils.md5DigestAsHex(request.getPassword().getBytes());
        if (!user.getPassword().equals(encryptedPassword) &&
            !user.getPassword().equals(request.getPassword())) { // 开发环境支持明文密码
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查状态
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("账号已被禁用");
        }

        // 生成Token
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, user);

        // 更新最后登录时间
        user.setLastLoginTime(LocalDateTime.now());
        sysUserRepository.updateById(user);

        return token;
    }

    /**
     * 登出
     */
    public void logout(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }

    /**
     * 验证Token
     */
    public SysUser validateToken(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return tokenStore.get(token);
    }

    /**
     * 获取当前用户
     */
    public UserVO getCurrentUser(String token) {
        SysUser user = validateToken(token);
        if (user == null) {
            return null;
        }
        return toUserVO(user);
    }

    /** SysUser -> UserVO */
    public UserVO toUserVO(SysUser user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
