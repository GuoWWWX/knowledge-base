package com.knowledge.rag;

import com.knowledge.rag.dto.DocumentUploadRequest;
import com.knowledge.rag.dto.RagAskRequest;
import com.knowledge.rag.dto.RagAskResponse;
import com.knowledge.rag.service.KnowledgeStoreService;
import com.knowledge.rag.service.RagQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EnterpriseRagApplicationTest {

    @Autowired
    private KnowledgeStoreService knowledgeStoreService;

    @Autowired
    private RagQueryService ragQueryService;

    @Autowired
    private com.knowledge.rag.controller.RagController ragController;

    private static final String TENANT_FINANCE = "tenant-finance-001";
    private static final String TENANT_OTHER = "tenant-other-002";

    @BeforeEach
    void setUp() {
        knowledgeStoreService.clearAll();

        // 录入企业差旅报销制度文档（第3页）
        DocumentUploadRequest travelDoc = DocumentUploadRequest.builder()
                .tenantId(TENANT_FINANCE)
                .docName("企业差旅管理规范")
                .pageNumber(3)
                .content("北京地区普通员工住宿标准上限为550元/天，超出部分需员工自理；部门经理标准为800元/天。")
                .build();
        ragController.uploadDocument(travelDoc);

        // 录入考勤假勤制度（第1页）
        DocumentUploadRequest attendanceDoc = DocumentUploadRequest.builder()
                .tenantId(TENANT_FINANCE)
                .docName("员工考勤管理规范")
                .pageNumber(1)
                .content("员工每月享有1天全薪病假，请假需在钉钉系统提交二级甲等医院就医证明。")
                .build();
        ragController.uploadDocument(attendanceDoc);
    }

    @Test
    @DisplayName("验证准确命中知识库并返回550元住宿上限与第3页出处引用")
    void testAskTravelLodgingLimitSuccess() {
        RagAskRequest request = RagAskRequest.builder()
                .tenantId(TENANT_FINANCE)
                .question("普通员工北京住宿上限是多少？")
                .topK(3)
                .minScore(0.20)
                .build();

        RagAskResponse response = ragQueryService.ask(request);

        assertNotNull(response);
        assertTrue(response.isHit(), "应该成功命中知识库");
        assertNotNull(response.getConfidenceScore(), "置信度不应为空");
        assertTrue(response.getConfidenceScore() >= 0.20, "置信度应高于门限");

        // 断言回答准确性
        String answer = response.getAnswer();
        assertTrue(answer.contains("550"), "回答中应包含住宿上限 550 元");
        assertTrue(answer.contains("企业差旅管理规范"), "回答中应包含出处文档名");
        assertTrue(answer.contains("3"), "回答中应包含出处页码第3页");

        // 断言引用元数据
        assertFalse(response.getReferences().isEmpty(), "引用列表不应为空");
        RagAskResponse.ReferenceItem firstRef = response.getReferences().get(0);
        assertEquals("企业差旅管理规范", firstRef.getDocName());
        assertEquals(3, firstRef.getPageNumber());
        assertTrue(firstRef.getContentSnippet().contains("550元"));
    }

    @Test
    @DisplayName("验证检索无相关规范时触发置信度门限兜底防御")
    void testAskIrrelevantQuestionFallback() {
        RagAskRequest request = RagAskRequest.builder()
                .tenantId(TENANT_FINANCE)
                .question("公司食堂星期天中午开门供餐吗？")
                .topK(3)
                .build();

        RagAskResponse response = ragQueryService.ask(request);

        assertNotNull(response);
        assertFalse(response.isHit(), "未录入该领域知识，不应命中");
        assertEquals("未检索到相关企业规范", response.getAnswer(), "应返回受控兜底回复");
        assertTrue(response.getReferences().isEmpty(), "不相关问题不应携带引用出处");
    }

    @Test
    @DisplayName("验证多租户硬隔离，防止未授权租户越权召回敏感制度")
    void testMultiTenantIsolation() {
        RagAskRequest request = RagAskRequest.builder()
                .tenantId(TENANT_OTHER) // 未授权租户
                .question("普通员工北京住宿上限是多少？")
                .topK(3)
                .build();

        RagAskResponse response = ragQueryService.ask(request);

        assertNotNull(response);
        assertFalse(response.isHit(), "未授权租户不应命中其他租户的知识库");
        assertEquals("未检索到相关企业规范", response.getAnswer());
    }
}
