---
title: 02 · Spring AI 子区（索引）
tags:
  - 领域/AI应用
  - 类型/索引
  - 阶段/02
created: 2026-08-04
updated: 2026-08-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# 🌱 Spring AI 子区

> [!info] 本区说明
> Spring AI 是 Spring 官方出品的 Java AI 框架，深度融入 Spring Boot 生态（自动配置、Bean、可观测性）。本子区 9 篇从搭建到原语逐个讲透。**这是 02 章的学习主线**。

> [!info] 版本现状（2026-08）
> **Spring AI 2.0.0 GA**（Boot 4.x / MCP SDK 2.0 / Options 全 builder / 记忆需显式 conversationId）。维护线 1.1.8、1.0.9；迁移参考[官方 Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)。

## 📚 知识笔记清单（按学习顺序编号，顺着编号读即可）

- [x] [[01-Spring-AI项目搭建]] — Spring Boot + Spring AI 起步（BOM、配置、最小调用）
- [x] [[02-ChatClient与ChatModel]] — 调用入口与底层模型抽象
- [x] [[03-PromptTemplate]] — 模板化提示词
- [x] [[04-流式输出-SSE]] — 流式响应与前端体验
- [x] [[05-多模型接入与切换]] — 多 Provider / 多模型路由与降级
- [x] [[06-Function-Calling工具调用]] — Tool Calling / Agent 基础
- [x] [[07-Advisor拦截器]] — 对话链路增强（记忆 / RAG / 观测）
- [x] [[08-ChatMemory记忆]] — 多轮会话上下文（2.0 记忆三件套）
- [x] [[09-VectorStore向量存储]] — 向量检索与 RAG

## 🧭 学习路线

1. **跑通（01）**：BOM 管版本 + 最小依赖 + 第一个 `ChatClient` 调用。
2. **核心抽象（02→04）**：`ChatClient`/`ChatModel` 双层抽象、PromptTemplate、流式输出。
3. **工程能力（05）**：多模型接入、路由与降级（生产必会）。
4. **Agent/RAG 原语（06→09）**：工具调用、Advisor 链、记忆、向量库——为 05-Agent-MCP-Skills 和 03-RAG 章铺路。

## 🔗 相关

- 上级：[[10-AI应用开发/02-框架-SpringAI与LangChain4j/_index|02 · 框架总览]]
- 对照：[[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/_index|LangChain4j 子区]]
