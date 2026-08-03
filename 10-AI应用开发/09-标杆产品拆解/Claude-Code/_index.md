---
title: Claude Code 拆解索引
tags: [领域/AI应用, 类型/索引, 主题/ClaudeCode]
created: 2026-07-08
updated: 2026-07-08
status: 已完成
mastery: ⭐☆☆☆☆
---

# Claude Code 拆解索引

> [!info] 本区说明
> Claude Code 是 Anthropic 的 agentic coding tool。这里按 terminal-native 产品体验、上下文记忆、工具系统和扩展机制拆解。

## 文档清单

- [[01-Claude-Code产品定位与总体架构]]
- [[02-Claude-Code一次任务执行全链路]]
- [[03-Claude-Code上下文工程-CLAUDE-md-Memory]]
- [[04-Claude-Code工具系统-MCP-Hooks-Subagents-Skills]]
- [[99-Claude-Code总结与复习]]

## 学习顺序

1. 先读 [[01-Claude-Code产品定位与总体架构]]，建立产品心智。
2. 再读 [[02-Claude-Code一次任务执行全链路]]，理解一次任务的完整链路。
3. 然后按需深入 [[03-Claude-Code上下文工程-CLAUDE-md-Memory]] 与 [[04-Claude-Code工具系统-MCP-Hooks-Subagents-Skills]]。
4. 最后用 [[99-Claude-Code总结与复习]] 自测，并对照 [[../02-编程Agent通用架构图]] 回看六层架构。

## 拆解重点

| 主题 | 学什么 |
| ---- | ------ |
| Terminal-native | 为什么从终端进入代码库是强体验 |
| CLAUDE.md / Memory | 如何沉淀项目上下文和团队规则 |
| Settings / Permissions | 如何控制工具和行为 |
| MCP | 如何连接外部工具 |
| Hooks | 如何在生命周期节点插入自动化和治理 |
| Subagents | 如何把复杂任务拆给专业代理 |
| Skills | 如何把重复工作流变成可加载能力 |

## 与现有笔记的关系

- [[../../05-Agent-MCP-Skills/13-Skills详解]]
- [[../../05-Agent-MCP-Skills/09-MCP协议详解]]
- [[../../05-Agent-MCP-Skills/06-Multi-Agent-Supervisor实现]]
- [[../../05-Agent-MCP-Skills/20-Agent安全与防注入]]

## 📚 参考资料

- [Anthropic Claude Code Overview](https://docs.anthropic.com/en/docs/claude-code/overview)
- [Claude Code Memory](https://docs.anthropic.com/en/docs/claude-code/memory)
- [Claude Code MCP](https://docs.anthropic.com/en/docs/claude-code/mcp)
- [Claude Code Hooks](https://docs.anthropic.com/en/docs/claude-code/hooks)
- [Claude Code Subagents](https://docs.anthropic.com/en/docs/claude-code/sub-agents)
- [Claude Code Skills](https://docs.anthropic.com/en/docs/claude-code/skills)
