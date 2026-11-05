---
title: 03 · 消息系统与Prompt工程
tags:
  - 领域/AI应用
  - 类型/核心机制
  - 主题/AgentScope
  - 主题/消息协议
  - 阶段/02
created: 2026-10-30
updated: 2026-10-30
status: 已完成
mastery: ⭐⭐⭐⭐⭐
---

# 💬 消息系统与 Prompt 工程

> [!abstract] 一句话总结
> Agent 通过消息表达用户输入、模型回复和工具结果；提示词规定任务与输出要求，但应用仍需在工具和服务端校验权限与业务规则。

## 🧩 一次调用里有哪些消息？

用户问“订单 A123 到哪了？”Agent 将用户消息交给模型；模型可能请求订单查询，工具执行后把结果返回，再由模型生成回答。工具结果是外部数据，不应覆盖系统指令。

| 内容 | 作用 |
| :--- | :--- |
| 系统指令 | 描述角色、任务边界和回答要求 |
| 用户消息 | 提供当前问题及必要参数 |
| 工具调用与结果 | 记录模型要求执行的操作及其返回数据 |
| 模型回复 | 面向用户的最终回答或下一步工具请求 |

## 💻 构造用户消息

以下是短片段，假设 Agent 和 `RuntimeContext` 已经配置；返回值是响应式结果。没有在本机连接模型运行。

```java
UserMessage question = new UserMessage("订单 A123 到哪了？");
Mono<Msg> response = agent.call(question, runtimeContext);
```

响应式结果需要订阅后才执行；命令行可以用 `block()` 等待，WebFlux 中应返回响应式结果并避免阻塞事件循环。

## 📝 提示词怎么写？

写清角色、任务、需要查询的事实、证据不足时如何处理，以及希望返回的格式。订单状态这类动态事实应由工具提供；提示词中不要放密钥，也不要相信用户输入里的“忽略此前规则”等指令。
输入转义可以避免部分模板语法冲突，但不等于防住提示词注入；权限限制需要由应用和工具执行。

## 🧱 结构化输出有哪些边界？

例如希望把订单状态交给后续 Java 程序处理，可以定义返回结构，再请求 Agent 按该结构输出。假设 `agent` 和认证后的 `runtimeContext` 已配置；这是接口片段，未实际连接模型运行。

```java
record OrderSummary(String orderId, String status) {}
Mono<Msg> result = agent.call(List.of(question), OrderSummary.class, runtimeContext);
Mono<OrderSummary> summary = result.map(msg -> msg.getStructuredData(OrderSummary.class));
```

`OrderSummary.class` 描述期望的结构，真正的状态仍应来自工具或业务系统。即使字段能成功映射，也要校验必填字段、取值范围和事实；模型不支持相关能力、输出被截断或校验失败时，应有明确错误处理。

> [!question]- 提示词写了“只能查询自己的订单”，就不需要鉴权了吗？
> 不能省。模型可能误调用工具或提供错误参数；服务端需要独立验证身份、订单归属和权限。

## 🔗 关联与资料

- [[02-第一个Agent与工具调用]]；[[04-流式输出与事件驱动]]。
- [AgentScope Java：Agent](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.4/docs/v2/en/docs/building-blocks/agent.md)
