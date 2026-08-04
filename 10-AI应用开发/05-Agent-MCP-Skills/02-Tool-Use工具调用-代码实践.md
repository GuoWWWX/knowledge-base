---
title: Tool-Use工具调用-代码实践
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/Agent
  - 阶段/05
created: 2026-07-04
updated: 2026-08-03
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# 🛠️ Tool Use 工具调用 · 代码实践

> [!abstract] 一句话总结
> Tool Use（Function Calling）是**一切 Agent 的基石**：模型输出"要调哪个工具 + 什么参数"，应用执行工具并把结果回传给模型。本篇用 Spring AI 走通"定义工具 → 模型选工具 → 执行 → 回传结果"的完整循环，并讲清楚 schema 生成、参数校验、错误处理这些工程细节——这是 03 篇 ReAct 实现的前置。

> [!info] 本节定位
> 前置：[[01-Agent设计模式详解]]（Tool Use 在模式全景中的位置）。本篇是**代码实践**篇，讲清楚调用循环的每个环节；[[03-ReAct-Spring-AI实现]] 在其上叠加思考循环。延伸：[[09-MCP协议详解]]（把工具暴露成跨应用协议）、[[20-Agent安全与防注入]]（工具调用的安全边界）。

## 📌 这是什么

一次工具调用在 Spring AI 里走四条消息：

```text
用户问题
  ↓ ① 模型收到问题 + 工具清单（schema）
② 模型输出 tool_calls：{name: "queryOrder", arguments: {orderId: "A1001"}}
  ↓ ③ 应用执行工具，把结果作为 tool 消息回传
④ 模型基于结果生成最终答案
```

**关键认知**：模型**不会真的执行任何代码**——它只负责"选工具 + 填参数"，执行、鉴权、校验、审计全部发生在你的应用里。这个边界决定了工具调用的工程职责：**模型是建议者，服务端是执行者**。

## 🧩 核心概念

| 概念 | 含义 | 工程要点 |
| ---- | ---- | -------- |
| Tool Schema | 模型的工具清单（名字、参数、描述） | 描述决定模型选得准不准，必须具体 |
| Tool Execution | 应用侧执行真实逻辑 | 鉴权、幂等、超时、异常映射 |
| Tool Message | 执行结果回传给模型 | 模型靠它生成下一步或最终答案 |
| 循环终止 | 模型不再输出 tool_calls 即结束 | 必须限制 maxSteps 防死循环 |
| 工具调用日志 | 每次调用的审计记录 | 用户、租户、参数摘要、结果 |

## 💻 代码实践

### 1. 定义工具（Spring AI @Tool 注解）

```java
@Service
class OrderTools {
    private final OrderService orderService;

    @Tool(description = "根据订单号查询订单状态，订单号形如 A 开头 + 4 位数字，如 A1001")
    String queryOrder(String orderId) {
        return orderService.queryStatus(orderId);
    }

    @Tool(description = "查询指定用户的全部订单，userId 为内部系统 ID")
    List<Order> listOrders(String userId) {
        return orderService.listByUser(userId);
    }
}
```

> [!note] 描述怎么写
> 描述里带上**参数格式和示例**（`订单号形如 A1001`），模型才能填对参数。描述模糊是"模型该调不调、调错参数"的第一大原因。

### 2. 把工具交给 ChatClient

```java
@Bean
ChatClient chatClient(ChatClient.Builder builder, OrderTools orderTools) {
    return builder
        .defaultTools(orderTools)   // 扫描 @Tool 方法，自动生成 schema
        .build();
}
```

调用：

```java
String answer = chatClient.prompt()
    .user("帮我查一下订单 A1001 到哪了")
    .call()
    .content();
```

Spring AI 自动完成：生成 schema → 传给模型 → 模型输出 tool_calls → 执行 → 结果回传 → 最终答案。

### 3. 手动控制循环（看透内部机制）

想看清楚每一步，用 ChatModel 手动驱动：

```java
List<Message> messages = new ArrayList<>();
messages.add(new UserMessage("查一下订单 A1001 状态"));

for (int step = 0; step < 5; step++) {          // ① maxSteps 限制，防死循环
    ChatResponse resp = chatModel.call(new Prompt(messages));
    AssistantMessage assistantMsg = resp.getResult().getOutput();
    messages.add(assistantMsg);

    if (assistantMsg.hasToolCalls()) {          // ② 模型要调工具
        for (ToolCall call : assistantMsg.getToolCalls()) {
            String result = executeTool(call);   // ③ 执行 + 鉴权 + 日志
            messages.add(new ToolResponseMessage(call.id(), result));
        }
    } else {
        return assistantMsg.getText();           // ④ 不再调工具 → 输出答案
    }
}
throw new IllegalStateException("超过最大步数，终止");   // ⑤ 防死循环兜底
```

```java
private String executeTool(ToolCall call) {
    // 鉴权：该用户能否调用这个工具？（不能只看模型传参）
    // 校验：call.arguments() 里的参数是否合法
    // 幂等：写操作要带 requestId，防重试重复执行
    // 审计：记录 用户、工具名、参数摘要、结果
    return toolDispatcher.dispatch(call.name(), call.arguments());
}
```

> [!important] 五个工程细节
> 1. **maxSteps 必须设**：模型可能反复调同一个工具，不限制就是死循环 + 烧钱。
> 2. **参数要二次校验**：模型填的参数不可信，执行前用 schema 校验（类型、必填、枚举、长度）。
> 3. **写操作要幂等**：工具执行失败重试时，不能让"转账"被执行两次。
> 4. **鉴权在服务端**：不信任模型传的 userId，从会话上下文取。
> 5. **每次调用留日志**：这是排查"模型为什么乱调"的唯一依据。

## 🔍 源码 / 原理深挖

### Spring AI 工具调用的三层

| 层 | 类 | 职责 |
| ---- | ---- | ---- |
| 注解层 | `@Tool` / `@ToolParam` | 声明工具与参数描述 |
| 注册层 | `ToolCallback` | 把方法包装成"schema + 执行器"的单元 |
| 执行层 | `ToolCallingManager` | 解析模型输出的 tool_calls，调用对应 ToolCallback |

**schema 是怎么生成的**：`@Tool` 注解的 `description` + 方法签名（参数名、类型、`@ToolParam` 描述）会被序列化成 JSON Schema，随请求发给模型——所以**参数类型用简单类型**（String/int/枚举），复杂对象模型填不准。

## ⚠️ 踩坑与注意事项

- [ ] **工具描述模糊**：模型该调用时不调用，或参数填错
- [ ] **不限制 maxSteps**：模型反复调用工具，死循环 + 成本失控
- [ ] **信任模型传参**：userId、权限参数直接用了，越权风险
- [ ] **写操作不幂等**：重试导致重复转账、重复发单
- [ ] **不记录调用日志**：线上模型乱调，无从排查
- [ ] **工具返回超大结果**：整个塞回上下文，撑爆窗口（应截断/摘要后回传）
- [ ] **异常处理粗暴**：工具抛异常直接断链，应转为错误消息回传，让模型重试或换工具

## 🎯 面试问题

> [!question] 高频问题
> - **Q: 模型"调用工具"时，代码真的执行了吗？**
>   - A: 没有。模型只输出 tool_calls（工具名 + 参数 JSON），执行发生在应用侧；模型拿到执行结果后再生成答案。
> - **Q: 工具调用循环怎么防止死循环？**
>   - A: 限制 maxSteps + 每步都记录日志；超过步数直接终止并抛错。生产上还要监控单次对话的调用次数和成本。
> - **Q: 工具参数模型填错了怎么办？**
>   - A: 两层：一是执行前用 JSON Schema 二次校验（类型/必填/枚举）；二是把校验失败转成清晰的错误消息回传给模型，让它修正参数重试。
> - **Q: Function Calling 和 MCP 的关系？**
>   - A: Function Calling 是应用内工具调用；MCP 把工具按标准协议暴露，跨应用复用。MCP Server 内部本质上还是"模型选工具 + 应用执行"的循环。详见 [[09-MCP协议详解]]。

## ✅ 精通检查清单

- 能画出工具调用循环的四条消息，并指出"模型只建议、应用才执行"这个边界
- 能写出 Spring AI 的 @Tool 定义 + ChatClient 接入 + 手动循环三版代码
- 能说出五个工程细节（maxSteps、参数校验、幂等、服务端鉴权、日志）并解释各自防什么
- 能解释 @Tool 方法签名如何变成发给模型的 JSON Schema
- 能答出"模型填错参数怎么办""怎么防死循环"等面试题

## 🔗 关联笔记

- [[01-Agent设计模式详解]] —— Tool Use 在模式全景中的位置
- [[03-ReAct-Spring-AI实现]] —— 在其上叠加"思考-行动-观察"循环
- [[09-MCP协议详解]] —— 把工具暴露成跨应用协议
- [[20-Agent安全与防注入]] —— 工具调用的安全边界
- [[22-Agent成本控制与降级]] —— 调用次数与成本控制
- [[_index]]

## 📚 参考资料

- [Spring AI Function Calling 文档](https://docs.spring.io/spring-ai/reference/api/chat/functions.html)
- [OpenAI Function Calling Guide](https://platform.openai.com/docs/guides/function-calling)
