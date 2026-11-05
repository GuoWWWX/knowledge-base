package com.knowledge.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库文档分块模型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    /**
     * 分块唯一标识
     */
    private String id;

    /**
     * 所属文档名称（如《企业差旅管理规范》）
     */
    private String docName;

    /**
     * 所属文档页码
     */
    private Integer pageNumber;

    /**
     * 分块正文内容
     */
    private String content;

    /**
     * 文本对应的向量嵌入 (Embedding)
     */
    private List<Double> embedding;

    /**
     * 多租户隔离租户ID
     */
    private String tenantId;

    /**
     * 检索时的相似度/置信度打分
     */
    private Double score;
}
