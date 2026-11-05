package com.knowledge.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG 智能问答提问请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagAskRequest {

    /**
     * 租户标识（默认 default）
     */
    private String tenantId;

    /**
     * 用户提问
     */
    private String question;

    /**
     * Top-K 召回条数限制（默认 3）
     */
    private Integer topK;

    /**
     * 最小相似度阈值（若为空则使用系统配置的置信度门限）
     */
    private Double minScore;
}
