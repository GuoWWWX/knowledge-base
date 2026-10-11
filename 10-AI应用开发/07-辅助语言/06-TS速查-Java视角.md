---
title: TS速查-Java视角
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

# TS速查-Java视角

> [!abstract] 一句话总结
> 目标是看懂 Next.js、Vercel AI SDK、LangChain.js、Mastra 这类 AI 全栈项目，并能跑通一个流式 Chat Demo。

## 🎯 本节定位

- **本章第 6 课，TypeScript 子区起点**：与第 1 课对称，建立 Java → TypeScript 的语法与心智对照。
- **延伸**：[[07-Node-pnpm-环境配置]]（环境）、[[08-Next.js-AppRouter基础]]（全栈结构）。

## 🎯 学习目标

- [ ] 看懂 TypeScript 类型、函数、对象、模块导入
- [ ] 会用 `pnpm` 安装依赖、启动示例项目
- [ ] 理解 Next.js App Router 的页面和接口结构
- [ ] 看懂 Vercel AI SDK 的服务端流式输出和前端 `useChat`

## 🧠 Java 到 TypeScript 对照

| Java | TypeScript | 记忆点 |
| ---- | ---------- | ------ |
| `String name` | `const name: string` | 类型写在变量后面 |
| `Integer / Double` | `number` | 双精度浮点；大整数可用 `bigint`，金额另选精确表示 |
| `boolean` | `boolean` | 基本一致 |
| `List<String>` | `string[]` | 数组写法更短 |
| `Map<String, Object>` | `Record<string, unknown>` | 对象字典 |
| `Object` | `unknown` | 比 `any` 更安全 |
| `Optional<T>` | `T \| undefined` | 联合类型表达可选 |
| `CompletableFuture<T>` | `Promise<T>` | 异步结果 |
| `interface` | `interface` / `type` | TS 是结构化类型 |

## 📦 环境与包管理

```bash
node -v
corepack enable
pnpm -v
pnpm install
pnpm dev
```

| 文件 | 作用 |
| ---- | ---- |
| `package.json` | 依赖和脚本，类似 `pom.xml` |
| `pnpm-lock.yaml` | 锁定依赖版本 |
| `tsconfig.json` | TypeScript 编译配置 |
| `.env.local` | 本地模型 Key |
| `app/` | Next.js App Router 主目录 |

## ✍️ 类型速查

```ts
const model: string = "deepseek-chat";
const topK: number = 5;
const stream: boolean = true;

type ChatMessage = {
  role: "system" | "user" | "assistant";
  content: string;
};

const messages: ChatMessage[] = [
  { role: "user", content: "解释 RAG" },
];
```

```ts
interface ChatRequest {
  message: string;
  userId?: string;
}

type ChatResponse = {
  answer: string;
  sources: string[];
};
```

**TypeScript 类型在编译后会被擦除**：`as ChatResponse` 仅提供编译期类型断言，处理接口入参与模型输出等外部 JSON 数据时，需配合 Zod 等库进行运行时校验。

## 🧩 函数、异步、解构

```ts
async function callLLM(prompt: string): Promise<string> {
  const response = await fetch("/api/chat", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ message: prompt }),
  });

  if (!response.ok) throw new Error(`HTTP ${response.status}`);
  const data: unknown = await response.json();
  if (typeof data !== "object" || data === null ||
      !("answer" in data) || typeof data.answer !== "string") {
    throw new Error("响应格式错误");
  }
  return data.answer;
}
```

```ts
const req = { message: "hi", userId: "u1" };
const { message, userId } = req;

const nextReq = {
  ...req,
  stream: true,
};
```

> [!tip] TS / React 项目里到处都是解构和 `...` 展开。先看懂这两个，阅读速度会明显提升。

## 🚀 Next.js App Router 最小结构

```text
app/
  page.tsx
  api/
    chat/
      route.ts
components/
  chat-box.tsx
```

```ts
export async function POST(req: Request) {
  const body = await req.json();

  return Response.json({
    answer: `收到：${body.message}`,
  });
}
```

| Next.js | Spring Boot |
| ------- | ----------- |
| `app/api/chat/route.ts` | `@RestController` |
| `export async function POST` | `@PostMapping` |
| `Request` | HTTP 请求对象 |
| `Response.json` | `ResponseEntity.ok` |

## 🤖 Vercel AI SDK 最小心智

本节与后续两篇统一基于 **AI SDK 5**（`ai@5`、`@ai-sdk/react@2`、`@ai-sdk/openai@2`）编写，采用 `sendMessage`、`message.parts` 与 `toUIMessageStreamResponse` 等新版 API（替代 v4 的 `handleSubmit`、`message.content` 与 `toDataStreamResponse`）。

数据流是：组件用 `sendMessage` 提交 UI 消息 → 服务端校验并用 `convertToModelMessages` 转换 → `streamText` 调模型 → `toUIMessageStreamResponse` 返回 → 前端渲染 `message.parts`。

完整可运行的配套示例见 [[09-Vercel-AI-SDK核心]] 和 [[10-Vercel-AI-SDK-useChat流式UI]]。

| 关键词 | 作用 |
| ------ | ---- |
| `streamText` | 服务端调用 LLM 并流式返回 |
| `toUIMessageStreamResponse` | 转成前端可消费的数据流 |
| `useChat` | 前端聊天状态管理 |
| `"use client"` | 声明 React 客户端组件 |

## 🧪 常用检查命令

```bash
pnpm install
pnpm dev
pnpm lint
pnpm typecheck
pnpm build
```

> [!warning] 读 TS AI 项目时，优先找 `app/api/*/route.ts`、`package.json`、`.env.example`。模型调用、流式输出、工具调用通常都在 API Route 或 server action 里。

## 🔗 关联笔记

- [[07-Node-pnpm-环境配置]]
- [[08-Next.js-AppRouter基础]]
- [[09-Vercel-AI-SDK核心]]
- [[10-Vercel-AI-SDK-useChat流式UI]]
- [[11-LangChain.js简介]]
- [[12-Mastra简介]]
- [[13-TS与Java-AI生态对照]]

## 📚 参考资料

- [TypeScript Handbook](https://www.typescriptlang.org/docs/handbook/intro.html)
- [Next.js App Router Documentation](https://nextjs.org/docs/app)
- [AI SDK Documentation](https://ai-sdk.dev/docs)
