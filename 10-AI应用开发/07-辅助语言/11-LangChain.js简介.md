---
title: LangChain.js简介
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

# LangChain.js简介

> [!abstract] 一句话总结
> LangChain.js 是 TypeScript 生态里的 LLM 编排库，用来在 Node/Next.js 项目中组织 Prompt、模型、Retriever、Tool 和 Agent。

## 📌 这是什么

如果 Python 侧项目用 LangChain，TypeScript 侧通常会遇到 LangChain.js。它适合：

- 在 Next.js API Route 里做 RAG。
- 在 Node 服务里封装工具调用。
- 和前端 TypeScript 类型共享部分请求/响应结构。
- 学习 LangChain 的 Runnable 管道心智。

## 🧠 Java 对照

| Java AI 生态 | LangChain.js | 记忆点 |
| ------------ | ------------ | ------ |
| Spring AI `ChatClient` | `ChatOpenAI` | 模型调用 |
| LangChain4j chain | Runnable chain | 编排流程 |
| Retriever | retriever | 检索上下文 |
| Tool specification | tool | 函数调用 |
| record / DTO | `zod` schema | 参数校验 |

## ⚙️ 环境命令

```bash
pnpm add langchain @langchain/openai
pnpm add zod
pnpm dev
```

`.env.local`：

```text
OPENAI_API_KEY=sk-...
```

## 💻 最小示例

```ts
import { ChatOpenAI } from "@langchain/openai";
import { ChatPromptTemplate } from "@langchain/core/prompts";

const prompt = ChatPromptTemplate.fromMessages([
  ["system", "你是一个面向 Java 开发者的 AI 学习助手。"],
  ["user", "解释 {topic}"],
]);

const model = new ChatOpenAI({
  model: "gpt-4.1-mini",
  temperature: 0,
});

const chain = prompt.pipe(model);
const result = await chain.invoke({ topic: "RAG" });

console.log(result.content);
```

这段代码的数据流是：输入对象 -> Prompt 模板 -> 模型 -> 消息结果。

## 🤖 AI 生态落点

- RAG：用 retriever 查向量库，把文档片段拼进 Prompt。
- Agent：把业务函数封成 tool，让模型决定是否调用。
- Next.js：在 `app/api/chat/route.ts` 里跑 chain，再把结果返回前端。
- Node 后端：作为独立 AI 服务，给 Java 主系统提供 HTTP 接口。

## ⚠️ 踩坑与注意事项

- LangChain.js 和 Python LangChain API 名字相似但不完全一样，复制示例时要确认语言版本。
- Next.js Edge Runtime 对部分 Node 依赖有限制，复杂 LangChain 逻辑优先放 Node Runtime。
- Chain 抽象层多，调试时要拆开看 Prompt、retriever、model 的输入输出。
- 工具调用要写清参数 schema，并在服务端做权限校验。
- RAG 结果要限制条数和长度，避免一次请求塞太多上下文。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: LangChain.js 适合什么项目？
>   - A: 适合 TypeScript 栈里的 RAG、工具调用和多步骤模型编排。
> - Q: 它和 Vercel AI SDK 怎么选？
>   - A: 只做流式 Chat UI，AI SDK 更直接；复杂检索和编排，LangChain.js 更顺手。
> - Q: 线上怎么排查回答质量差？
>   - A: 分别记录用户输入、检索结果、最终 Prompt、模型参数和输出。

项目里可以用它做：Next.js 知识库问答、Node Agent 服务、面向前端的 AI API。

## ✅ 精通检查清单

- 能写出 `prompt.pipe(model)` 的最小示例。
- 能说明 LangChain.js 和 Python LangChain 的关系。
- 能把 chain 放进 Next.js API Route。
- 能解释 RAG 调试要看哪些中间数据。

## 🔗 关联笔记

- [[06-TS速查-Java视角]]
- [[09-Vercel-AI-SDK核心]]
- [[13-TS与Java-AI生态对照]]

## 📚 参考资料

- [LangChain.js Docs](https://js.langchain.com/)
