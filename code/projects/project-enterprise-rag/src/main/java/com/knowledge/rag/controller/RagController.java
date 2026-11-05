package com.knowledge.rag.controller;

import com.knowledge.rag.dto.DocumentUploadRequest;
import com.knowledge.rag.dto.RagAskRequest;
import com.knowledge.rag.dto.RagAskResponse;
import com.knowledge.rag.model.DocumentChunk;
import com.knowledge.rag.service.KnowledgeStoreService;
import com.knowledge.rag.service.RagQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 企业知识库 RAG 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final KnowledgeStoreService knowledgeStoreService;
    private final RagQueryService ragQueryService;

    /**
     * 录入/上传企业文档并自动切分向量化入库
     */
    @PostMapping("/documents")
    public ResponseEntity<Map<String, Object>> uploadDocument(@RequestBody DocumentUploadRequest request) {
        if (request.getContent() == null || request.getContent().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", "文档正文内容不能为空"));
        }

        String tenantId = (request.getTenantId() == null || request.getTenantId().isBlank())
                ? "default" : request.getTenantId();
        String docName = (request.getDocName() == null || request.getDocName().isBlank())
                ? "未命名规范" : request.getDocName();
        int pageNumber = request.getPageNumber() != null ? request.getPageNumber() : 1;

        // 支持分页标记切割（例如 '---PAGE N---' 或换行分段）
        String[] sections = request.getContent().split("---PAGE\\s*(\\d+)---");
        List<DocumentChunk> chunks = new ArrayList<>();

        if (sections.length > 1) {
            // 包含显式页码切分标记
            int currentPage = pageNumber;
            for (String section : sections) {
                String trimmed = section.trim();
                if (!trimmed.isEmpty()) {
                    DocumentChunk chunk = DocumentChunk.builder()
                            .id(UUID.randomUUID().toString())
                            .docName(docName)
                            .pageNumber(currentPage)
                            .content(trimmed)
                            .tenantId(tenantId)
                            .build();
                    chunks.add(chunk);
                    currentPage++;
                }
            }
        } else {
            // 单页/常规分块录入
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(UUID.randomUUID().toString())
                    .docName(docName)
                    .pageNumber(pageNumber)
                    .content(request.getContent().trim())
                    .tenantId(tenantId)
                    .build();
            chunks.add(chunk);
        }

        knowledgeStoreService.storeAll(chunks);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", "文档录入与向量化切分成功");
        result.put("tenantId", tenantId);
        result.put("docName", docName);
        result.put("chunksCreated", chunks.size());

        return ResponseEntity.ok(result);
    }

    /**
     * RAG 受控智能问答接口
     */
    @PostMapping("/ask")
    public ResponseEntity<RagAskResponse> ask(@RequestBody RagAskRequest request) {
        RagAskResponse response = ragQueryService.ask(request);
        return ResponseEntity.ok(response);
    }
}
