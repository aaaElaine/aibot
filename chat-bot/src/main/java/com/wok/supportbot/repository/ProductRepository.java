package com.wok.supportbot.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wok.supportbot.dao.ProductMapper;
import com.wok.supportbot.entity.Product;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 商品Repository
 */
@Repository
public class ProductRepository extends ServiceImpl<ProductMapper, Product> {

    /**
     * 根据关键词搜索商品
     */
    public List<Product> searchByKeyword(String keyword, int limit) {
        return baseMapper.searchByKeyword(keyword, limit);
    }

    /**
     * 根据分类获取商品
     */
    public List<Product> findByCategory(String category, int limit) {
        return baseMapper.findByCategory(category, limit);
    }

    /**
     * 获取所有在线商品
     */
    public List<Product> findAllOnline() {
        return lambdaQuery()
                .eq(Product::getStatus, "ONLINE")
                .orderByDesc(Product::getSalesCount)
                .list();
    }
}
