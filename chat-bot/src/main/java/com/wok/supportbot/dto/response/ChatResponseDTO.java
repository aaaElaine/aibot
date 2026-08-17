package com.wok.supportbot.dto.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI聊天响应DTO - 支持文本和商品卡片混合返回
 */
@Data
public class ChatResponseDTO {

    /**
     * 回复文本内容
     */
    private String text;

    /**
     * 推荐的商品列表
     */
    private List<ProductCard> products = new ArrayList<>();

    /**
     * 工具调用结果（天气、新闻等）
     */
    private Map<String, Object> toolResult;

    /**
     * 是否有商品推荐
     */
    public boolean hasProducts() {
        return products != null && !products.isEmpty();
    }

    /**
     * 是否有工具结果
     */
    public boolean hasToolResult() {
        return toolResult != null && !toolResult.isEmpty();
    }

    /**
     * 商品卡片
     */
    @Data
    public static class ProductCard {
        private Long id;
        private String name;
        private String description;
        private String price;
        private String originalPrice;
        private String imageUrl;
        private String category;
        private String productUrl;
        private Integer salesCount;
    }
}
