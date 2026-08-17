package com.wok.supportbot.controller;

import com.wok.supportbot.entity.Product;
import com.wok.supportbot.service.ProductService;
import com.wok.supportbot.dto.response.Result;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 商品管理Controller
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    @Resource
    private ProductService productService;

    /**
     * 搜索商品
     */
    @GetMapping("/search")
    public Result<List<Product>> searchProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "5") int limit) {
        List<Product> products = productService.searchProducts(keyword, limit);
        return Result.success(products);
    }

    /**
     * 获取分类商品
     */
    @GetMapping("/category/{category}")
    public Result<List<Product>> getProductsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "10") int limit) {
        List<Product> products = productService.getProductsByCategory(category, limit);
        return Result.success(products);
    }

    /**
     * 获取所有商品
     */
    @GetMapping
    public Result<List<Product>> getAllProducts() {
        List<Product> products = productService.getAllProducts();
        return Result.success(products);
    }

    /**
     * 根据ID获取商品
     */
    @GetMapping("/{id}")
    public Result<Product> getProductById(@PathVariable Long id) {
        Product product = productService.getProductById(id);
        if (product == null) {
            return Result.error("商品不存在");
        }
        return Result.success(product);
    }

    /**
     * 保存商品
     */
    @PostMapping
    public Result<Product> saveProduct(@RequestBody Product product) {
        Product saved = productService.saveProduct(product);
        return Result.success(saved);
    }

    /**
     * 批量保存商品
     */
    @PostMapping("/batch")
    public Result<Void> saveBatchProducts(@RequestBody List<Product> products) {
        productService.saveBatchProducts(products);
        return Result.success();
    }
}
