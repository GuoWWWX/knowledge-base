package com.knowledge.rag.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 知识库文档上传/录入请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentUploadRequest {

    /**
     * 租户标识
     */
    private String tenantId;

    /**
     * 文档名称（如《企业差旅管理规范》）
     */
    private String docName;

    /**
     * 页码
     */
    private Integer pageNumber;

    /**
     * 文本内容
     */
    private String content;
}
