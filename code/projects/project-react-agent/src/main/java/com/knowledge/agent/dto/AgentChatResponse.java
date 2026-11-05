package com.knowledge.agent.dto;

import com.knowledge.agent.core.ReActEngine.StepTrace;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 智能体对话响应（含全链路执行追踪与最终答复）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentChatResponse {

    /**
     * 智能体最终给出的业务答复
     */
    private String finalAnswer;

    /**
     * 是否顺利推导演算完成
     */
    private boolean success;

    /**
     * 循环终止原因 (FINISHED, MAX_STEPS_EXCEEDED, ERROR)
     */
    private String stopReason;

    /**
     * ReAct 循环中各步骤的 Thought / Action / Observation 轨迹
     */
    private List<StepTrace> traces;
}
