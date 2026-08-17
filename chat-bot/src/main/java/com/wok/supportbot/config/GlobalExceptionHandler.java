package com.wok.supportbot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 全局异常处理器
 * 专门处理阿里云 DashScope API 错误，特别是欠费和模型错误
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final Pattern ARREARS_PATTERN = Pattern.compile("Arrears", Pattern.CASE_INSENSITIVE);
    private static final Pattern INVALID_MODEL_PATTERN = Pattern.compile("InvalidParameter|model.*not.*exist|model.*not.*found", Pattern.CASE_INSENSITIVE);
    private static final Pattern RATE_LIMIT_PATTERN = Pattern.compile("rate.*limit|throttl|quota|busy", Pattern.CASE_INSENSITIVE);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        String errorMessage = extractRootCauseMessage(e);
        log.error("请求异常: {}", errorMessage, e);

        Map<String, Object> result = new HashMap<>();
        int statusCode = 500;
        String userMessage = "服务暂时不可用，请稍后再试~";

        if (ARREARS_PATTERN.matcher(errorMessage).find()) {
            statusCode = 402;
            userMessage = "抱歉，AI 服务账户已欠费，请充值后再使用💳";
            log.error("❌ 阿里云 API 欠费！message={}", errorMessage);
        } else if (INVALID_MODEL_PATTERN.matcher(errorMessage).find()) {
            statusCode = 400;
            userMessage = "抱歉，AI 模型配置有误，请联系管理员🔧";
            log.error("❌ 模型参数错误！message={}", errorMessage);
        } else if (RATE_LIMIT_PATTERN.matcher(errorMessage).find()) {
            statusCode = 429;
            userMessage = "抱歉，AI 服务繁忙，请稍后再试⏰";
            log.warn("⚠️ API 限流！message={}", errorMessage);
        }

        result.put("code", statusCode);
        result.put("message", userMessage);
        result.put("detail", errorMessage);
        result.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.status(statusCode).body(result);
    }

    private String extractRootCauseMessage(Throwable e) {
        Throwable cause = e;
        String message = e.getMessage();
        while (cause.getCause() != null) {
            cause = cause.getCause();
            if (cause.getMessage() != null && !cause.getMessage().isEmpty()) {
                message = cause.getMessage();
            }
        }
        return message != null ? message : e.getClass().getSimpleName();
    }
}
