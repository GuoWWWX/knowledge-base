package com.knowledge.ai.agent;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * 实验：自主智能体内核 —— ReAct (Reasoning + Acting) 推理行动循环
 * ============================================================================
 * 
 * <h3>ReAct 核心架构与认知心智模型</h3>
 * 经典单一 Prompt 无法解决跨多系统联动与动态决策问题。
 * 普林斯顿大学与谷歌团队提出的 <b>ReAct (Reasoning and Acting)</b> 范式，为 Agent 赋予了“边思考、边行动、边观察、边修正”的自主生命周期：
 * <ul>
 *   <li><b>Thought (推理思考)</b>：模型分析当前目标、已知事实及潜在缺口，规划下一步动作；</li>
 *   <li><b>Action (行动工具)</b>：选择要调用的工具名称（如计算器、数据库、API）；</li>
 *   <li><b>Action Input (工具参数)</b>：构造符合该工具契约的结构化输入载荷；</li>
 *   <li><b>Observation (外部环境观察)</b>：外部执行引擎运行本地代码/网络请求，回填真实客观事实；</li>
 *   <li><b>Thought -> Final Answer (收敛终答)</b>：循环往复，直至 Agent 确认证据链完整，输出最终交付成果。</li>
 * </ul>
 * 
 * @author Knowledge Base Architecture Team
 * @version 1.0.0
 */
public class ReActAgentLoopDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("    智能体核心实验：手写 ReAct (Thought-Action-Observation) 推理行动闭环        ");
        System.out.println("================================================================================\n");

        // 1. 注册智能体可用工具箱 (Agent Tool Box)
        Map<String, AgentTool> toolBox = new LinkedHashMap<>();
        toolBox.put("order_query", new OrderQueryTool());
        toolBox.put("calculator", new CalculatorTool());

        // 2. 打印可用工具清单
        System.out.println(">>> [阶段 1] 智能体环境感知：已挂载工具列表：");
        for (AgentTool tool : toolBox.values()) {
            System.out.println("  • [" + tool.name() + "]: " + tool.description());
        }
        System.out.println("\n--------------------------------------------------------------------------------\n");

        // 3. 用户复杂任务提问 (需要跨“业务数据查询”与“数学精确折算”)
        String taskGoal = "请帮我查一下订单号 ORD-20261103-9872 的状态。如果已发货，请帮我计算里面单价 89.5 元的商品买了 4 件，再叠加上 8.5 折优惠后的实际应付总金额是多少？";
        System.out.println(">>> [阶段 2] 接收复杂自主任务目标 (User Goal)：");
        System.out.println("用户: \"" + taskGoal + "\"\n");

        // 4. 启动 ReAct 自主智能体循环引擎
        ReActAgentEngine agent = new ReActAgentEngine(toolBox, 6); // 最大迭代上限为 6 步，防止死循环
        String finalAnswer = agent.run(taskGoal);

        System.out.println("\n================================================================================");
        System.out.println(">>> [阶段 4] ReAct 智能体收敛交付的最终答案 (Final Answer)：");
        System.out.println("================================================================================");
        System.out.println(finalAnswer);
        System.out.println("================================================================================");
    }

    // ========================================================================
    // 智能体工具契约与具体实现
    // ========================================================================

    public interface AgentTool {
        String name();
        String description();
        String execute(String inputJson);
    }

    /**
     * 订单中心查询工具
     */
    public static class OrderQueryTool implements AgentTool {
        @Override
        public String name() {
            return "order_query";
        }

        @Override
        public String description() {
            return "用于查询电商订单状态、明细、商品单价与物流信息的工具。参数格式: {\"orderId\":\"...\"}";
        }

        @Override
        public String execute(String inputJson) {
            JSONObject json = JSON.parseObject(inputJson);
            String orderId = json.getString("orderId");
            System.out.println("      [物理工具执行] 正在连接电商订单中心数据库，检索单号: " + orderId);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("orderId", orderId);
            data.put("status", "DELIVERING");
            data.put("productName", "极客定制降噪机械键盘");
            data.put("unitPrice", 89.5);
            data.put("quantity", 4);
            data.put("logisticsCompany", "顺丰速运");
            data.put("trackingNo", "SF138982348");
            data.put("currentLocation", "北京大兴转运中心，已装车发往下一站");
            return JSON.toJSONString(data);
        }
    }

    /**
     * 高精度数学计算器工具 (克服 LLM 计算精度差的缺陷)
     */
    public static class CalculatorTool implements AgentTool {
        @Override
        public String name() {
            return "calculator";
        }

        @Override
        public String description() {
            return "执行高精度加减乘除数学表达式计算。参数格式: {\"expression\":\"89.5 * 4 * 0.85\"}";
        }

        @Override
        public String execute(String inputJson) {
            JSONObject json = JSON.parseObject(inputJson);
            String expr = json.getString("expression");
            System.out.println("      [物理工具执行] Calculator 计算引擎运算中，输入表达式: " + expr);

            // 针对 89.5 * 4 * 0.85 的模拟运算
            BigDecimal unit = new BigDecimal("89.5");
            BigDecimal qty = new BigDecimal("4");
            BigDecimal discount = new BigDecimal("0.85");
            BigDecimal result = unit.multiply(qty).multiply(discount).setScale(2, RoundingMode.HALF_UP);

            return result.toString();
        }
    }

    // ========================================================================
    // ReAct 自主智能体循环引擎 (ReActAgentEngine)
    // ========================================================================

    public static class ReActAgentEngine {
        private final Map<String, AgentTool> tools;
        private final int maxIterations;

        public ReActAgentEngine(Map<String, AgentTool> tools, int maxIterations) {
            this.tools = tools;
            this.maxIterations = maxIterations;
        }

        public String run(String userGoal) {
            StringBuilder trajectory = new StringBuilder();
            trajectory.append("User Goal: ").append(userGoal).append("\n");

            System.out.println(">>> [阶段 3] 启动 ReAct 自主推理与行动闭环 (Max Iterations: " + maxIterations + ")：\n");

            for (int step = 1; step <= maxIterations; step++) {
                System.out.println("┌── 【ReAct 循环 Step " + step + "】");

                // 1. 调用大模型生成当前步的思考与动作决策 (模拟 LLM 生成 Thought / Action / Final Answer)
                AgentStepDecision decision = mockLlmDecide(step, trajectory.toString());

                // 打印 Thought
                System.out.println("│  💡 Thought: " + decision.thought);
                trajectory.append("Thought: ").append(decision.thought).append("\n");

                // 2. 检查是否达到 Final Answer 终止态
                if (decision.isFinalAnswer) {
                    System.out.println("│  🏁 达到终止条件：生成最终交付答复！");
                    System.out.println("└── 循环收敛结束 ───────────────────────────────────────────────\n");
                    return decision.finalAnswer;
                }

                // 3. 执行行动 (Action + Action Input)
                System.out.println("│  🎯 Action: " + decision.actionName);
                System.out.println("│  📥 Action Input: " + decision.actionInput);
                trajectory.append("Action: ").append(decision.actionName).append("\n");
                trajectory.append("Action Input: ").append(decision.actionInput).append("\n");

                // 4. 环境交互与观察反馈 (Observation)
                AgentTool tool = tools.get(decision.actionName);
                String observation;
                if (tool != null) {
                    observation = tool.execute(decision.actionInput);
                } else {
                    observation = "Error: Tool [" + decision.actionName + "] not found in registry.";
                }

                System.out.println("│  👁  Observation: " + observation);
                trajectory.append("Observation: ").append(observation).append("\n");
                System.out.println("└── 步执行完成 ─────────────────────────────────────────────────\n");
            }

            return "未能于最大限制步数内完成全部推理任务。";
        }

        /**
         * 模拟大模型根据当前轨迹推断下一步动作
         */
        private AgentStepDecision mockLlmDecide(int step, String currentTrajectory) {
            if (step == 1) {
                return new AgentStepDecision(
                        "用户想查询订单状态并核算优惠后的总金额。我应当首先通过订单查询工具获得订单详情和物流状态。",
                        "order_query",
                        "{\"orderId\":\"ORD-20261103-9872\"}",
                        false,
                        null
                );
            } else if (step == 2) {
                return new AgentStepDecision(
                        "根据第一步的 Observation，订单状态为 DELIVERING (派送中)，单价为 89.5，购买数量为 4。用户要求在 4 件的基础上计算 8.5 折优惠。由于大模型不擅长直接进行浮点数心算，我应调用专业计算器工具进行准确运算。",
                        "calculator",
                        "{\"expression\":\"89.5 * 4 * 0.85\"}",
                        false,
                        null
                );
            } else {
                return new AgentStepDecision(
                        "我已经获得了订单当前物流状态（顺丰速运派送中），并且利用计算器获得了精准折后金额（304.30 元）。所有事实完备，我可以直接总结生成最终答案。",
                        null,
                        null,
                        true,
                        "尊敬的客户，已为您查询并核算完毕：\n" +
                                "1. 【订单物流】：您的订单 ORD-20261103-9872 已经发货！当前状态为【顺丰速运派送中】，单号 SF138982348，最新轨迹位于“北京大兴转运中心，已装车发往下一站”；\n" +
                                "2. 【费用核算】：购买商品为《极客定制降噪机械键盘》，单价 89.5 元，购买数量 4 件（原总价 358.00 元）。叠加 8.5 折优惠后的实际应付总金额为：¥304.30 元。"
                );
            }
        }
    }

    @Data
    @AllArgsConstructor
    public static class AgentStepDecision {
        private String thought;
        private String actionName;
        private String actionInput;
        private boolean isFinalAnswer;
        private String finalAnswer;
    }
}
