package com.wok.supportbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wok.supportbot.entity.Document;
import com.wok.supportbot.entity.KbQualityCheck;
import com.wok.supportbot.entity.KnowledgeBase;
import com.wok.supportbot.repository.DocumentRepository;
import com.wok.supportbot.repository.KbQualityCheckRepository;
import com.wok.supportbot.repository.KnowledgeBaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库质量检测服务
 */
@Service
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class QualityCheckService {

    @Autowired(required = false)
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Autowired(required = false)
    private DocumentRepository documentRepository;

    @Autowired(required = false)
    private KbQualityCheckRepository kbQualityCheckRepository;

    /**
     * 执行质量检测
     */
    @Transactional
    public KbQualityCheck performCheck(Long kbId, String checkType) {
        // 验证知识库存在
        KnowledgeBase kb = knowledgeBaseRepository.selectById(kbId);
        if (kb == null) {
            throw new RuntimeException("知识库不存在");
        }

        KbQualityCheck check = new KbQualityCheck();
        check.setKbId(kbId);
        check.setCheckType(checkType);
        check.setCheckTime(LocalDateTime.now());

        // 查询文档
        LambdaQueryWrapper<Document> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Document::getKbId, kbId);
        List<Document> documents = documentRepository.selectList(wrapper);

        // 根据检测类型执行检测
        switch (checkType) {
            case "COVERAGE":
                checkCoverage(check, documents);
                break;
            case "ACCURACY":
                checkAccuracy(check, documents);
                break;
            case "COMPLETENESS":
                checkCompleteness(check, documents);
                break;
            default:
                throw new RuntimeException("不支持的检测类型：" + checkType);
        }

        // 保存检测结果
        kbQualityCheckRepository.insert(check);
        
        return check;
    }

    /**
     * 覆盖度检测
     */
    private void checkCoverage(KbQualityCheck check, List<Document> documents) {
        Map<String, Object> result = new HashMap<>();
        
        // 计算文档覆盖率
        long totalDocs = documents.size();
        long vectorCompleted = documents.stream()
                .filter(d -> "COMPLETED".equals(d.getVectorStatus()))
                .count();
        
        double coverage = totalDocs > 0 ? (double) vectorCompleted / totalDocs * 100 : 0;
        
        result.put("totalDocuments", totalDocs);
        result.put("vectorCompleted", vectorCompleted);
        result.put("coverageRate", String.format("%.2f%%", coverage));
        
        check.setCheckResult(result);
        check.setScore(BigDecimal.valueOf(coverage).setScale(2, RoundingMode.HALF_UP));
        
        // 生成建议
        StringBuilder suggestions = new StringBuilder();
        if (coverage < 80) {
            suggestions.append("文档向量化覆盖率较低，建议检查文档上传流程是否正常。\n");
        }
        if (totalDocs == 0) {
            suggestions.append("知识库暂无文档，请添加文档以提高知识库价值。\n");
        }
        
        check.setSuggestions(suggestions.toString());
    }

    /**
     * 准确度检测
     */
    private void checkAccuracy(KbQualityCheck check, List<Document> documents) {
        Map<String, Object> result = new HashMap<>();
        
        // 计算平均质量分数
        double avgQuality = documents.stream()
                .filter(d -> d.getQualityScore() != null)
                .mapToDouble(d -> d.getQualityScore().doubleValue())
                .average()
                .orElse(0.0);
        
        // 计算文档完整度（有内容的文档比例）
        long docsWithContent = documents.stream()
                .filter(d -> d.getContent() != null && !d.getContent().isEmpty())
                .count();
        double completeness = documents.size() > 0 ? (double) docsWithContent / documents.size() * 100 : 0;
        
        result.put("averageQualityScore", String.format("%.2f", avgQuality));
        result.put("documentCompleteness", String.format("%.2f%%", completeness));
        
        // 综合评分
        double score = (avgQuality * 0.6 + completeness * 0.4);
        
        check.setCheckResult(result);
        check.setScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));
        
        // 生成建议
        StringBuilder suggestions = new StringBuilder();
        if (avgQuality < 60) {
            suggestions.append("文档整体质量分数偏低，建议检查文档内容质量和格式。\n");
        }
        if (completeness < 90) {
            suggestions.append("部分文档缺少内容，建议补充完整文档内容。\n");
        }
        
        check.setSuggestions(suggestions.toString());
    }

    /**
     * 完整度检测
     */
    private void checkCompleteness(KbQualityCheck check, List<Document> documents) {
        Map<String, Object> result = new HashMap<>();
        
        // 统计各状态文档数量
        long pendingDocs = documents.stream().filter(d -> "PENDING".equals(d.getVectorStatus())).count();
        long processingDocs = documents.stream().filter(d -> "PROCESSING".equals(d.getVectorStatus())).count();
        long completedDocs = documents.stream().filter(d -> "COMPLETED".equals(d.getVectorStatus())).count();
        long failedDocs = documents.stream().filter(d -> "FAILED".equals(d.getVectorStatus())).count();
        
        // 统计文档版本情况
        double avgVersion = documents.stream()
                .mapToInt(Document::getVersion)
                .average()
                .orElse(1.0);
        
        result.put("pendingDocuments", pendingDocs);
        result.put("processingDocuments", processingDocs);
        result.put("completedDocuments", completedDocs);
        result.put("failedDocuments", failedDocs);
        result.put("averageVersion", String.format("%.2f", avgVersion));
        
        // 评分：基于完成率和失败率
        double completionRate = documents.size() > 0 ? (double) completedDocs / documents.size() * 100 : 0;
        double failRate = documents.size() > 0 ? (double) failedDocs / documents.size() * 100 : 0;
        double score = Math.max(0, completionRate - failRate * 2);
        
        check.setCheckResult(result);
        check.setScore(BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP));
        
        // 生成建议
        StringBuilder suggestions = new StringBuilder();
        if (pendingDocs > 0) {
            suggestions.append(String.format("有%d个文档待处理，建议及时处理。\n", pendingDocs));
        }
        if (failedDocs > 0) {
            suggestions.append(String.format("有%d个文档处理失败，建议检查失败原因并重新上传。\n", failedDocs));
        }
        if (avgVersion > 1.5) {
            suggestions.append("文档版本较新，说明知识库持续更新中，保持良好。\n");
        }
        
        check.setSuggestions(suggestions.toString());
    }

    /**
     * 查询检测历史
     */
    public List<KbQualityCheck> getCheckHistory(Long kbId) {
        LambdaQueryWrapper<KbQualityCheck> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbQualityCheck::getKbId, kbId);
        wrapper.orderByDesc(KbQualityCheck::getCheckTime);
        
        return kbQualityCheckRepository.selectList(wrapper);
    }

    /**
     * 获取最新检测结果
     */
    public KbQualityCheck getLatestCheck(Long kbId, String checkType) {
        LambdaQueryWrapper<KbQualityCheck> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbQualityCheck::getKbId, kbId);
        wrapper.eq(KbQualityCheck::getCheckType, checkType);
        wrapper.orderByDesc(KbQualityCheck::getCheckTime);
        wrapper.last("LIMIT 1");
        
        return kbQualityCheckRepository.selectOne(wrapper);
    }
}