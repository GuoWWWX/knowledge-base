---
title: Next.js-AppRouter基础
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

# Next.js-AppRouter基础

> [!abstract] 一句话总结
> Next.js App Router 把页面、布局和后端接口放在 `app/` 目录下；AI Chat Demo 常用它承载页面和 `/api/chat` 流式接口。

## 🎯 本节定位

- **本章第 8 课**：前置 [[07-Node-pnpm-环境配置]]，本篇讲 Next.js App Router 的页面与接口结构。
- **延伸**：[[09-Vercel-AI-SDK核心]]、[[10-Vercel-AI-SDK-useChat流式UI]]。

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
pnpm typecheck
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

## 🤖 AI 生态落点

- `/api/chat/route.ts`：放模型调用、流式输出和工具调用。
- `page.tsx`：承载聊天页面主体布局。
- 客户端组件：使用 `useChat`、输入框、消息列表。
- 服务端代码：读取环境变量、调用模型、访问数据库或向量库。

在 App Router 里，模型 Key 应该只出现在 server 侧文件，例如 `route.ts`，不要传到浏览器组件。

## ⚠️ 踩坑与注意事项

- 使用 `useState`、事件监听和浏览器 API 的组件需归属在 `"use client"` 边界内（父组件声明后，其导入的子组件会自动纳入客户端模块）。访问 `window` 等浏览器专属 API 时应放在事件回调或 `useEffect` 中，避免服务端预渲染阶段报错。
- API Route 默认运行在服务端，但如果指定 Edge Runtime，要确认 SDK 和依赖兼容。
- `req.json()` 只能读一次，请求体读完后不能重复解析。
- 带有 `NEXT_PUBLIC_` 前缀的环境变量会被打包进浏览器端代码，因此 API Key 等服务端敏感配置不得使用该前缀，也不应通过组件 props 或接口响应下发。同时，Route Handler 与 Server Action 作为独立的服务端入口，需要各自实现完整的权限校验与入参验证。
- 流式接口应直接返回流式响应对象（如 `toUIMessageStreamResponse()`），避免使用 `Response.json` 一次性包装。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: App Router 里 API 接口写在哪里？
>   - A: 写在 `app/api/<name>/route.ts`，导出 `GET`、`POST` 等函数。
> - Q: `"use client"` 是什么？
>   - A: 用于标记服务端到客户端的模块边界，使组件可以使用状态 Hook 与交互事件；客户端组件在首屏仍会参与服务端预渲染，敏感逻辑与鉴权则应保留在服务端接口中。
> - Q: AI Chat 为什么常用 Next.js？
>   - A: 页面、服务端 API、流式响应和部署链路放在一个项目里，原型开发快。

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
