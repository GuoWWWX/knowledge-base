package com.knowledge.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG 智能问答提问响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagAskResponse {

    /**
     * 最终受控回答内容
     */
    private String answer;

    /**
     * 是否成功命中知识库
     */
    private boolean hit;

    /**
     * 最高置信度打分
     */
    private Double confidenceScore;

    /**
     * 召回的知识引用来源列表
     */
    private List<ReferenceItem> references;

    /**
     * 组装好的严格 System/User Prompt（便于可观测与排错）
     */
    private String strictPrompt;

    /**
     * 引用溯源条目
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferenceItem {
        private String docName;
        private Integer pageNumber;
        private String contentSnippet;
        private Double similarityScore;
    }
}
