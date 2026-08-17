package com.wok.supportbot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wok.supportbot.dto.request.KnowledgeBaseCreateRequest;
import com.wok.supportbot.dto.request.KnowledgeBaseUpdateRequest;
import com.wok.supportbot.dto.response.KnowledgeBaseVO;
import com.wok.supportbot.dto.response.Result;
import com.wok.supportbot.service.KnowledgeBaseService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库管理控制器
 */
@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseController {

    @Resource
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 创建知识库
     */
    @PostMapping("/create")
    public Result<KnowledgeBaseVO> create(@RequestBody KnowledgeBaseCreateRequest request) {
        try {
            KnowledgeBaseVO vo = knowledgeBaseService.create(request);
            return Result.success("创建成功", vo);
        } catch (Exception e) {
            return Result.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 更新知识库
     */
    @PutMapping("/update")
    public Result<KnowledgeBaseVO> update(@RequestBody KnowledgeBaseUpdateRequest request) {
        try {
            KnowledgeBaseVO vo = knowledgeBaseService.update(request);
            return Result.success("更新成功", vo);
        } catch (Exception e) {
            return Result.error("更新失败：" + e.getMessage());
        }
    }

    /**
     * 删除知识库
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            knowledgeBaseService.delete(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBaseVO> getById(@PathVariable Long id) {
        KnowledgeBaseVO vo = knowledgeBaseService.getById(id);
        return vo != null ? Result.success(vo) : Result.error("知识库不存在");
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public Result<Page<KnowledgeBaseVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        // 兼容 name 和 keyword 两种参数名
        String searchKeyword = name != null ? name : keyword;
        Page<KnowledgeBaseVO> page = knowledgeBaseService.page(pageNum, pageSize, searchKeyword, status);
        return Result.success(page);
    }

    /**
     * 查询所有启用的知识库
     */
    @GetMapping("/list")
    public Result<List<KnowledgeBaseVO>> listAll() {
        List<KnowledgeBaseVO> list = knowledgeBaseService.listAll();
        return Result.success(list);
    }
}