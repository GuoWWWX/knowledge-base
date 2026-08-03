---
title: Mastra简介
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/TypeScript
  - 阶段/07
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# Mastra简介

> [!abstract] 一句话总结
> Mastra 是 TypeScript 生态里的 Agent 和 Workflow 框架，适合把模型、工具、记忆和多步骤流程组织成可运行的 AI 应用。

## 📌 这是什么

读 Mastra 项目时，先找四类对象：

- Agent：面向某个任务的模型助手。
- Tool：Agent 能调用的业务函数。
- Workflow：多步骤流程编排。
- Memory / Storage：会话记忆和持久化。

它的定位更接近“AI 应用后端框架”，不是单纯的模型 SDK。

## 🧠 Java 对照

| Java / Spring 心智 | Mastra 心智 | 记忆点 |
| ------------------ | ----------- | ------ |
| Service | Agent | 承载一个业务能力 |
| Method | Tool | 可被模型调用的函数 |
| Workflow / BPM | Workflow | 多步骤流程 |
| Repository / Cache | Memory / Storage | 状态和记忆 |
| Configuration | model/provider 配置 | 模型接入 |

## ⚙️ 环境命令

常见 TypeScript 项目命令仍然先看 `package.json`：

```bash
pnpm install
pnpm dev
pnpm typecheck
pnpm build
```

新项目或示例项目通常还需要：

```bash
pnpm add mastra
pnpm add zod
```

具体 CLI 以项目 README 为准，因为 Mastra 项目模板和版本可能变化较快。

## 💻 最小示例

下面是阅读心智示例，重点看 Agent 如何绑定模型和工具：

```ts
import { z } from "zod";

const searchDocsTool = {
  id: "searchDocs",
  description: "根据问题检索知识库片段",
  inputSchema: z.object({ query: z.string() }),
  execute: async ({ query }: { query: string }) => {
    return [{ title: "RAG 笔记", content: `匹配问题：${query}` }];
  },
};

const supportAgent = {
  name: "support-agent",
  instructions: "基于知识库回答问题，不知道就说明缺少资料。",
  tools: { searchDocsTool },
};
```

真实项目里会用 Mastra 的 Agent/Tool API 来声明，上面只保留最小结构，方便先看懂代码意图。

## 🤖 AI 生态落点

- 客服 Agent：根据用户问题调用订单、知识库、工单工具。
- 自动化 Workflow：摘要文档、分类、入库、通知。
- 多 Agent 原型：不同 Agent 负责检索、分析、生成。
- TypeScript 全栈：和 Next.js、Node API、前端类型体系更容易放在同一仓库。

## ⚠️ 踩坑与注意事项

- 不要把 Agent 当万能入口；简单 Chat 或单次生成用模型 SDK 更轻。
- Tool 的权限和副作用要可控，写操作要有确认或业务校验。
- Workflow 每一步要记录输入输出，否则失败时难以恢复。
- Memory 会影响回答，调试时要区分“当前 Prompt 问题”还是“历史记忆污染”。
- 框架版本更新快，跟着官方模板和 README 跑，不要混用旧示例。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: Mastra 适合解决什么问题？
>   - A: 适合用 TypeScript 组织 Agent、工具、记忆和多步骤 AI Workflow。
> - Q: 它和直接用 Vercel AI SDK 有什么区别？
>   - A: AI SDK 更偏模型调用和 UI 流；Mastra 更偏 Agent/Workflow 应用结构。
> - Q: 业务 Agent 上线最怕什么？
>   - A: 工具越权、写操作不可追踪、记忆污染、异常步骤不可恢复。

项目里可以用它做：内部业务助手、自动化文档处理流水线、带工具调用的运营 Agent。

## ✅ 精通检查清单

- 能在项目里找到 Agent、Tool、Workflow 的定义。
- 能说明一个 Tool 的入参 schema、执行函数和权限边界。
- 能判断简单 Chat 是否需要引入 Mastra。
- 能设计 Workflow 的日志和失败重试点。

## 🔗 关联笔记

- [[06-TS速查-Java视角]]
- [[11-LangChain.js简介]]
- [[09-Vercel-AI-SDK核心]]

## 📚 参考资料

- [Mastra Docs](https://mastra.ai/docs)
