package com.knowledge.agent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 智能体对话请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatRequest {

    /**
     * 用户自然语言指令
     */
    private String message;

    /**
     * 可选基准日期（例如 2026-11-03），便于自动化测试对齐确定性
     */
    private String currentDate;
}
