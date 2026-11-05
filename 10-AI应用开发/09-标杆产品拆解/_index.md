---
title: 09 · 标杆产品拆解（索引）
tags:
  - 领域/AI应用
  - 类型/索引
  - 阶段/09
created: 2026-07-08
updated: 2026-07-08
status: 已完成
mastery: ⭐☆☆☆☆
---

# 09 · 标杆产品拆解

> [!info] 本区说明
> 本章拆解行业主流编程 Agent 产品，重点分析其上下文工程、工具调用、权限治理、工作流编排、扩展机制与交互体验设计。
> 其中架构图是便于学习的抽象，不代表产品公开了内部全部实现；功能、入口和权限行为请以各产品当前文档及实际配置为准。

## 📌 为什么要拆 Codex 和 Claude Code

Codex 和 Claude Code 可以读取项目上下文、编辑文件、调用工具和运行命令，并通过规则文件、MCP 与权限配置支持持续的开发任务。本章重点看这些能力如何组合，以及哪些设计可以借鉴到企业 Agent。

## 📚 目录

### Codex

- [ ] [[Codex/_index|Codex 拆解索引]]
- [ ] [[Codex/01-Codex产品定位与总体架构]]
- [ ] [[Codex/02-Codex一次任务执行全链路]]
- [ ] [[Codex/03-Codex执行模型-线程-沙箱-审批]]
- [ ] [[Codex/04-Codex上下文工程-AGENTS-MCP-Skills]]

### Claude Code

- [ ] [[Claude-Code/_index|Claude Code 拆解索引]]
- [ ] [[Claude-Code/01-Claude-Code产品定位与总体架构]]
- [ ] [[Claude-Code/02-Claude-Code一次任务执行全链路]]
- [ ] [[Claude-Code/03-Claude-Code上下文工程-CLAUDE-md-Memory]]
- [ ] [[Claude-Code/04-Claude-Code工具系统-MCP-Hooks-Subagents-Skills]]

### 对比

- [ ] [[01-标杆产品对比-Codex-vs-Claude-Code]]
- [ ] [[02-编程Agent通用架构图]]
- [ ] [[03-从Codex和Claude-Code反推企业Agent设计]]

## 🧭 学习顺序

1. 先读 [[01-标杆产品对比-Codex-vs-Claude-Code]]，建立整体差异。
2. 再读 [[02-编程Agent通用架构图]]，记住六层架构。
3. 再读 [[Codex/02-Codex一次任务执行全链路]] 和 [[Claude-Code/02-Claude-Code一次任务执行全链路]]，掌握“一句话任务如何跑完”。
4. 再读两个产品各自的模块拆解，重点看上下文、工具、权限、扩展。
5. 最后读 [[03-从Codex和Claude-Code反推企业Agent设计]]，把产品设计迁移到企业 Agent。

## 🧾 本章复习

- [ ] [[99-本章总结与复习]]（本章整体回顾）
- [ ] [[Codex/99-Codex总结与复习]]（Codex 部分回顾）
- [ ] [[Claude-Code/99-Claude-Code总结与复习]]（Claude Code 部分回顾）

## 📌 拆解方法

每个产品都按五层看：

| 层次 | 要看什么 |
| ---- | -------- |
| 用户体验层 | 用户如何发起任务、查看 diff、审批、恢复工作 |
| Agent 执行层 | 任务如何规划、调用工具、验证结果 |
| 上下文层 | 项目规则、文件、历史、技能如何进入上下文 |
| 工具层 | shell、文件系统、MCP、浏览器、IDE 如何接入 |
| 治理层 | 沙箱、权限、审计、团队规则、企业配置 |

## 📚 参考资料

- [OpenAI Codex Documentation](https://learn.chatgpt.com/docs/glossary)
- [Anthropic Claude Code Documentation](https://code.claude.com/docs/overview)
- [[../05-Agent-MCP-Skills/16-AGENTS规范详解]]
- [[../05-Agent-MCP-Skills/09-MCP协议详解]]
