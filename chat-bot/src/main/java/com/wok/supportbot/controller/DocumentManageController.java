package com.wok.supportbot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wok.supportbot.dto.response.DocumentVO;
import com.wok.supportbot.dto.response.Result;
import com.wok.supportbot.entity.Document;
import com.wok.supportbot.entity.DocumentVersion;
import com.wok.supportbot.service.DocumentService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 文档管理控制器
 */
@RestController
@RequestMapping("/api/document")
public class DocumentManageController {

    @Resource
    private DocumentService documentService;

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public Result<DocumentVO> upload(
            @RequestParam Long kbId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "1") Long userId) {
        try {
            DocumentVO vo = documentService.uploadDocument(kbId, categoryId, file, userId);
            return Result.success("文档上传成功", vo);
        } catch (Exception e) {
            return Result.error("文档上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        try {
            documentService.deleteDocument(id);
            return Result.success("删除成功", null);
        } catch (Exception e) {
            return Result.error("删除失败：" + e.getMessage());
        }
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public Result<Page<DocumentVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long kbId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String status) {
        Page<DocumentVO> page = documentService.page(pageNum, pageSize, kbId, categoryId, name, status);
        return Result.success(page);
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public Result<DocumentVO> getById(@PathVariable Long id) {
        DocumentVO vo = documentService.getById(id);
        return vo != null ? Result.success(vo) : Result.error("文档不存在");
    }

    /**
     * 重新向量化文档（用于更换向量模型后重新生成向量）
     */
    @PostMapping("/revectorize/{id}")
    public Result<Void> revectorize(@PathVariable Long id) {
        try {
            documentService.revectorizeDocument(id);
            return Result.success("重新向量化成功", null);
        } catch (Exception e) {
            return Result.error("重新向量化失败：" + e.getMessage());
        }
    }

    /**
     * 重新向量化所有文档
     */
    @PostMapping("/revectorize-all")
    public Result<Void> revectorizeAll() {
        try {
            documentService.revectorizeAllDocuments();
            return Result.success("全部重新向量化成功", null);
        } catch (Exception e) {
            return Result.error("全部重新向量化失败：" + e.getMessage());
        }
    }

    /**
     * 下载文档
     */
    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id, HttpServletResponse response) {
        try {
            DocumentVO vo = documentService.getById(id);
            if (vo == null || vo.getFilePath() == null) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("文档不存在");
                return;
            }

            Path filePath = Path.of(vo.getFilePath());
            if (!Files.exists(filePath)) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("文件不存在");
                return;
            }

            // 设置响应头
            String encodedFileName = URLEncoder.encode(vo.getTitle(), StandardCharsets.UTF_8).replace("+", "%20");
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", 
                    "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName);
            response.setContentLengthLong(Files.size(filePath));

            // 复制文件到响应流
            try (InputStream is = Files.newInputStream(filePath);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (Exception e) {
            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write("下载失败：" + e.getMessage());
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * 创建文档版本
     */
    @PostMapping("/version/{documentId}")
    public Result<Void> createVersion(
            @PathVariable Long documentId,
            @RequestParam String changeSummary,
            @RequestParam(defaultValue = "1") Long userId) {
        try {
            documentService.createVersion(documentId, changeSummary, userId);
            return Result.success("版本创建成功", null);
        } catch (Exception e) {
            return Result.error("版本创建失败：" + e.getMessage());
        }
    }

    /**
     * 获取文档版本历史
     */
    @GetMapping("/version/{documentId}")
    public Result<List<DocumentVersion>> getVersionHistory(@PathVariable Long documentId) {
        List<DocumentVersion> history = documentService.getVersionHistory(documentId);
        return Result.success(history);
    }
}
