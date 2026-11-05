package com.knowledge.agent.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 智能体工具注册与发现中心
 */
@Slf4j
@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> toolMap = new ConcurrentHashMap<>();

    public AgentToolRegistry(List<AgentTool> tools) {
        if (tools != null) {
            for (AgentTool tool : tools) {
                register(tool);
            }
        }
    }

    public void register(AgentTool tool) {
        toolMap.put(tool.getName(), tool);
        log.info("【Agent工具注册】注册工具: name={}, desc={}", tool.getName(), tool.getDescription());
    }

    public Optional<AgentTool> getTool(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(toolMap.get(name.trim()));
    }

    public Collection<AgentTool> getAllTools() {
        return Collections.unmodifiableCollection(toolMap.values());
    }

    /**
     * 生成供 Prompt 使用的工具列表描述规范
     */
    public String getToolsPromptDescription() {
        StringBuilder sb = new StringBuilder();
        for (AgentTool tool : toolMap.values()) {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }
        return sb.toString();
    }
}
