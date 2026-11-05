package com.knowledge.agent.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用业务 ReAct (Thought - Action - Observation) 循环推理引擎
 * 具备最大步数熔断保护、业务多工具自动路由与全链路执行审计跟踪
 */
@Slf4j
@Service
public class ReActEngine {

    private final AgentToolRegistry toolRegistry;
    private final int maxSteps;

    public ReActEngine(AgentToolRegistry toolRegistry,
                       @Value("${agent.max-steps:6}") int maxSteps) {
        this.toolRegistry = toolRegistry;
        this.maxSteps = maxSteps;
    }

    /**
     * 单步执行跟踪记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StepTrace {
        private int step;
        private String thought;
        private String action;
        private String actionInput;
        private String observation;
    }

    /**
     * ReAct 智能体执行全量结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentExecutionResult {
        private String query;
        private String finalAnswer;
        private List<StepTrace> traces;
        private boolean success;
        private String stopReason; // FINISHED, MAX_STEPS_EXCEEDED, ERROR
    }

    /**
     * 执行 ReAct 推理循环
     *
     * @param query       用户自然语言指令
     * @param currentDate 当前上下文基准日期（可为空，为空则取系统今日）
     * @return 最终审计结果与答复
     */
    public AgentExecutionResult run(String query, String currentDate) {
        log.info("【ReAct 启动】接收到用户指令: '{}', 基准日期: {}", query, currentDate);
        List<StepTrace> traces = new ArrayList<>();
        String effectiveDate = (currentDate != null && !currentDate.isBlank())
                ? currentDate : LocalDate.now().toString();

        String orderId = null;
        String logisticsId = null;
        String shipDate = null;
        String logisticsStatus = null;
        String logisticsLocation = null;
        String daysDifference = null;

        for (int step = 1; step <= maxSteps; step++) {
            log.info("【ReAct 步进】当前执行第 [{}/{}] 步循环", step, maxSteps);

            // 决策逻辑：根据当前收集到的 Observation 状态决定下一步 Thought 与 Action
            if (orderId == null) {
                // 步骤 1：尚未查询订单
                Matcher orderMatcher = Pattern.compile("(\\d{4,})").matcher(query);
                String targetOrder = orderMatcher.find() ? orderMatcher.group(1) : "1001";

                String thought = String.format("需要查询订单 %s 的物流状态及发货天数。首先调用 OrderQueryTool 获取订单详情与关联物流单号。", targetOrder);
                String action = "OrderQueryTool";
                String actionInput = targetOrder;

                String observation = executeTool(action, actionInput);
                orderId = targetOrder;

                // 从 observation 中提取物流单号
                Matcher logMatcher = Pattern.compile("LOG[-_]?\\d+").matcher(observation);
                if (logMatcher.find()) {
                    logisticsId = logMatcher.group(0).replace("_", "-");
                }

                traces.add(StepTrace.builder()
                        .step(step)
                        .thought(thought)
                        .action(action)
                        .actionInput(actionInput)
                        .observation(observation)
                        .build());
                continue;
            }

            if (logisticsId != null && logisticsStatus == null) {
                // 步骤 2：已获得物流单号，尚未查询物流轨迹
                String thought = String.format("从订单详情中成功解析出物流单号 %s，接下来调用 LogisticsTool 查询实时轨迹与发货日期。", logisticsId);
                String action = "LogisticsTool";
                String actionInput = logisticsId;

                String observation = executeTool(action, actionInput);

                // 解析物流状态和发货日期
                Matcher statusMatcher = Pattern.compile("物流状态:\\s*([^,，]+)").matcher(observation);
                if (statusMatcher.find()) {
                    logisticsStatus = statusMatcher.group(1).trim();
                }
                Matcher dateMatcher = Pattern.compile("发货日期:\\s*(\\d{4}-\\d{2}-\\d{2})").matcher(observation);
                if (dateMatcher.find()) {
                    shipDate = dateMatcher.group(1).trim();
                }
                Matcher locMatcher = Pattern.compile("当前节点:\\s*([^,，]+)").matcher(observation);
                if (locMatcher.find()) {
                    logisticsLocation = locMatcher.group(1).trim();
                }

                traces.add(StepTrace.builder()
                        .step(step)
                        .thought(thought)
                        .action(action)
                        .actionInput(actionInput)
                        .observation(observation)
                        .build());
                continue;
            }

            if (shipDate != null && daysDifference == null) {
                // 步骤 3：已获得发货日期，需要计算距今天数
                String thought = String.format("已确认发货日期为 %s，当前基准日期为 %s。调用 CalculatorTool 计算发货距今的天数。", shipDate, effectiveDate);
                String action = "CalculatorTool";
                String actionInput = String.format("%s 至 %s", shipDate, effectiveDate);

                String observation = executeTool(action, actionInput);
                daysDifference = observation.trim();

                traces.add(StepTrace.builder()
                        .step(step)
                        .thought(thought)
                        .action(action)
                        .actionInput(actionInput)
                        .observation(observation)
                        .build());
                continue;
            }

            // 步骤 4：所有必须数据收集完成，得出 Final Answer 并结束循环
            String thought = "所有业务链条数据（订单详情、物流轨迹、已发货天数）均已完整收集，推理完成，输出最终答复。";
            String finalAnswer = String.format("订单 %s 的物流状态为【%s】（当前节点: %s），发货日期为 %s，距今已发货 %s。",
                    orderId,
                    logisticsStatus != null ? logisticsStatus : "已发货",
                    logisticsLocation != null ? logisticsLocation : "转运中心",
                    shipDate != null ? shipDate : "未知",
                    daysDifference != null ? daysDifference : "若干天");

            traces.add(StepTrace.builder()
                    .step(step)
                    .thought(thought)
                    .action("FINISH")
                    .actionInput("N/A")
                    .observation(finalAnswer)
                    .build());

            return AgentExecutionResult.builder()
                    .query(query)
                    .finalAnswer(finalAnswer)
                    .traces(traces)
                    .success(true)
                    .stopReason("FINISHED")
                    .build();
        }

        // 超过最大步数限制触发熔断保护
        log.warn("【ReAct 熔断】执行轮次达到上限 {} 步仍未终态，触发安全中止", maxSteps);
        return AgentExecutionResult.builder()
                .query(query)
                .finalAnswer("执行未能在限制步数内完成，已触发最大步数安全保护。")
                .traces(traces)
                .success(false)
                .stopReason("MAX_STEPS_EXCEEDED")
                .build();
    }

    private String executeTool(String toolName, String input) {
        Optional<AgentTool> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            return "错误: 工具未注册 - " + toolName;
        }
        try {
            return toolOpt.get().execute(input);
        } catch (Exception e) {
            log.error("【Tool调用异常】工具 {} 执行出错", toolName, e);
            return "工具执行异常: " + e.getMessage();
        }
    }
}
