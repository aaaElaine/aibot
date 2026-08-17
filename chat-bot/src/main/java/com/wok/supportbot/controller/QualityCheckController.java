package com.wok.supportbot.controller;

import com.wok.supportbot.dto.response.Result;
import com.wok.supportbot.entity.KbQualityCheck;
import com.wok.supportbot.service.QualityCheckService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库质量检测控制器
 */
@RestController
@RequestMapping("/api/quality")
public class QualityCheckController {

    @Resource
    private QualityCheckService qualityCheckService;

    /**
     * 执行质量检测
     */
    @PostMapping("/check/{kbId}")
    public Result<KbQualityCheck> performCheck(
            @PathVariable Long kbId,
            @RequestParam(required = false, defaultValue = "COMPLETENESS") String checkType) {
        try {
            KbQualityCheck check = qualityCheckService.performCheck(kbId, checkType);
            return Result.success("检测完成", check);
        } catch (Exception e) {
            return Result.error("检测失败：" + e.getMessage());
        }
    }

    /**
     * 执行全部检测
     */
    @PostMapping("/check-all/{kbId}")
    public Result<List<KbQualityCheck>> performAllChecks(@PathVariable Long kbId) {
        try {
            List<KbQualityCheck> checks = List.of(
                qualityCheckService.performCheck(kbId, "COVERAGE"),
                qualityCheckService.performCheck(kbId, "ACCURACY"),
                qualityCheckService.performCheck(kbId, "COMPLETENESS")
            );
            return Result.success("全部检测完成", checks);
        } catch (Exception e) {
            return Result.error("检测失败：" + e.getMessage());
        }
    }

    /**
     * 查询检测历史
     */
    @GetMapping("/history/{kbId}")
    public Result<List<KbQualityCheck>> getCheckHistory(@PathVariable Long kbId) {
        List<KbQualityCheck> history = qualityCheckService.getCheckHistory(kbId);
        return Result.success(history);
    }

    /**
     * 获取最新检测结果
     */
    @GetMapping("/latest/{kbId}")
    public Result<KbQualityCheck> getLatestCheck(
            @PathVariable Long kbId,
            @RequestParam String checkType) {
        KbQualityCheck check = qualityCheckService.getLatestCheck(kbId, checkType);
        return check != null ? Result.success(check) : Result.error("暂无检测记录");
    }
}