package com.wok.supportbot.config;

import com.wok.supportbot.entity.SysUser;
import com.wok.supportbot.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 鉴权拦截器：
 * - 解析 Authorization 头中的 token
 * - 白名单放行 → token 验证 → 401 拦截
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** 无需登录的白名单路径 */
    private static final Set<String> WHITE_LIST = new HashSet<>(Arrays.asList(
            "/api/auth/login",
            "/api/auth/current",
            "/api/auth/validate",
            "/h2-console",
            "/ai/assistant_app/chat",
            "/ai/assistant_app/chat/with-tools",
            "/api/h5/config",
            "/api/public",
            "/doc.html",
            "/v3/api-docs",
            "/webjars",
            "/favicon.ico"
    ));

    @Autowired
    private AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // CORS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }

        // 白名单路径直接放行
        for (String prefix : WHITE_LIST) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }

        String token = extractToken(request.getHeader("Authorization"));
        SysUser user = authService.validateToken(token);
        if (user == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=utf-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
            return false;
        }

        return true;
    }

    private String extractToken(String authorization) {
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return authorization;
    }
}
