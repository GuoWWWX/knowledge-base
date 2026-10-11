---
title: Node-pnpm-环境配置
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

# Node-pnpm-环境配置

> [!abstract] 一句话总结
> Node 是 TypeScript AI 全栈项目的运行时，`pnpm` 是常用包管理器；先跑通它们，才能读 Next.js、Vercel AI SDK、LangChain.js 和 Mastra 项目。

## 🎯 本节定位

- **本章第 7 课**：前置 [[06-TS速查-Java视角]]，本篇解决 Node / pnpm 环境与项目命令。
- **延伸**：[[08-Next.js-AppRouter基础]]。

## 📌 这是什么

TypeScript AI 项目通常不是直接运行 `.ts` 文件，而是由 Node、包管理器、框架和构建工具一起工作。

接手项目优先看：

- `package.json`：脚本和依赖。
- `pnpm-lock.yaml`：锁定依赖版本。
- `tsconfig.json`：TypeScript 编译规则。
- `.env.local`：模型 Key 和后端地址。
- `app/` 或 `src/`：业务代码入口。

## 🧠 Java 对照

| Java / Maven | Node / pnpm | 记忆点 |
| ------------ | ----------- | ------ |
| JDK | Node.js | 运行时 |
| Maven / Gradle | pnpm | 安装依赖和跑脚本 |
| `pom.xml` | `package.json` | 依赖、脚本、项目元信息 |
| `mvn test` | `pnpm test` | 执行测试脚本 |
| `target/` | `.next/` / `dist/` | 构建产物 |
| `application.yml` | `.env.local` | 本地配置 |

## ⚙️ 环境命令

```bash
node -v
# Corepack 用于管理包管理器版本（Node 25+ 需单独安装 corepack 或直接按官方文档安装 pnpm）
corepack enable
pnpm -v
pnpm install
pnpm dev
```

新建 Next.js 项目：

```bash
pnpm create next-app ai-chat-demo --ts
cd ai-chat-demo
pnpm add ai@5 @ai-sdk/openai@2 @ai-sdk/react@2
pnpm dev
```

常用检查：

```bash
pnpm lint
pnpm typecheck
pnpm build
pnpm why ai
```

## 💻 最小示例

`package.json` 里先找脚本：

```json
{
  "scripts": {
    "dev": "next dev",
    "build": "next build",
    "lint": "eslint .",
    "typecheck": "tsc --noEmit"
  }
}
```

上述命令对应 `package.json` 的 `scripts` 配置（Next.js 16 起推荐直接使用 `eslint .` 替代已移除的 `next lint`）。在 CI 环境中，建议使用 `pnpm install --frozen-lockfile` 并配合 `packageManager` 字段锁定包管理器与依赖版本。

运行逻辑是：`pnpm dev` -> 找 `scripts.dev` -> 启动框架开发服务。

## 🤖 AI 生态落点

- Next.js + Vercel AI SDK：前端页面、API Route、流式 Chat 常放在同一仓库。
- LangChain.js：Node 服务里编排 RAG、工具调用和 Agent。
- Mastra：用 TypeScript 组织 Agent、Workflow、Memory。
- SDK Key：一般放 `.env.local`，变量名常见为 `OPENAI_API_KEY`、`ANTHROPIC_API_KEY`。

## ⚠️ 踩坑与注意事项

- 不要混用 `npm install`、`yarn install`、`pnpm install`，否则 lockfile 和 `node_modules` 容易乱。
- Windows 上脚本失败时，先确认 Node 版本和项目 README 要求一致。
- `Cannot find module` 先跑 `pnpm install`，再查依赖是否在 `dependencies` 或 `devDependencies`。
- `.env.local` 修改后通常要重启 `pnpm dev`。
- AI SDK 相关包版本要配套，升级 `ai` 时也要看 provider 包版本。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: `pnpm` 相比 `npm` 的特点是什么？
>   - A: 依赖通过内容寻址存储和链接复用，安装快，磁盘占用更少，monorepo 支持也常见。
> - Q: 接手 TS AI 项目第一步做什么？
>   - A: 看 `package.json` 脚本、`.env.example`、框架目录，再 `pnpm install && pnpm dev`。
> - Q: 为什么要提交 `pnpm-lock.yaml`？
>   - A: 锁定依赖树，避免不同机器安装出不同版本导致构建或运行差异。

项目里可以用它快速复现：Vercel AI SDK Chat Demo、Next.js 管理台、LangChain.js RAG 服务。

## ✅ 精通检查清单

- 能解释 `package.json`、`pnpm-lock.yaml`、`tsconfig.json` 的作用。
- 能根据 README 跑通 `pnpm install`、`pnpm dev`。
- 能排查 Node 版本、环境变量、依赖缺失问题。
- 能说明 TypeScript AI 项目中服务端代码通常放在哪里。

## 🔗 关联笔记

- [[06-TS速查-Java视角]]
- [[08-Next.js-AppRouter基础]]
- [[09-Vercel-AI-SDK核心]]

## 📚 参考资料

- [pnpm Docs](https://pnpm.io/)
- [Node.js Docs](https://nodejs.org/docs/)
