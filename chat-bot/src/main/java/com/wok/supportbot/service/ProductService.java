package com.wok.supportbot.service;

import com.wok.supportbot.entity.Product;
import com.wok.supportbot.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 商品服务
 * 注意：不使用 @Service 注解，由配置类创建 Bean
 */
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    // 用于无数据库模式的空实现
    private static final ProductService EMPTY = new ProductService(null);

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public static ProductService empty() {
        return EMPTY;
    }

    public List<Product> searchProducts(String keyword, int limit) {
        if (productRepository == null) return Collections.emptyList();
        log.info("搜索商品，关键词：{}，限制数量：{}", keyword, limit);
        // H2 数据库 LIKE 对中文支持不完善，使用内存过滤
        List<Product> allProducts = productRepository.findAllOnline();
        return allProducts.stream()
                .filter(p -> {
                    String kw = keyword.toLowerCase();
                    return (p.getName() != null && p.getName().toLowerCase().contains(kw)) ||
                           (p.getDescription() != null && p.getDescription().toLowerCase().contains(kw)) ||
                           (p.getCategory() != null && p.getCategory().toLowerCase().contains(kw));
                })
                .limit(limit)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<Product> getProductsByCategory(String category, int limit) {
        if (productRepository == null) return Collections.emptyList();
        log.info("获取分类商品，分类：{}，限制数量：{}", category, limit);
        return productRepository.findByCategory(category, limit);
    }

    public List<Product> getAllProducts() {
        if (productRepository == null) return Collections.emptyList();
        return productRepository.findAllOnline();
    }

    public Product getProductById(Long id) {
        if (productRepository == null) return null;
        return productRepository.getById(id);
    }

    public Product saveProduct(Product product) {
        if (productRepository == null) return product;
        productRepository.save(product);
        return product;
    }

    public void saveBatchProducts(List<Product> products) {
        if (productRepository == null) return;
        productRepository.saveBatch(products);
    }
}
