---
title: 05 · Agent · MCP · Skills（索引）
tags:
  - 领域/AI应用
  - 类型/索引
  - 阶段/05
created: 2026-05-31
updated: 2026-11-01
status:
mastery:
---

# 🤖 05 · Agent · MCP · Skills

> [!abstract] 本章学什么
> 从一个能调用工具的 Agent 出发，了解不同决策方式，再学习怎样通过 MCP 接入外部能力、用 Skill 整理任务做法，以及如何评估和管理实际运行。

## 🗺️ 学习路线

第一次学习可以按下面的顺序：先弄懂一次工具调用，再看 Agent 如何根据结果决定下一步；MCP 和 Skill 是可选能力，不是每个 Agent 都必须使用。

```mermaid
flowchart LR
    A["Agent 与工具调用"] --> B["决策与编排"]
    B --> C["MCP：连接外部能力"]
    C --> D["Skills：整理任务做法"]
    D --> E["评估、权限与运行治理"]
```

## 📚 文档导航

### 先看全景

- [ ] [[00-Agent-MCP-Skills全景与心智模型|Agent、MCP 与 Skills 全景]]：用慢查询排查理解各自的作用。

### Agent：如何决定和执行

- [ ] [[01-Agent设计模式详解|Agent 设计模式]]：先了解几种模式的区别。
- [ ] [[02-Tool-Use工具调用-代码实践|工具调用实践]]：看清模型、应用和工具如何配合。
- [ ] [[03-ReAct-Spring-AI实现|ReAct 实现]]：根据每次工具结果决定下一步。
- [ ] [[04-Plan-and-Execute-实现|先规划再执行]]：拆解较复杂的多步骤任务。
- [ ] [[05-Reflection-实现|Reflection 实现]]：检查结果并决定是否重试。
- [ ] [[06-Multi-Agent-Supervisor实现|多 Agent 协作]]：由主管协调不同角色。
- [ ] [[07-LangGraph生产级Agent编排|LangGraph 编排]]：用状态图组织流程与恢复执行。
- [ ] [[08-OpenAI-Agents-SDK与Handoffs-Guardrails|Agents SDK 实践]]：了解交接和护栏。

### MCP：如何连接外部能力

- [ ] [[09-MCP协议详解|MCP 协议]]：认识 Host、Client、Server 和三类能力。
- [ ] [[10-MCP-Server开发-Java实战|MCP Server 开发]]：用 Java 提供工具。
- [ ] [[11-MCP-Client开发实战|MCP Client 接入]]：让 Spring AI 应用连接 Server。
- [ ] [[12-MCP调试-Inspector|MCP Inspector 调试]]：检查能力发现和调用结果。

### Skills：如何复用任务做法

- [ ] [[13-Skills详解|Skills 基础]]：理解 `SKILL.md` 和按需加载。
- [ ] [[14-设计自己的Skill实战|自定义 Skill]]：从会议纪要场景练习设计和验证。
- [ ] [[15-Skills在企业落地的模式|Skills 企业实践]]：考虑分发、版本和维护。
- [ ] [[16-AGENTS规范详解|AGENTS.md]]：了解项目指引与 Skill 的区别。
- [ ] [[17-Agent-MCP-Skills组合实战|三者组合实践]]：看它们怎样协作完成任务。

### 运行、评估与治理

- [ ] [[18-Agent可观测|可观测]]：追踪一次任务的调用、耗时和费用。
- [ ] [[19-Agent评估|评估]]：检查任务结果和过程是否符合预期。
- [ ] [[20-Agent安全与防注入|安全与提示注入]]：识别外部内容中的恶意指令。
- [ ] [[21-Agent权限治理与分级自治|权限与分级自治]]：为不同风险的操作设置边界。
- [ ] [[22-Agent成本控制与降级|成本控制与降级]]：限制调用成本并处理异常。
- [ ] [[99-本章总结与复习|本章总结与复习]]：串联概念并通过问题自测。

## 🔗 相关链接

- 上级总览：[[../00-总览/学习仪表盘]]
- 前置章节：[[../04-多轮对话与上下文工程/_index|04 · 多轮对话与上下文工程]]
- 下一章节：[[../06-平台与工程化/_index|06 · 平台与工程化]]
