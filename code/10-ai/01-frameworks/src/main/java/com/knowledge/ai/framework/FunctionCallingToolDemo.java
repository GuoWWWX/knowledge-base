package com.knowledge.ai.framework;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

/**
 * ============================================================================
 * 实验：Spring AI Function Calling (Tool Use) 工具调用全链路执行机制
 * ============================================================================
 * 
 * <h3>核心原理与执行闭环 (The Tool Calling Loop)</h3>
 * 大语言模型本身无法直接访问实时互联网信息或执行本地业务代码，其核心能力在于<b>意图理解与参数结构化析取</b>。
 * Spring AI 工具调用的底层架构闭环包含 5 个关键步骤：
 * <ol>
 *   <li><b>工具契约注册</b>：开发者定义带 {@code @Description} 的 Java Function，框架自动将其转换为 JSON Schema 描述并在请求时传递给模型。</li>
 *   <li><b>模型决策意图 (Tool Call Decision)</b>：大模型判断是否需要调用工具，若需要则返回 {@code finish_reason: tool_calls} 及参数 JSON。</li>
 *   <li><b>框架拦截与本地调度 (Local Dispatching)</b>：Spring AI 框架拦截响应，解析调用参数并路由到本地对应的 Java Bean/方法执行。</li>
 *   <li><b>执行结果回填 (Context Feedback)</b>：将工具输出结果封装为 {@code ToolResponseMessage} 追加进对话历史。</li>
 *   <li><b>次轮最终合成 (Final Synthesis)</b>：模型结合多路工具执行的真实事实，合成最终对用户友好的自然语言答复。</li>
 * </ol>
 * 
 * @author Knowledge Base Architecture Team
 * @version 1.0.0
 */
public class FunctionCallingToolDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("    Spring AI 工具调用机制实验：Function Calling / Tool Execution 闭环演练       ");
        System.out.println("================================================================================\n");

        // 1. 注册本地工具函数箱 (Tool Registry)
        ToolRegistry registry = new ToolRegistry();
        registry.registerTool(new WeatherTool());
        registry.registerTool(new CurrencyExchangeTool());

        System.out.println(">>> [阶段 1] 系统已就绪，已向 Spring AI 容器注册本地工具列表：");
        for (ToolCallback tool : registry.getTools()) {
            System.out.println("  • 工具名称: " + tool.getName() + " | 说明: " + tool.getDescription());
            System.out.println("    输入参数 Schema: " + tool.getInputSchemaJson());
        }
        System.out.println("\n--------------------------------------------------------------------------------\n");

        // 2. 模拟用户提问（包含多工具组合诉求）
        String userQuery = "你好，请帮我查一下北京今天的天气如何？另外，我打算将 1500 美元 (USD) 兑换成人民币 (CNY)，大概能换多少钱？";
        System.out.println(">>> [阶段 2] 接收用户复杂多工具提问：");
        System.out.println("用户: \"" + userQuery + "\"\n");

        // 3. 构建模拟的智能模型调度引擎 (MockSpringAiExecutionEngine)
        MockSpringAiExecutionEngine engine = new MockSpringAiExecutionEngine(registry);

        // 4. 执行完整的两阶段工具调用循环 (Two-Turn Conversation)
        String finalAnswer = engine.execute(userQuery);

        System.out.println("\n>>> [阶段 5] 最终大模型向用户输出的完整解答：");
        System.out.println("================================================================================");
        System.out.println(finalAnswer);
        System.out.println("================================================================================");
    }

    // ========================================================================
    // 业务工具实现类 (仿照 Spring AI @Bean Function 机制)
    // ========================================================================

    /**
     * 天气查询工具入参
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherRequest {
        private String city;
    }

    /**
     * 天气查询工具出参
     */
    @Data
    @AllArgsConstructor
    public static class WeatherResponse {
        private String city;
        private String condition;
        private double temperature;
        private String airQuality;
        private String updateDate;
    }

    /**
     * 天气查询工具实现 (implements Function<WeatherRequest, WeatherResponse>)
     */
    public static class WeatherTool implements ToolCallback {
        @Override
        public String getName() {
            return "getWeather";
        }

        @Override
        public String getDescription() {
            return "根据给定的城市名称查询当天的实时天气、温度与空气质量指数";
        }

        @Override
        public String getInputSchemaJson() {
            return "{\"type\":\"object\",\"properties\":{\"city\":{\"type\":\"string\",\"description\":\"城市中文名，如北京、上海\"}},\"required\":[\"city\"]}";
        }

        @Override
        public String call(String jsonArguments) {
            WeatherRequest req = JSON.parseObject(jsonArguments, WeatherRequest.class);
            System.out.println("    [本地执行] WeatherTool 正在调用气象权威接口，查询城市: " + req.getCity());
            WeatherResponse response = new WeatherResponse(
                    req.getCity(),
                    "晴转多云，微风",
                    23.5,
                    "优 (AQI 35)",
                    LocalDate.now().toString()
            );
            return JSON.toJSONString(response);
        }
    }

    /**
     * 汇率兑换工具入参
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrencyRequest {
        private String fromCurrency;
        private String toCurrency;
        private BigDecimal amount;
    }

    /**
     * 汇率兑换工具出参
     */
    @Data
    @AllArgsConstructor
    public static class CurrencyResponse {
        private String from;
        private String to;
        private BigDecimal originalAmount;
        private BigDecimal rate;
        private BigDecimal convertedAmount;
        private String quoteTime;
    }

    /**
     * 汇率兑换工具实现
     */
    public static class CurrencyExchangeTool implements ToolCallback {
        @Override
        public String getName() {
            return "exchangeCurrency";
        }

        @Override
        public String getDescription() {
            return "提供两种法定货币之间的实时汇率计算与金额兑换";
        }

        @Override
        public String getInputSchemaJson() {
            return "{\"type\":\"object\",\"properties\":{\"fromCurrency\":{\"type\":\"string\",\"description\":\"源币种代码，如 USD\"},\"toCurrency\":{\"type\":\"string\",\"description\":\"目标币种代码，如 CNY\"},\"amount\":{\"type\":\"number\",\"description\":\"兑换金额\"}},\"required\":[\"fromCurrency\",\"toCurrency\",\"amount\"]}";
        }

        @Override
        public String call(String jsonArguments) {
            CurrencyRequest req = JSON.parseObject(jsonArguments, CurrencyRequest.class);
            System.out.println("    [本地执行] CurrencyExchangeTool 正在调用中央银行汇率接口: " + req.getFromCurrency() + " -> " + req.getToCurrency());
            
            BigDecimal rate = BigDecimal.valueOf(7.2350); // 假定当前 1 USD = 7.2350 CNY
            BigDecimal converted = req.getAmount().multiply(rate).setScale(2, RoundingMode.HALF_UP);

            CurrencyResponse response = new CurrencyResponse(
                    req.getFromCurrency(),
                    req.getToCurrency(),
                    req.getAmount(),
                    rate,
                    converted,
                    "2026-11-03 14:30:00 CST"
            );
            return JSON.toJSONString(response);
        }
    }

    // ========================================================================
    // Spring AI 工具抽象核心框架组件
    // ========================================================================

    public interface ToolCallback {
        String getName();
        String getDescription();
        String getInputSchemaJson();
        String call(String jsonArguments);
    }

    public static class ToolRegistry {
        private final Map<String, ToolCallback> toolMap = new HashMap<>();

        public void registerTool(ToolCallback tool) {
            toolMap.put(tool.getName(), tool);
        }

        public ToolCallback getTool(String name) {
            return toolMap.get(name);
        }

        public Collection<ToolCallback> getTools() {
            return toolMap.values();
        }
    }

    public record ToolCall(String id, String toolName, String argumentsJson) {}

    /**
     * 模拟 Spring AI 内部编排执行器
     */
    public static class MockSpringAiExecutionEngine {
        private final ToolRegistry registry;

        public MockSpringAiExecutionEngine(ToolRegistry registry) {
            this.registry = registry;
        }

        public String execute(String userQuery) {
            System.out.println(">>> [阶段 3] 大模型第一轮推理 (First Turn)：分析用户意图并生成工具调用指令 (Tool Calls)");

            // 模拟模型识别意图，生成两个并行 ToolCall
            List<ToolCall> toolCalls = List.of(
                    new ToolCall("call_wx_001", "getWeather", "{\"city\":\"北京\"}"),
                    new ToolCall("call_fx_002", "exchangeCurrency", "{\"fromCurrency\":\"USD\",\"toCurrency\":\"CNY\",\"amount\":1500}")
            );

            System.out.println("  • 模型判定: 需调用 " + toolCalls.size() + " 个外部工具来满足实时事实需求。");
            for (ToolCall tc : toolCalls) {
                System.out.println("    -> 工具请求 ID: " + tc.id() + ", 函数: " + tc.toolName() + ", 参数: " + tc.argumentsJson());
            }

            System.out.println("\n>>> [阶段 4] Spring AI 框架层介入：拦截模型指令，本地多线程/顺序调度执行工具：");
            Map<String, String> toolResults = new LinkedHashMap<>();
            for (ToolCall tc : toolCalls) {
                ToolCallback callback = registry.getTool(tc.toolName());
                if (callback != null) {
                    String resultJson = callback.call(tc.argumentsJson());
                    toolResults.put(tc.id(), resultJson);
                    System.out.println("    <- 工具 " + tc.toolName() + " 返回 Observation 报文:\n       " + resultJson);
                }
            }

            // 模拟第二轮请求：模型获得 Observation 后的最终合成
            System.out.println("\n>>> [阶段 4.1] 回填工具结果至对话上下文 (ToolResponseMessage)，触发第二轮最终合成...");
            
            return "您好！已为您查询到最新信息：\n" +
                    "1. 【北京天气】：今日北京为 晴转多云，微风，实时气温约 23.5℃，空气质量优秀 (AQI 35)，非常适宜户外出行。\n" +
                    "2. 【汇率换算】：按当前最新参考汇率 (1 USD ≈ 7.2350 CNY) 计算，您的 1,500 美元可兑换约 10,852.50 元人民币。\n" +
                    "(数据更新时间：2026-11-03 14:30，实际换算请以银行柜台结汇牌价为准。)";
        }
    }
}
