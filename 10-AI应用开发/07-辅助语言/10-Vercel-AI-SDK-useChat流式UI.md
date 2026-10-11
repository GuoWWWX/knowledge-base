---
title: Vercel-AI-SDK-useChat流式UI
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/TypeScript
  - 阶段/08
created: 2026-07-04
updated: 2026-09-28
status: 已完成
mastery: ⭐☆☆☆☆
---

# Vercel-AI-SDK-useChat流式UI

> [!abstract] 一句话总结
> `useChat` 是 Vercel AI SDK 的 React Hook，用来管理聊天消息、输入框、提交状态和流式更新；它适合快速做一个可用的 AI Chat 前端。

## 🎯 本节定位

- **本章第 10 课**：前置 [[09-Vercel-AI-SDK核心]]，本篇把服务端流式接到 React 前端。
- **延伸**：[[11-LangChain.js简介]] 讲更重的编排方案。

## 📌 这是什么

`useChat` 把聊天 UI 里重复的状态处理收起来：

以下基于 **AI SDK 5** 的核心接口说明：
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

`components/chat-box.tsx`：

```tsx
"use client";

import { useState } from "react";
import { useChat } from "@ai-sdk/react";

export function ChatBox() {
  const [input, setInput] = useState("");
  const { messages, sendMessage, status, error, stop } = useChat();
  const busy = status === "submitted" || status === "streaming";

  return (
    <form onSubmit={(event) => {
      event.preventDefault();
      if (busy || !input.trim()) return;
      void sendMessage({ text: input });
      setInput("");
    }}>
      {messages.map((message) => (
        <div key={message.id}>
          {message.parts.map((part, i) =>
            part.type === "text" ? <span key={i}>{part.text}</span> : null
          )}
        </div>
      ))}
      <input value={input} onChange={(e) => setInput(e.target.value)} />
      <button type="submit" disabled={busy || !input.trim()}>发送</button>
      <button type="button" disabled={!busy} onClick={() => stop()}>停止</button>
      {error && <p role="alert">请求失败，请稍后重试。</p>}
    </form>
  );
}
```

`app/api/chat/route.ts`：

```ts
import { convertToModelMessages, streamText } from "ai";
import { openai } from "@ai-sdk/openai";

export async function POST(req: Request) {
  const { messages } = await req.json();

  const result = streamText({
    model: openai("gpt-4.1-mini"),
    messages: await convertToModelMessages(messages),
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

- 组件必须写 `"use client"`，否则不能使用 Hook。
- 后端必须返回 AI SDK 兼容的数据流，普通 JSON 不能驱动逐字更新。
- `messages` 会越来越长，真实项目要做会话存储、截断或摘要。
- 不要把模型 API Key 放到客户端组件。
- 提交表单时需做好空字符串拦截与防重提交处理；点击“停止”会中止本地流读取与网络请求，若需同步终止服务端推理，可在后端监听 `req.signal` 中断信号。
- 上述示例仅渲染了 `text` 类型的消息片段；若涉及工具调用、文件附件或知识库引用，可遍历 `message.parts` 按对应类型分别渲染。同时，身份鉴权与会话归属校验应在服务端接口中完成。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: `useChat` 帮你做了什么？
>   - A: 它管理消息、发送、请求状态和流式更新；输入框内容由组件自己管理。
> - Q: 为什么流式 UI 体验更好？
>   - A: 用户能更早看到首段输出，不必等模型完整生成后才显示。
> - Q: 生产 Chat UI 还要补什么？
>   - A: 会话存储、错误提示、停止生成、重试、鉴权、消息长度控制和埋点。

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
