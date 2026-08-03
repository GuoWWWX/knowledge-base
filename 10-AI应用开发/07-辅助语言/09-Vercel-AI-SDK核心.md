---
title: Vercel-AI-SDK核心
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/TypeScript
  - 阶段/08
created: 2026-07-04
updated: 2026-08-03
status: 已完成
mastery: ⭐☆☆☆☆
---

# Vercel-AI-SDK核心

> [!abstract] 一句话总结
> Vercel AI SDK 是 TypeScript 生态里做模型调用和流式输出的常用库，核心是 `generateText`、`streamText`、provider 和工具调用。

## 🎯 本节定位

- **本章第 9 课**：前置 [[08-Next.js-AppRouter基础]]，本篇讲 Vercel AI SDK 的核心 API（generateText / streamText / tool）。
- **延伸**：[[10-Vercel-AI-SDK-useChat流式UI]] 做前端 UI。

## 📌 这是什么

它把不同模型厂商的调用方式统一成一套 TypeScript API：

- provider：例如 `@ai-sdk/openai`、`@ai-sdk/anthropic`。
- `generateText`：一次性生成文本。
- `streamText`：边生成边返回，适合聊天 UI。
- `tool`：声明模型可调用的函数。
- `zod`：给工具参数做 schema。

## 🧠 Java 对照

| Spring AI / LangChain4j | Vercel AI SDK | 记忆点 |
| ----------------------- | ------------- | ------ |
| `ChatClient.call` | `generateText` | 一次性模型调用 |
| Streaming response | `streamText` | 流式输出 |
| Tool / Function calling | `tool(...)` | 工具声明 |
| DTO validation | `z.object(...)` | 参数 schema |
| Controller 返回流 | `toDataStreamResponse()` | 给前端消费的流 |

## ⚙️ 环境命令

```bash
pnpm add ai @ai-sdk/openai zod
pnpm dev
pnpm typecheck
```

`.env.local`：

```text
OPENAI_API_KEY=sk-...
```

## 💻 最小示例

一次性生成：

```ts
import { generateText } from "ai";
import { openai } from "@ai-sdk/openai";

const result = await generateText({
  model: openai("gpt-4.1-mini"),
  prompt: "用 Java 开发者能懂的话解释 RAG",
});

console.log(result.text);
```

Next.js 流式接口：

```ts
import { streamText } from "ai";
import { openai } from "@ai-sdk/openai";

export async function POST(req: Request) {
  const { messages } = await req.json();

  const result = streamText({
    model: openai("gpt-4.1-mini"),
    messages,
  });

  return result.toDataStreamResponse();
}
```

## 🤖 AI 生态落点

- Chat UI：后端 `streamText`，前端 `useChat`。
- Tool calling：把数据库查询、订单查询、搜索接口封成 tool。
- RAG：先检索文档，再把 context 放进 `system` 或 `prompt`。
- 模型网关：同一层里切换 OpenAI、Anthropic、本地兼容接口。

工具调用示意：

```ts
import { tool } from "ai";
import { z } from "zod";

const getWeather = tool({
  description: "查询城市天气",
  parameters: z.object({ city: z.string() }),
  execute: async ({ city }) => ({ city, weather: "sunny" }),
});
```

## ⚠️ 踩坑与注意事项

- `streamText` 返回的是流，不要用 `Response.json` 包住。
- provider 包和 `ai` 主包版本要兼容。
- 工具参数必须写 schema，否则模型容易传错字段。
- 不要在客户端组件里直接调用带 Key 的 provider。
- RAG 上下文要限制长度，否则流式再顺也会慢且贵。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: `generateText` 和 `streamText` 怎么选？
>   - A: 短任务、后台任务用 `generateText`；聊天和长回答用 `streamText` 改善首 token 体验。
> - Q: Vercel AI SDK 和直接调 OpenAI SDK 的区别？
>   - A: AI SDK 统一 provider、流式协议、前端 Hook 和工具调用，适合全栈 Chat 应用。
> - Q: 工具调用最重要的工程点是什么？
>   - A: 工具描述、参数 schema、权限校验、超时和执行日志。

项目里可以用它做：流式客服、AI 搜索框、带工具调用的业务助手、前端可调试模型 Playground。

## ✅ 精通检查清单

- 能写出 `generateText` 和 `streamText` 的最小代码。
- 能解释 provider、model、messages、system 的关系。
- 能把流式结果返回给 Next.js 前端。
- 能给一个 tool 写出 `description`、`parameters`、`execute`。

## 🔗 关联笔记

- [[06-TS速查-Java视角]]
- [[08-Next.js-AppRouter基础]]
- [[10-Vercel-AI-SDK-useChat流式UI]]

## 📚 参考资料

- [Vercel AI SDK Docs](https://ai-sdk.dev/docs)
