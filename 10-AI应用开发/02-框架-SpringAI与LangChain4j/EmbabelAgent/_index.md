---
title: 02 · Embabel Agent 子区（索引）
tags:
  - 领域/AI应用
  - 类型/索引
  - 主题/Agent
  - 主题/EmbabelAgent
  - 阶段/02
created: 2026-08-13
updated: 2026-08-14
status: 已完成
mastery: ⭐☆☆☆☆
---

# Embabel Agent 子区

> [!info] 本区说明
> Embabel Agent 是面向 JVM 的强类型 Agent 规划与编排框架。它可以使用 Spring AI 提供的模型与工具接入能力，但定位高于一次模型调用：开发者声明 `Action`、`Goal` 和约束，由框架根据当前状态动态选择下一步。本子区从环境搭建讲到规划、工具、运行、测试和完整项目，目标是能独立实现一个可测试的 Java Agent。

> [!info] 版本现状（2026-08-14）
> 最新稳定版为 **1.5.0**，发布于 2026-08-11。该版本使用 Spring Boot 4.1.0、Spring AI 2.0.0，发布 POM 中 Kotlin 依赖为 2.2.21，并提供 MCP Client、A2A 等集成。消费工程的最终 Kotlin 运行时可能被 Spring Boot BOM 统一到更新版本，应以 `mvn dependency:tree` 为准。README 中仍可能出现旧版本示例，版本判断以 Maven Central 和 GitHub Releases 为准。

## 知识笔记清单

- [ ] [[01-Embabel-Agent概览与桌面Agent选型]] — 定位、与 Spring AI 的关系、成熟度和选型边界
- [ ] [[02-环境搭建与第一个Agent]] — Java 21、Maven、Starter、Shell 和第一个 Goal
- [ ] [[03-领域对象与Blackboard状态模型]] — 参数、返回类型和 Blackboard 如何形成规划图
- [ ] [[04-Action-Goal-Condition核心注解]] — 核心注解、条件、成本、副作用与幂等
- [ ] [[05-规划器与三种执行模式]] — GOAP、Utility、Hybrid、Supervisor 与 Focused/Closed/Open
- [ ] [[06-OperationContext-模型调用与工具集成]] — 结构化输出、模型选择、Tool、ToolGroup 和 MCP
- [ ] [[07-AgentInvocation与Process生命周期]] — 类型化调用、异步 Process、状态、事件与取消
- [ ] [[08-测试-可观测性与可靠性]] — Fake、Mock 集成测试、评测、Trace、预算和重试
- [ ] [[09-实战-学习计划Agent]] — 用同一个项目串起完整规划与测试闭环
- [ ] [[99-本章总结与复习]] — 核心模型、速查、自测题和掌握等级

## 学习路线

1. **定位（01）**：先判断 Embabel 解决什么、不解决什么。
2. **跑通（02）**：搭建固定 `1.5.0` 工程，在 Shell 中看到 Agent、Action、Goal。
3. **核心模型（03→05）**：按“Blackboard → 注解 → Planner”理解类型化规划，不先背高级 API。
4. **开发运行（06→07）**：把 LLM、Tool/MCP 接入 Action，再用 AgentInvocation/Process 接到产品入口。
5. **质量与实战（08→09）**：先写 Fake/Mock 测试，再完成学习计划 Agent。
6. **复习（99）**：闭卷画类型图、解释执行链，达到“能做项目”的检查项。

## 学习完成标准

- 能从 Goal 反推一条类型化 Action 路径。
- 能解释 Blackboard、ChatMemory 和数据库的区别。
- 能独立写 `OperationContext → PromptRunner → createObject`。
- 能选择 GOAP/Utility/Supervisor 与 Focused/Closed/Open。
- 能用 Fake 测一个 Action，用 Mock 集成测试跑完整 Goal。
- 能为 Process 设置预算、终止、观测和副作用边界。
- 能说明 Embabel 在桌面编码 Agent 架构中的位置。

## 相关

- 上级：[[../_index|02 · Java AI 框架总览]]
- 基础能力：[[../SpringAI/_index|Spring AI 子区]]
- 对照框架：[[../LangChain4j/_index|LangChain4j 子区]]
- 后续生产化：[[../../05-Agent-MCP-Skills/_index|Agent、MCP 与 Skills]]
