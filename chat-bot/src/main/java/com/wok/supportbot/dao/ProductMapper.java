package com.wok.supportbot.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wok.supportbot.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品Mapper
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    /**
     * 根据关键词搜索商品
     */
    @Select("SELECT * FROM product WHERE is_delete = FALSE AND status = 'ONLINE' " +
            "AND (name REGEXP CONCAT('.*', #{keyword}, '.*') " +
            "OR description REGEXP CONCAT('.*', #{keyword}, '.*') " +
            "OR category REGEXP CONCAT('.*', #{keyword}, '.*')) " +
            "ORDER BY sales_count DESC LIMIT #{limit}")
    List<Product> searchByKeyword(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 根据分类获取商品
     */
    @Select("SELECT * FROM product WHERE is_delete = FALSE AND status = 'ONLINE' " +
            "AND category = #{category} ORDER BY sales_count DESC LIMIT #{limit}")
    List<Product> findByCategory(@Param("category") String category, @Param("limit") int limit);
}
