---
title: 08 · Spring整合与生产部署
tags:
  - 领域/AI应用
  - 类型/工程实战
  - 主题/AgentScope
  - 主题/SpringBoot
  - 主题/生产部署
  - 阶段/02
created: 2026-10-30
updated: 2026-10-30
status: 已完成
mastery: ⭐⭐⭐⭐⭐
---

# 🌱 Spring 整合与生产部署

> [!abstract] 一句话总结
> Spring 负责管理模型、工具和应用服务的生命周期；AgentScope 负责 Agent 调用。身份、权限、持久化、超时与监控仍需由应用明确设计。

## 🎯 怎么接入现有服务？

可以把模型、`Toolkit` 和业务工具交给 Spring 管理，再在配置类中创建 Agent。先确认使用的 AgentScope 版本、模型扩展和 Spring Boot 兼容关系；不同版本的 Starter 与配置项可能不同，不要照搬其他版本的配置。

## 💻 创建 Agent 的片段

以下片段假设 `model` 和 `toolkit` 已经由应用配置并注入；省略 imports、模型密钥和依赖，未在本机编译或调用模型。

```java
@Bean
ReActAgent orderAgent(Model model, Toolkit toolkit) {
    return ReActAgent.builder().name("order_agent")
            .sysPrompt("根据工具查询结果回答订单问题。")
            .model(model).toolkit(toolkit).build();
}
```

在 `Toolkit` 中只注册应用需要的工具。扫描整个容器并自动暴露所有工具可能扩大可调用范围，应明确注册和检查工具权限。

## 🔐 用户和会话如何传递？

Web 请求先由服务端完成认证、确定租户和用户身份，再创建 `RuntimeContext` 传入 Agent；业务工具基于该身份再次检查具体资源权限。前端传入的 `userId` 或模型生成的参数不能替代认证结果。
会话隔离、持久化与恢复要按具体状态存储配置验证；多实例部署时不能假设本地文件天然在各节点共享。

## 🚦 WebFlux 与流式输出

Agent 的事件流可用于展示文本片段、工具调用与执行状态。WebFlux 控制器应返回响应式结果，由框架订阅；不要在事件循环线程调用 `block()`。如果工具使用阻塞数据库驱动，也要合理隔离阻塞工作，不能仅凭返回类型是 `Mono` 就认定整个链路非阻塞。
流被取消、客户端断开或工具超时后，需要测试 Agent 和外部任务是否正确停止。

## 🧪 上线前应验证什么？

- 对模型与工具分别设置超时，按操作是否有副作用决定能否重试。
- 记录请求和工具调用的关联标识，保护密钥与敏感日志。
- 健康检查应明确测试的依赖；没有实际调用模型或探测服务时，不应宣称模型连接正常。
- 并发测试同一会话、不同用户和多实例恢复；有写操作时验证幂等与权限。

> [!question]- 给方法返回 `Mono`，是否意味着数据库调用一定不会阻塞？
> 不是。内部如果调用阻塞驱动，线程仍会等待；要选择非阻塞驱动或把阻塞工作隔离到合适的线程池。

## 🔗 关联与资料

- [[09-实战-订单查询Agent]]：把身份与订单工具放进完整流程。
- [AgentScope Java：快速开始](https://github.com/agentscope-ai/agentscope-java/blob/v2.0.4/docs/v2/en/docs/quickstart.md)
- [Spring Framework：WebFlux](https://docs.spring.io/spring-framework/reference/web/webflux.html)
