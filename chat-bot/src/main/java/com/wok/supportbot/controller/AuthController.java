package com.wok.supportbot.controller;

import com.wok.supportbot.dto.request.LoginRequest;
import com.wok.supportbot.dto.response.Result;
import com.wok.supportbot.dto.response.UserVO;
import com.wok.supportbot.service.AuthService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Resource
    private AuthService authService;

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request);
            UserVO userVO = authService.getCurrentUser(token);
            
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("user", userVO);
            
            return Result.success("登录成功", data);
        } catch (Exception e) {
            return Result.error(401, e.getMessage());
        }
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = extractToken(request.getHeader("Authorization"));
        authService.logout(token);
        return Result.success("登出成功", null);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<UserVO> getCurrentUser(HttpServletRequest request) {
        String token = extractToken(request.getHeader("Authorization"));
        UserVO userVO = authService.getCurrentUser(token);
        
        if (userVO == null) {
            return Result.error(401, "未登录或登录已过期");
        }
        
        return Result.success(userVO);
    }

    /**
     * 验证Token
     */
    @GetMapping("/validate")
    public Result<Boolean> validateToken(HttpServletRequest request) {
        String token = extractToken(request.getHeader("Authorization"));
        boolean valid = authService.validateToken(token) != null;
        return Result.success(valid);
    }

    /**
     * 从Authorization头中提取Token
     */
    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}