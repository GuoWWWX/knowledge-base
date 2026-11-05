package com.knowledge.agent;

import com.knowledge.agent.controller.AgentController;
import com.knowledge.agent.core.AgentToolRegistry;
import com.knowledge.agent.core.ReActEngine;
import com.knowledge.agent.dto.AgentChatRequest;
import com.knowledge.agent.dto.AgentChatResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReactAgentApplicationTest {

    @Autowired
    private AgentController agentController;

    @Autowired
    private ReActEngine reActEngine;

    @Autowired
    private AgentToolRegistry toolRegistry;

    @Test
    @DisplayName("验证工具注册中心成功装配 OrderQueryTool, LogisticsTool, CalculatorTool")
    void testToolsRegistered() {
        assertTrue(toolRegistry.getTool("OrderQueryTool").isPresent(), "OrderQueryTool 应该已注册");
        assertTrue(toolRegistry.getTool("LogisticsTool").isPresent(), "LogisticsTool 应该已注册");
        assertTrue(toolRegistry.getTool("CalculatorTool").isPresent(), "CalculatorTool 应该已注册");
    }

    @Test
    @DisplayName("验证多步自主推理执行：查询订单 1001 的物流状态并计算距今已发货多少天")
    void testMultiStepReActExecution() {
        AgentChatRequest request = AgentChatRequest.builder()
                .message("查询订单 1001 的物流状态并计算距今已发货多少天")
                .currentDate("2026-11-03")
                .build();

        ResponseEntity<AgentChatResponse> responseEntity = agentController.chat(request);

        assertNotNull(responseEntity);
        assertEquals(200, responseEntity.getStatusCode().value());

        AgentChatResponse response = responseEntity.getBody();
        assertNotNull(response);
        assertTrue(response.isSuccess(), "智能体应顺利完成推理执行");
        assertEquals("FINISHED", response.getStopReason(), "终止状态应为 FINISHED");

        // 验证轨迹步骤
        assertNotNull(response.getTraces());
        assertTrue(response.getTraces().size() >= 4, "至少应包含订单查询、物流查询、天数计算与终态答复 4 个步骤");

        // 验证各步骤行动
        ReActEngine.StepTrace step1 = response.getTraces().get(0);
        assertEquals("OrderQueryTool", step1.getAction());
        assertTrue(step1.getObservation().contains("LOG-9527"));

        ReActEngine.StepTrace step2 = response.getTraces().get(1);
        assertEquals("LogisticsTool", step2.getAction());
        assertTrue(step2.getObservation().contains("已发货"));
        assertTrue(step2.getObservation().contains("2026-10-30"));

        ReActEngine.StepTrace step3 = response.getTraces().get(2);
        assertEquals("CalculatorTool", step3.getAction());
        assertTrue(step3.getObservation().contains("4 天"));

        // 验证最终答复
        String finalAnswer = response.getFinalAnswer();
        assertNotNull(finalAnswer);
        assertTrue(finalAnswer.contains("1001"), "答复中应包含订单号 1001");
        assertTrue(finalAnswer.contains("已发货"), "答复中应包含物流状态【已发货】");
        assertTrue(finalAnswer.contains("4 天") || finalAnswer.contains("4天"), "答复中应包含已发货天数 4 天");
    }

    @Test
    @DisplayName("验证受限步数下的最大步数安全熔断保护机制")
    void testMaxStepsProtection() {
        // 创建一个仅允许 2 步的引擎实例以触发步数熔断
        ReActEngine restrictedEngine = new ReActEngine(toolRegistry, 2);

        ReActEngine.AgentExecutionResult result = restrictedEngine.run("查询订单 1001 的物流状态并计算距今已发货多少天", "2026-11-03");

        assertNotNull(result);
        assertFalse(result.isSuccess(), "步数被限制后应触发中止保护");
        assertEquals("MAX_STEPS_EXCEEDED", result.getStopReason(), "终止原因应为 MAX_STEPS_EXCEEDED");
        assertEquals(2, result.getTraces().size(), "轨迹记录应恰好为 2 步");
    }
}
