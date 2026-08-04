---
title: 02 · 框架 Spring AI 与 LangChain4j（索引）
tags:
  - 领域/AI应用
  - 类型/索引
  - 阶段/02
created: 2026-05-31
updated: 2026-08-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# ☕ 02 · 框架（Spring AI / LangChain4j）

> [!info] 本区说明
> 对应学习路径 **阶段二**。本区是 Java AI 框架的总览区：两个框架各有独立子目录深入介绍，本区负责选型对比与总览导航。Spring AI 为主，LangChain4j 为辅。

> [!info] 框架版本现状（2026-08）
> - **Spring AI 2.0.0 GA**（2026-06）：基于 Boot 4.x、MCP SDK 2.0；Options 全面 builder 化；记忆类 Advisor 需显式 conversationId；SAP HANA/Infinispan 向量库移除；可观测性增强。维护线：1.1.8、1.0.9。
> - **LangChain4j 1.18.1**（2026-07）：1.x 持续迭代。
> - 各篇示例以概念与稳定 API 为主，具体版本以官方 BOM 为准（见 [[SpringAI/01-Spring-AI项目搭建]]）。

## 📚 目录结构

| 入口 | 内容 | 什么时候读 |
| ---- | ---- | ---- |
| [[SpringAI/_index\|Spring AI 子区]] | Spring AI 9 篇：搭建、ChatClient、模板、流式、多模型、工具、Advisor、记忆、向量库 | **主线**，按编号读 |
| [[LangChain4j/_index\|LangChain4j 子区]] | LangChain4j 4 篇：核心概念、AiServices、记忆与 RAG、生态集成 | **对照**，学完 Spring AI 后读 |
| [[01-SpringAI与LangChain4j对比\|两框架对比与选型]] | 抽象对照表、选型结论 | 两个子区都看完后读 |

## 🧭 学习路线

1. **主线：Spring AI 子区**（[[SpringAI/01-Spring-AI项目搭建]] → [[SpringAI/09-VectorStore向量存储]]）——先跑通工程，再逐个掌握核心原语。
2. **对照：LangChain4j 子区**（[[LangChain4j/01-LangChain4j核心概念]] → [[LangChain4j/04-与Spring生态集成]]）——用 Spring AI 的心智对照理解，避免框架内耗。
3. **收尾：选型对比**（[[10-AI应用开发/02-框架-SpringAI与LangChain4j/01-SpringAI与LangChain4j对比]]）——按团队技术栈和场景做决策。

## 🧾 本章复习

- [[99-本章总结与复习]]

## 🔗 相关

- 学习路径对应章节：[[10-AI应用开发/00-总览/AI应用开发学习路径#第五章 分阶段学习计划]]（阶段二）
- 上级：[[10-AI应用开发/00-总览/学习仪表盘]]


