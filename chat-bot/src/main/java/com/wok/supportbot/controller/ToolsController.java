package com.wok.supportbot.controller;

import com.wok.supportbot.service.NewsService;
import com.wok.supportbot.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 实用工具控制器
 * 提供天气查询、新闻浏览等实用工具 API
 */
@RestController
@RequestMapping("/api/tools")
public class ToolsController {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private NewsService newsService;

    /**
     * 天气查询
     * @param city 城市名（可选，默认返回 IP 所在地天气）
     * @param dateOffset 日期偏移（0=今天, 1=明天, 2=后天，可选，默认0）
     */
    @GetMapping("/weather")
    public ResponseEntity<Map<String, Object>> getWeather(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "0") int dateOffset) {
        Map<String, Object> weather = weatherService.getWeather(city, dateOffset);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", weather);
        result.put("formatted", weatherService.formatWeather(city != null ? city : "默认", weather));
        return ResponseEntity.ok(result);
    }

    /**
     * 获取支持的城市列表
     */
    @GetMapping("/weather/cities")
    public ResponseEntity<Map<String, Object>> getSupportedCities() {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", weatherService.getSupportedCities());
        return ResponseEntity.ok(result);
    }

    /**
     * 新闻 Top10
     * @param city 城市名（可选，不指定则返回全国新闻）
     * @param limit 数量限制（默认10）
     */
    @GetMapping("/news")
    public ResponseEntity<Map<String, Object>> getNews(
            @RequestParam(required = false) String city,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> news;
        String scope;
        if (city != null && !city.trim().isEmpty()) {
            news = newsService.getCityNews(city, limit);
            scope = city;
        } else {
            news = newsService.getNationalNews(limit);
            scope = "全国";
        }
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("data", news);
        result.put("scope", scope);
        result.put("formatted", newsService.formatNews(news, scope));
        return ResponseEntity.ok(result);
    }
}
