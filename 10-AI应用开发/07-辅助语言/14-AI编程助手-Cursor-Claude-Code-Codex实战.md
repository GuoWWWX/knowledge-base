---
title: AI编程助手-Cursor-Claude-Code-Codex实战
tags: [领域/AI应用, 类型/知识, 主题/AI编程, 主题/Agent]
created: 2026-07-05
updated: 2026-08-03
status: 已完成
mastery: ⭐☆☆☆☆
---

# AI 编程助手：Cursor / Claude Code / Codex 实战

> [!abstract] 一句话总结
> 2026 年 AI 应用开发岗位不仅看你会不会写代码，也看你能不能用 AI 编程助手把需求拆解、实现、测试和文档沉淀做成稳定流程。

## 🎯 本节定位

- **本章第 14 课，收尾实战篇**：不依赖 Python/TS 语法，讲如何用 AI 编程助手完成“理解需求→最小修改→测试验证→总结交付”的闭环。
- **延伸**：09 章 [[../09-标杆产品拆解/_index|标杆产品拆解]] 会拆解 Codex / Claude Code 的产品设计。

## 为什么要学

AI IDE 和编程 Agent 已经从“补全代码”变成“读仓库、改文件、跑测试、解释设计”的协作工具。你需要掌握的是工作流，而不是某个按钮。

## 三类能力

| 能力 | 代表工具 | 重点 |
| ---- | -------- | ---- |
| IDE 内联协作 | Cursor | 读上下文、局部改代码、规则文件 |
| 终端/仓库 Agent | Claude Code / Codex | 多文件修改、跑命令、检查 diff |
| 项目规范注入 | AGENTS.md / Rules / Skills | 让 Agent 遵守项目约定 |

## 实战工作流

1. 先让 Agent 读 `README`、`AGENTS.md`、关键目录。
2. 要求它复述理解和最小改动方案。
3. 让它实现一个小闭环，不要一次改太大。
4. 让它跑测试、构建、lint。
5. 要求输出改了什么、如何验证、残余风险。
6. 把反复出现的规则沉淀到 [[../05-Agent-MCP-Skills/16-AGENTS规范详解]] 或技能文档。

## 好提示词模板

```text
请先阅读本模块现有实现，按现有风格做最小改动。
不要重构无关代码。
完成后运行相关测试，并汇报：
1. 改了哪些文件
2. 如何验证
3. 还有什么风险
```

## 常见坑

- 直接让 Agent “优化整个项目”，容易引入无关改动。
- 不给测试命令，Agent 可能只靠静态判断。
- 不区分事实和猜测，容易接受错误解释。
- 不维护项目规则，导致每次都要重新教。

## 面试讲法

> 我使用 AI 编程助手时会把它当成初级协作者，而不是自动驾驶。先给上下文和边界，再小步实现，最后要求它跑测试并汇报风险。项目级约定放在 AGENTS.md 或 rules 里，减少重复沟通。

## 与 AI 应用开发的关系

懂编程 Agent 能反过来帮助理解 Agent 产品：

- 工具调用：读文件、改文件、跑命令。
- 上下文工程：选择哪些文件进入上下文。
- 评估：测试是否通过、diff 是否合理。
- 安全：权限、危险命令、用户确认。

## 📚 参考资料

- [Cursor Docs](https://docs.cursor.com/)
- [Claude Code Documentation](https://code.claude.com/docs/overview)
- [OpenAI Codex](https://developers.openai.com/codex/)
- [[../05-Agent-MCP-Skills/16-AGENTS规范详解]]

