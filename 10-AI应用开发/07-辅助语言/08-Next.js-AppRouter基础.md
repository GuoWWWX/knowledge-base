---
title: Next.js-AppRouter基础
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

# Next.js-AppRouter基础

> [!abstract] 一句话总结
> Next.js App Router 把页面、布局和后端接口放在 `app/` 目录下；AI Chat Demo 常用它承载页面和 `/api/chat` 流式接口。

## 📌 这是什么

App Router 的阅读入口很固定：

- `app/page.tsx`：当前路由页面。
- `app/layout.tsx`：全局布局。
- `app/api/*/route.ts`：服务端 API。
- `"use client"`：声明组件需要在浏览器端运行。
- `components/`：可复用 UI 组件。

## 🧠 Java 对照

| Spring Boot / MVC | Next.js App Router | 记忆点 |
| ----------------- | ------------------ | ------ |
| Controller route | `app/api/x/route.ts` | 后端接口 |
| Thymeleaf / 前端页面 | `app/page.tsx` | 页面组件 |
| Filter / layout | `app/layout.tsx` | 包住子页面 |
| `@PostMapping` | `export async function POST` | HTTP 方法函数 |
| DTO 解析 | `await req.json()` | 手动读请求体 |

## ⚙️ 环境命令

```bash
pnpm create next-app ai-chat-demo --ts
cd ai-chat-demo
pnpm add ai @ai-sdk/openai
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
- `page.tsx`：放聊天页面壳子。
- 客户端组件：使用 `useChat`、输入框、消息列表。
- 服务端代码：读取环境变量、调用模型、访问数据库或向量库。

在 App Router 里，模型 Key 应该只出现在 server 侧文件，例如 `route.ts`，不要传到浏览器组件。

## ⚠️ 踩坑与注意事项

- 使用 React Hook 的组件必须加 `"use client"`。
- API Route 默认运行在服务端，但如果指定 Edge Runtime，要确认 SDK 和依赖兼容。
- `req.json()` 只能读一次，请求体读完后不能重复解析。
- 环境变量暴露到前端必须以 `NEXT_PUBLIC_` 开头；模型 Key 不要这样命名。
- 流式接口不能随便包一层 `Response.json`，要返回 stream response。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: App Router 里 API 接口写在哪里？
>   - A: 写在 `app/api/<name>/route.ts`，导出 `GET`、`POST` 等函数。
> - Q: `"use client"` 是什么？
>   - A: 它声明该文件是客户端组件，可以使用浏览器 API 和 React Hook。
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
