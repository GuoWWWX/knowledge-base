---
title: Langfuse接入
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/评估与可观测
  - 阶段/06
created: 2026-07-04
updated: 2026-09-28
status: 已完成
mastery: ⭐☆☆☆☆
---

# Langfuse接入

> [!abstract] 一句话总结
> Langfuse 是 LLM 应用可观测与评估平台，用来记录 trace、span、generation、prompt、score 和 dataset；学习重点是把每次模型调用变成可复盘、可打分、可对比的工程数据。

## 📌 定位

Langfuse 解决的是“线上模型为什么这样回答”的问题。它不是业务日志替代品，也不是单纯 APM，而是专门面向 LLM 链路的 trace 平台。
适合接入的链路：
- Chat/RAG/Agent 的一次用户请求。
- 模型输入输出、token、耗时、模型名、成本。
- 检索片段、工具调用、重试和异常。
- 人工评分、自动评估、数据集回放和 Prompt 版本对比。

## 🤔 解决什么问题 / 为什么需要

Langfuse 可以把模型调用、检索和工具执行串成可排查的记录：
- 按需要记录 Prompt、上下文、工具结果和模型输出，并控制脱敏与访问权限。
- 按模型、用户或租户统计 Token、耗时和失败率。
- 将线上问题样本加入数据集，用 Ragas 或自定义评估器回放。
- 管理 Prompt 版本，并用同一批样例比较效果。

## 🧩 核心组件

| 组件 | 作用 | 学习重点 |
| ---- | ---- | -------- |
| Trace | 一次完整用户请求。 | trace_id 要和业务 request_id 互通。 |
| Span | 链路中的普通步骤。 | 检索、重排、工具调用、后处理都适合记录 span。 |
| Generation | 模型调用。 | 记录 model、prompt、completion、usage、latency。 |
| Score | 人工或自动评分。 | 评分维度要固定，避免不同人随意打分。 |
| Dataset | 可回放样例集。 | 从线上坏例沉淀回归集。 |

## ⚙️ 部署与接入流程

```mermaid
flowchart LR
    A[部署 Langfuse] --> B[创建 Project]
    B --> C[生成 Public/Secret Key]
    C --> D[SDK/OTel 接入]
    D --> E[记录 Trace/Generation]
    E --> F[打分与数据集]
    F --> G[回放评估]
```

1. 选择 Langfuse Cloud 或自托管；自托管要准备数据库、对象存储、密钥和外部访问域名。
2. 创建项目，生成 `public_key`、`secret_key` 和 `host`。
3. 在后端 SDK 或 OpenTelemetry 链路中创建 trace，写入用户、会话、租户、应用版本等 metadata。
4. 包裹模型调用，记录 prompt、completion、usage、model、latency 和 error。
5. 把检索、rerank、工具调用写成 span，方便定位 RAG 或 Agent 失败点。
6. 对关键样例打 score，并把坏例加入 dataset 做回归。

## 💻 Java 埋点示意

假设项目已配置 Micrometer Observation 和 OpenTelemetry 导出，`ragService` 为业务服务；下方片段只展示一次业务步骤，不会自动配置 Langfuse 接收端，未在本机运行。

```java
Observation.createNotStarted("rag.answer", observationRegistry)
    .observe(() -> ragService.answer(question));
```

接入 Langfuse 还需要配置 OTLP 导出地址、认证、采样和敏感信息处理，具体方式以所用版本的官方文档为准。验证时用同一请求 ID 核对业务日志和 trace。

## ⚠️ 生产坑

- 把完整身份证、手机号、合同金额等敏感字段原样写入 trace，后续导出和共享会有合规风险。
- 只记录最终答案，不记录检索片段和工具结果，排查时仍然无法判断错误来源。
- trace_id 没有回写到业务日志，Langfuse 和后端日志对不上。
- 自动评分没有固定样例和 rubric，分数波动会被误判成模型质量波动。
- 高并发场景同步上报 trace 会增加响应耗时，必要时使用异步 flush 或队列。

## 🎯 面试 / 选型

> [!question]- Langfuse 和普通日志平台有什么区别？
> 普通日志偏文本和系统事件；Langfuse 按 LLM 链路组织 trace、span、generation、prompt、score 和 dataset。

> [!question]- 接 Langfuse 最少要记录什么？
> 至少关联请求标识、模型、耗时、用量和错误；用户、租户和业务内容按排查需要记录，并做好脱敏、权限控制与保留期限管理。

> [!question]- Langfuse 怎么和 Ragas 配合？
> Langfuse 负责采集线上样例和存 score，Ragas 负责对 RAG 样例计算 faithfulness、context precision、answer relevancy 等指标。

## ✅ 精通检查清单

- 能部署或接入 Langfuse 项目并生成密钥。
- 能在一次 RAG 请求中记录 trace、span、generation。
- 能把线上坏例加入 dataset，并跑一次回归评估。
- 能说明敏感数据脱敏和 trace_id 贯通方案。

## 🔗 关联笔记

- [[_index]]
- [[07-Ragas评估实践]]
- [[08-LLM评估指标体系]]
- [[../00-总览/学习仪表盘]]

## 📚 参考资料

- [Langfuse Docs](https://langfuse.com/docs)；[Spring AI Observability](https://docs.spring.io/spring-ai/reference/observability/index.html)
- [Langfuse Self-hosting](https://langfuse.com/self-hosting)
