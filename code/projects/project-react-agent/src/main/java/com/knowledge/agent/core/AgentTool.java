package com.knowledge.agent.core;

/**
 * ReAct 智能体工具通用接口规范
 */
public interface AgentTool {

    /**
     * 工具全局唯一名称（例如 OrderQueryTool, LogisticsTool, CalculatorTool）
     */
    String getName();

    /**
     * 工具功能描述与入参规范提示（供 Agent 理解何时以及如何调用）
     */
    String getDescription();

    /**
     * 工具实际执行逻辑
     *
     * @param input 经过智能体解析提取的入参（通常为字符串或序列化 JSON）
     * @return 工具执行后的 Observation 观察结果文本
     */
    String execute(String input);
}
