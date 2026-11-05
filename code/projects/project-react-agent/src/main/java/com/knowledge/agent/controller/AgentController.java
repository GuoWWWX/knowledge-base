package com.knowledge.agent.controller;

import com.knowledge.agent.core.ReActEngine;
import com.knowledge.agent.dto.AgentChatRequest;
import com.knowledge.agent.dto.AgentChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ReAct 智能体交互 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final ReActEngine reActEngine;

    /**
     * 接收自然语言意图并触发 ReAct 多步自主推理
     */
    @PostMapping("/chat")
    public ResponseEntity<AgentChatResponse> chat(@RequestBody AgentChatRequest request) {
        if (request == null || request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(AgentChatResponse.builder()
                    .finalAnswer("用户指令不能为空")
                    .success(false)
                    .stopReason("INVALID_INPUT")
                    .build());
        }

        ReActEngine.AgentExecutionResult result = reActEngine.run(request.getMessage(), request.getCurrentDate());

        AgentChatResponse response = AgentChatResponse.builder()
                .finalAnswer(result.getFinalAnswer())
                .success(result.isSuccess())
                .stopReason(result.getStopReason())
                .traces(result.getTraces())
                .build();

        return ResponseEntity.ok(response);
    }
}
