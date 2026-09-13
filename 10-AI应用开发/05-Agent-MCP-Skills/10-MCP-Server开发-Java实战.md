---
title: MCP-Server开发-Java实战
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/MCP
  - 阶段/05
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# MCP-Server开发-Java实战

> [!abstract] 一句话总结
> Java MCP Server 的目标是把企业内部能力用 MCP 标准暴露给 Agent：工具 schema 清楚、权限在服务端执行、传输和审计可生产化。

## 📌 这是什么

MCP Server 是 Agent 访问外部能力的标准化服务端。对 Java 团队来说，它通常包装已有 Spring Boot 服务：

1. 把订单、库存、知识库、工单等能力暴露为 MCP tools。
2. 用 resources 暴露可读上下文，如文档、配置、业务实体摘要。
3. 用 prompts 提供可复用提示词模板。
4. 通过 stdio（本地）或 Streamable HTTP（远程）被 IDE、Agent Runtime、网关调用。

## 🤔 解决什么问题 / 为什么需要

- **统一工具协议**：不用为每个 Agent 单独写私有插件。
- **隔离业务系统**：Agent 只看到受控 tool，不直接拿数据库账号。
- **复用企业权限**：Java 服务继续使用 Spring Security、租户、审计。
- **便于调试**：Inspector 能列出 tools、查看 schema、发起测试调用。

## 🧩 设计要点

| 模块 | Java 实现关注点 | 生产要求 |
| ---- | -------------- | -------- |
| Tool schema | 参数名、类型、描述、必填项 | 描述必须具体，避免模型误填。 |
| Handler | 调用领域服务 | 鉴权、幂等、超时、异常映射。 |
| Transport | stdio / Streamable HTTP（SSE 已废弃） | 本地开发和生产部署分开配置。 |
| Audit | tool call 日志 | 记录用户、租户、参数摘要、结果。 |
| Version | server 与 tool 版本 | 避免客户端缓存旧 schema。 |

## ⚙️ 实现步骤

```mermaid
flowchart LR
    A[选择业务能力] --> B[定义Tool输入输出]
    B --> C[编写Spring服务]
    C --> D[注册MCP Tool]
    D --> E[Inspector调试]
    E --> F[接入Agent Client]
    F --> G[监控和审计]
```

1. 先选只读工具做 MVP，例如订单查询、库存查询、知识库检索。
2. 为每个工具写清楚 description：何时使用、参数格式、权限边界、返回含义。
3. Handler 内部调用领域服务，不在 MCP 层直接写 SQL。
4. 对写操作增加幂等键、人工确认或审批状态，不让 Agent 直接执行不可逆操作。
5. 用 Inspector 调用 `tools/list` 和 `tools/call`，确认 schema 与返回结构。

## 💻 Java / Spring AI 示例

```java
@Service
class OrderMcpTools {
    private final OrderService orderService;

    OrderMcpTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @Tool(description = "查询当前登录用户的订单状态。参数 orderId 必须是业务订单号。")
    OrderStatusView queryOrderStatus(
            @ToolParam(description = "业务订单号，例如 A20260704001") String orderId) {
        UserPrincipal user = SecurityContext.currentUser();
        Order order = orderService.mustBelongToUser(orderId, user.id());
        return new OrderStatusView(order.id(), order.status(), order.carrier(), order.updatedAt());
    }
}
```

```java
@Configuration
class McpServerConfig {
    @Bean
    ToolCallbackProvider orderToolProvider(OrderMcpTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
```

```yaml
spring:
  ai:
    mcp:
      server:
        name: order-mcp-server
        version: 1.0.0
        type: sync
```

## ⚠️ 工程坑

- 工具描述只写“查询订单”，模型不知道输入订单号还是用户号。
- Tool 返回整个 Entity，泄露内部字段；应返回专门的 View DTO。
- 在工具参数中接受 `userId` 并信任它，导致越权查询。
- 本地 stdio 正常，生产 HTTP 没配置鉴权和超时。
- 修改 tool schema 后客户端仍缓存旧定义，调用参数不匹配。

## 🎯 面试问题

> [!question] 高频问题
> - Q: MCP Server 和普通 REST API 有什么区别？
>   - A: REST 面向程序员调用；MCP 面向 Agent 暴露工具、资源和 prompt，并带有可被模型理解的 schema 与描述。
> - Q: Java MCP Server 的鉴权放在哪里？
>   - A: 放在服务端 handler 和领域服务里，不能依赖模型传参；MCP 传输层也要做认证。
> - Q: 怎么设计一个安全的写工具？
>   - A: 参数最小化、幂等键、状态机、人工审批、审计日志，必要时拆成“生成申请”和“确认执行”。

## ✅ 精通检查清单

- 能用 Spring 服务注册一个 MCP tool。
- 能用 Inspector 验证 tools/list 和 tools/call。
- 能说明 MCP server 的鉴权、审计和版本策略。
- 能把 Entity 返回改成安全 DTO。

## 🔗 关联笔记

- [[_index]]
- [[09-MCP协议详解]]
- [[12-MCP调试-Inspector]]
- [[../00-总览/学习仪表盘]]

## 📚 参考资料

- [Spring AI MCP Server Boot Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
- [Model Context Protocol Docs](https://modelcontextprotocol.io/)
