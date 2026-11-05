---
title: Vercel-AI-SDK-useChat流式UI
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/TypeScript
  - 阶段/08
created: 2026-07-04
updated: 2026-10-30
status: 已完成
mastery: ⭐☆☆☆☆
---

# Vercel-AI-SDK-useChat流式UI

> [!abstract] 一句话总结
> `useChat` 是 Vercel AI SDK 的 React Hook，用来管理聊天消息、请求状态和流式更新；此处的输入框内容由组件自行管理。

## 🎯 本节定位

本篇在 [[09-Vercel-AI-SDK核心]] 之后，介绍如何将服务端流式响应接入 React 前端；更复杂的编排可继续看 [[11-LangChain.js简介]]。

## 📌 这是什么

`useChat` 帮助管理聊天消息和请求状态。以下示例按 AI SDK 5 的接口组织，项目使用其他版本时应查阅对应文档：
- `messages`：UI 消息，文本在 `parts` 中。
- `sendMessage`：发送消息；输入框由 React `useState` 管理。
- `status` / `error`：提交、生成和错误状态。
- `stop` / `regenerate`：停止或重新生成。

后端需要提供兼容的数据流接口，常见路径是 `app/api/chat/route.ts`。

## 🧠 Java 对照

| Java Web 心智 | `useChat` 心智 | 记忆点 |
| ------------- | -------------- | ------ |
| Controller 返回 SSE | API Route 返回 data stream | 流式响应 |
| 前端状态管理 | `messages` + 本地 `input` | 消息和输入分开管理 |
| 提交表单 | `sendMessage` | 自动发请求 |
| loading 标志 | `status` | 控制按钮和占位 |
| 重试接口 | `regenerate` | 重新生成上一轮 |

## ⚙️ 环境命令

```bash
pnpm add ai@5 @ai-sdk/react@2 @ai-sdk/openai@2
pnpm dev
```

必要文件：

```text
app/
  page.tsx
  api/chat/route.ts
components/
  chat-box.tsx
```

## 💻 最小示例

下面展示一个自定义 Hook；假设由客户端组件调用，组件负责输入框、按钮与消息渲染。这里只演示消息和提交状态，代码未连接模型运行。

```tsx
"use client";
import { useChat } from "@ai-sdk/react";

function useChatSender() {
  const { messages, sendMessage, status } = useChat();
  const submit = async (text: string) => sendMessage({ text });
  return { messages, status, submit };
}
```

例如组件调用 `submit("解释 RAG")` 后，服务端流式返回消息，`messages` 随响应更新；`status` 可用于显示等待或生成状态。此片段本身不包含输入校验、停止按钮和界面。
服务端接口 `app/api/chat/route.ts` 的示例：

```ts
import { convertToModelMessages, streamText } from "ai";
import { openai } from "@ai-sdk/openai";

export async function POST(req: Request) {
  const { messages } = await req.json();

  const result = streamText({
    model: openai("gpt-4.1-mini"),
    messages: await convertToModelMessages(messages),
    abortSignal: req.signal,
  });

  return result.toUIMessageStreamResponse();
}
```

## 🤖 AI 生态落点

- ChatGPT 类页面：`useChat` 管 UI，`streamText` 管模型流。
- RAG 问答：后端先检索相关文档片段，作为参考上下文组装到对话请求中。
- 项目调试台：前端快速测试不同 Prompt、模型和工具调用。
- 内部业务助手：把消息、用户信息、会话 ID 发给 API Route，后端做权限和上下文组装。

## ⚠️ 踩坑与注意事项

- 使用 Hook 的组件应位于 `"use client"` 模块边界内，子组件不必重复声明。
- 后端必须返回 AI SDK 兼容的数据流，普通 JSON 不能驱动逐字更新。
- `messages` 会越来越长，真实项目要做会话存储、截断或摘要。
- 不要把模型 API Key 放到客户端组件。
- 提交前检查空输入，并避免重复提交。
- `stop` 可取消前端请求；后端可将 `req.signal` 传给 `streamText`，但远端是否停止仍取决于服务商。
- 示例只演示发送；实际组件需要遍历 `message.parts` 渲染文本和工具结果。
- 身份和会话归属需要在服务端接口中校验。

## 🎯 面试 / 项目用途

> [!question]- `useChat` 帮你做了什么？
> 它管理消息、发送、请求状态和流式更新；输入框内容由组件自己管理。

> [!question]- 为什么流式 UI 体验更好？
> 用户能更早看到首段输出，不必等模型完整生成后才显示。

> [!question]- 生产 Chat UI 还要补什么？
> 会话存储、错误提示、停止生成、重试、鉴权、消息长度控制和埋点。

项目里可以用它做：AI 助手浮窗、知识库问答页面、模型效果调试页。

## ✅ 精通检查清单

- 能写一个使用 `useChat` 的最小组件。
- 能解释前端 Hook 和后端 `streamText` 的配合关系。
- 能处理 loading、空输入、停止生成和错误提示。
- 能说清消息历史为什么不能无限传给模型。

## 🔗 关联笔记

- [[06-TS速查-Java视角]]
- [[08-Next.js-AppRouter基础]]
- [[09-Vercel-AI-SDK核心]]

## 📚 参考资料

- [AI SDK UI Docs](https://ai-sdk.dev/docs/ai-sdk-ui)
- [AI SDK：停止流式响应](https://ai-sdk.dev/docs/advanced/stopping-streams)
