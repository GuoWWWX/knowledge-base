---
title: Vercel-AI-SDK-useChat流式UI
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

# Vercel-AI-SDK-useChat流式UI

> [!abstract] 一句话总结
> `useChat` 是 Vercel AI SDK 的 React Hook，用来管理聊天消息、输入框、提交状态和流式更新；它适合快速做一个可用的 AI Chat 前端。

## 🎯 本节定位

- **本章第 10 课**：前置 [[09-Vercel-AI-SDK核心]]，本篇把服务端流式接到 React 前端。
- **延伸**：[[11-LangChain.js简介]] 讲更重的编排方案。

## 📌 这是什么

`useChat` 把聊天 UI 里重复的状态处理收起来：

- `messages`：历史消息。
- `input`：输入框内容。
- `handleInputChange`：输入变化。
- `handleSubmit`：提交表单。
- `isLoading`：模型是否还在生成。
- `stop` / `reload`：停止或重试。

后端需要提供兼容的数据流接口，常见路径是 `app/api/chat/route.ts`。

## 🧠 Java 对照

| Java Web 心智 | `useChat` 心智 | 记忆点 |
| ------------- | -------------- | ------ |
| Controller 返回 SSE | API Route 返回 data stream | 流式响应 |
| 前端状态管理 | `messages` / `input` | Hook 管状态 |
| 提交表单 | `handleSubmit` | 自动发请求 |
| loading 标志 | `isLoading` | 控制按钮和占位 |
| 重试接口 | `reload` | 重新生成上一轮 |

## ⚙️ 环境命令

```bash
pnpm add ai @ai-sdk/react @ai-sdk/openai
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

import { useChat } from "@ai-sdk/react";

export function ChatBox() {
  const { messages, input, handleInputChange, handleSubmit, isLoading } = useChat();

  return (
    <form onSubmit={handleSubmit}>
      <div>
        {messages.map((message) => (
          <p key={message.id}>
            <strong>{message.role}:</strong> {message.content}
          </p>
        ))}
      </div>

      <input value={input} onChange={handleInputChange} placeholder="问一个问题" />
      <button type="submit" disabled={isLoading || input.trim() === ""}>
        Send
      </button>
    </form>
  );
}
```

`app/api/chat/route.ts`：

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

- ChatGPT 类页面：`useChat` 管 UI，`streamText` 管模型流。
- RAG 问答：后端先检索，再把上下文加入 system prompt。
- 项目调试台：前端快速测试不同 Prompt、模型和工具调用。
- 内部业务助手：把消息、用户信息、会话 ID 发给 API Route，后端做权限和上下文组装。

## ⚠️ 踩坑与注意事项

- 组件必须写 `"use client"`，否则不能使用 Hook。
- 后端必须返回 AI SDK 兼容的数据流，普通 JSON 不能驱动逐字更新。
- `messages` 会越来越长，真实项目要做会话存储、截断或摘要。
- 不要把模型 API Key 放到客户端组件。
- 输入框提交要处理空字符串和重复提交，避免浪费模型调用。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: `useChat` 帮你做了什么？
>   - A: 它管理消息数组、输入框、提交请求、loading 状态和流式消息更新。
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
