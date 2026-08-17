package com.wok.supportbot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wok.supportbot.dto.request.KnowledgeBaseCreateRequest;
import com.wok.supportbot.dto.request.KnowledgeBaseUpdateRequest;
import com.wok.supportbot.dto.response.KnowledgeBaseVO;
import com.wok.supportbot.entity.KnowledgeBase;
import com.wok.supportbot.repository.KnowledgeBaseRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库服务
 */
@Service
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class KnowledgeBaseService {

    @Autowired(required = false)
    private KnowledgeBaseRepository knowledgeBaseRepository;

    @Transactional
    public KnowledgeBaseVO create(KnowledgeBaseCreateRequest request) {
        KnowledgeBase kb = new KnowledgeBase();
        BeanUtils.copyProperties(request, kb);
        kb.setStatus("ACTIVE");
        kb.setDocumentCount(0);

        knowledgeBaseRepository.insert(kb);

        return convertToVO(kb);
    }

    @Transactional
    public KnowledgeBaseVO update(KnowledgeBaseUpdateRequest request) {
        KnowledgeBase kb = knowledgeBaseRepository.selectById(request.getId());
        if (kb == null) throw new RuntimeException("知识库不存在");

        if (request.getName() != null) kb.setName(request.getName());
        if (request.getDescription() != null) kb.setDescription(request.getDescription());
        if (request.getIcon() != null) kb.setIcon(request.getIcon());
        if (request.getStatus() != null) kb.setStatus(request.getStatus());

        knowledgeBaseRepository.updateById(kb);
        return convertToVO(kb);
    }

    @Transactional
    public void delete(Long id) {
        KnowledgeBase kb = knowledgeBaseRepository.selectById(id);
        if (kb == null) return;
        knowledgeBaseRepository.deleteById(id);
    }

    public KnowledgeBaseVO getById(Long id) {
        KnowledgeBase kb = knowledgeBaseRepository.selectById(id);
        if (kb == null) return null;
        return convertToVO(kb);
    }

    public Page<KnowledgeBaseVO> page(Integer pageNum, Integer pageSize, String keyword, String status) {
        Page<KnowledgeBase> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null && !keyword.isEmpty(), KnowledgeBase::getName, keyword);
        wrapper.eq(status != null && !status.isEmpty(), KnowledgeBase::getStatus, status);
        wrapper.orderByDesc(KnowledgeBase::getCreateTime);

        Page<KnowledgeBase> result = knowledgeBaseRepository.selectPage(page, wrapper);

        Page<KnowledgeBaseVO> voPage = new Page<>();
        voPage.setCurrent(result.getCurrent());
        voPage.setSize(result.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList()));

        return voPage;
    }

    public Page<KnowledgeBaseVO> page(Integer pageNum, Integer pageSize, String keyword) {
        return page(pageNum, pageSize, keyword, null);
    }

    /** H5/AI 端可见知识库：平台公开 + 自己机构 ACTIVE */
    public List<KnowledgeBaseVO> listVisibleByOrg(Long orgId) {
        Long safeOrgId = orgId == null ? 0L : orgId;
        LambdaQueryWrapper<KnowledgeBase> w = new LambdaQueryWrapper<>();
        w.eq(KnowledgeBase::getStatus, "ACTIVE");
        w.and(wr -> wr.eq(KnowledgeBase::getOrgId, 0L).or().eq(KnowledgeBase::getOrgId, safeOrgId));
        w.orderByDesc(KnowledgeBase::getCreateTime);
        List<KnowledgeBase> list = knowledgeBaseRepository.selectList(w);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    public List<KnowledgeBaseVO> listAll() {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getStatus, "ACTIVE");
        wrapper.orderByDesc(KnowledgeBase::getCreateTime);

        List<KnowledgeBase> list = knowledgeBaseRepository.selectList(wrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Transactional
    public void updateDocumentCount(Long kbId, int delta) {
        KnowledgeBase kb = knowledgeBaseRepository.selectById(kbId);
        if (kb != null) {
            kb.setDocumentCount(kb.getDocumentCount() + delta);
            knowledgeBaseRepository.updateById(kb);
        }
    }

    private KnowledgeBaseVO convertToVO(KnowledgeBase kb) {
        KnowledgeBaseVO vo = new KnowledgeBaseVO();
        BeanUtils.copyProperties(kb, vo);
        return vo;
    }
}
