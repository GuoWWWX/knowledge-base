---
title: 02 · LangChain4j 子区（索引）
tags:
  - 领域/AI应用
  - 类型/索引
  - 阶段/02
created: 2026-08-04
updated: 2026-09-28
status: 已完成
mastery: ⭐☆☆☆☆
---

# 🔗 LangChain4j 子区

> [!info] 本区说明
> LangChain4j 是社区驱动的 Java LLM 框架（对应 Python 的 LangChain），不强制绑定 Spring，以普通 Java 抽象 + `AiServices` 声明式 AI 服务为特色，也可与 Spring Boot 集成。本子区以"对照 Spring AI"的视角讲透。**作为 02 章的对照线，在 Spring AI 主线之后读。**

> [!info] 版本现状（2026-08）
> **LangChain4j 1.18.1**（2026-07），1.x 持续稳定迭代；`OpenAiChatModel.builder()`、`AiServices`、`@Tool` 等核心 API 自 1.0 GA 起稳定。

## 📚 知识笔记清单（按学习顺序编号，顺着编号读即可）

- [ ] [[01-LangChain4j核心概念]] — 框架定位与核心抽象总览
- [ ] [[02-AiServices声明式AI服务]] — 用接口声明 AI 服务（LangChain4j 最大特色）
- [ ] [[03-记忆与RAG]] — ChatMemory、EmbeddingStore、ContentRetriever
- [ ] [[04-与Spring生态集成]] — Spring Boot Starter、与 Spring AI 共存
- [ ] [[05-流式输出与结构化输出]] — TokenStream 流式、OutputParser/POJO 结构化
- [ ] [[06-多模型接入与配置]] — 多 Provider builder、Embedding 模型、切换与降级

## 🧭 学习路线

1. **总览（01）**：`ChatModel` + `AiServices` + 记忆 + 工具 + 检索 = Java AI 应用。
2. **特色（02）**：`AiServices` 接口声明式——定义接口即获得 AI 服务，重点对比 `ChatClient` 的链式风格。
3. **能力（03→05）**：记忆与 RAG 组件（对照 Spring AI 的 `ChatMemory`/`VectorStore`），流式与结构化输出（对照 Spring AI 的 SSE/Structured Output）。
4. **工程（04→06）**：Spring Boot 集成方式、多模型接入与切换，最后读选型对比收尾。

## 🔗 相关

- 上级：[[../_index|02 · 框架总览]]
- 对照：[[../SpringAI/_index|Spring AI 子区]]
- 选型：[[../01-SpringAI与LangChain4j对比|两框架对比]]
