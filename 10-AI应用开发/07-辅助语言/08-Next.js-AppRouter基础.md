---
title: Next.js-AppRouter基础
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

# Next.js-AppRouter基础

> [!abstract] 一句话总结
> Next.js App Router 把页面、布局和后端接口放在 `app/` 目录下；AI Chat Demo 常用它承载页面和 `/api/chat` 流式接口。

## 🎯 本节定位

本篇在 [[07-Node-pnpm-环境配置]] 之后，介绍 Next.js App Router 的页面与接口结构；后续可学习 [[09-Vercel-AI-SDK核心]] 和 [[10-Vercel-AI-SDK-useChat流式UI]]。

## 📌 这是什么

App Router 的阅读入口很固定：
- `app/page.tsx`：当前路由页面。
- `app/layout.tsx`：全局布局。
- `app/api/*/route.ts`：服务端 API。
- `"use client"`：声明客户端组件模块边界，使组件支持浏览器端状态与交互（首次加载时仍会在服务端进行预渲染）。
- `components/`：可复用 UI 组件。

## 🧠 Java 对照

| Spring Boot / MVC | Next.js App Router | 记忆点 |
| ----------------- | ------------------ | ------ |
| Controller route | `app/api/x/route.ts` | 后端接口 |
| Thymeleaf / 前端页面 | `app/page.tsx` | 页面组件 |
| 页面布局 | `app/layout.tsx` | 共享外层 UI 结构 |
| `@PostMapping` | `export async function POST` | HTTP 方法函数 |
| JSON 解析 | `await req.json()` | 解析请求体 JSON（需配合运行时校验） |

## ⚙️ 环境命令

```bash
pnpm create next-app ai-chat-demo --ts
cd ai-chat-demo
pnpm add ai@5 @ai-sdk/openai@2 @ai-sdk/react@2
pnpm dev
```

常用验证：

```bash
pnpm lint
pnpm typecheck # 仅当 package.json 定义了该脚本
pnpm build
```

## 💻 最小示例

目录：

```text
app/
  page.tsx
  api/
    chat/
      route.ts
```

`app/api/chat/route.ts`：

```ts
export async function POST(req: Request) {
  const body = await req.json();

  return Response.json({
    answer: `收到：${body.message}`,
  });
}
```

`app/page.tsx`：

```tsx
export default function Page() {
  return <main>AI Chat Demo</main>;
}
```

## 🔍 页面和接口怎么配合？

浏览器访问 `/` 时对应 `app/page.tsx`；发送 `POST /api/chat` 时对应 `app/api/chat/route.ts` 的 `POST` 函数。上面的两个片段只分别展示页面和接口，页面并没有自动调用接口；要完成对话，还需要客户端组件发请求并显示响应。
示例直接读取 `body.message` 是为了展示路由，实际接口应先验证请求体；如果字段缺失、类型错误或用户无权限，应返回明确错误，而不是把这些值直接送给模型。

## 🤖 AI 生态落点

- `/api/chat/route.ts`：放模型调用、流式输出和工具调用。
- `page.tsx`：承载聊天页面主体布局。
- 客户端组件：使用 `useChat`、输入框、消息列表。
- 服务端代码：读取环境变量、调用模型、访问数据库或向量库。

在 App Router 里，模型 Key 应该只出现在 server 侧文件，例如 `route.ts`，不要传到浏览器组件。

## ⚠️ 踩坑与注意事项

- 使用 `useState`、事件监听和浏览器 API 的组件需处于 `"use client"` 边界内；该模块导入的子组件也会进入客户端模块。
- `window` 等浏览器 API 应放在事件回调或 `useEffect` 中访问，避免服务端预渲染时报错。
- API Route 默认运行在服务端，但如果指定 Edge Runtime，要确认 SDK 和依赖兼容。
- `req.json()` 只能读一次，请求体读完后不能重复解析。
- `NEXT_PUBLIC_` 环境变量会被打包进浏览器代码，API Key 等敏感配置不能使用该前缀。
- 服务端密钥也不能通过客户端组件的 props 或接口响应下发。
- Route Handler 和 Server Action 都是服务端入口，需要各自校验权限和输入。
- 流式接口应直接返回流式响应对象（如 `toUIMessageStreamResponse()`），避免使用 `Response.json` 一次性包装。

## 🎯 面试 / 项目用途

> [!question]- App Router 里 API 接口写在哪里？
> 写在 `app/api/<name>/route.ts`，导出 `GET`、`POST` 等函数。

> [!question]- `"use client"` 是什么？
> 用于标记服务端到客户端的模块边界，使组件可以使用状态 Hook 与交互事件；客户端组件在首屏仍会参与服务端预渲染，敏感逻辑与鉴权则应保留在服务端接口中。

> [!question]- AI Chat 为什么常用 Next.js？
> 页面、服务端 API、流式响应和部署链路放在一个项目里，原型开发快。

项目里可以用它做：AI Chat UI、知识库问答前端、后台管理台里的模型调试页面。

## ✅ 精通检查清单

- 能根据目录判断页面和接口路径。
- 能写一个 `POST` API Route 并用页面调用。
- 能解释 server component 和 client component 的基本边界。
- 能说明模型 Key 为什么只能留在服务端。

## 🔗 关联笔记

- [[06-TS速查-Java视角]]
- [[07-Node-pnpm-环境配置]]
- [[10-Vercel-AI-SDK-useChat流式UI]]

## 📚 参考资料

- [Next.js App Router Docs](https://nextjs.org/docs/app)
- [Next.js Route Handlers](https://nextjs.org/docs/app/building-your-application/routing/route-handlers)
