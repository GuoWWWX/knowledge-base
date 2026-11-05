---
title: AiServices声明式AI服务
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/LangChain4j
  - 阶段/02
created: 2026-08-04
updated: 2026-09-28
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# AiServices声明式AI服务

> [!abstract] 一句话总结
> `AiServices` 可以为 Java 接口生成实现，让业务代码通过接口调用模型。工具、记忆和 RAG 等能力按需装配，由框架组织调用过程。

## 🎯 本节定位

本篇在 [[01-LangChain4j核心概念]] 的基础上介绍 `AiServices`：用接口声明 AI 服务，可与 Spring AI 的 `ChatClient` 调用方式对照。工具方法的用途和执行边界见 [[../../05-Agent-MCP-Skills/02-Tool-Use工具调用-代码实践|工具调用]]。

## 📌 这是什么

`AiServices` 用接口和注解声明能力，再通过 builder 装配组件。下面假设模型、工具、记忆和检索器已创建，省略导入和连接配置：

```java
// ① 定义接口（业务语义）
interface Assistant {
    @SystemMessage("你是一个 Java 后端专家，回答要简洁")
    String chat(@UserMessage String userMessage);
}

// ② 用 AiServices 生成实现
Assistant assistant = AiServices.builder(Assistant.class)
        .chatModel(model)        // 模型
        .tools(new CalculatorTool())     // 工具（可多个）
        .chatMemory(chatMemory)          // 记忆
        .contentRetriever(retriever)     // RAG 检索
        .build();

// ③ 直接当普通方法调用
String answer = assistant.chat("什么是虚拟线程？");
```

`AiServices.builder()` 在运行期为接口生成代理。调用 `chat(...)` 时，代理负责组装请求和调用模型；配置了记忆、工具或检索器时，才会执行对应步骤。

## 🤔 解决什么问题 / 为什么需要

- 业务代码通过接口调用，测试时可以替换为模拟实现。
- 模型、工具、记忆和检索器可以按需装配。
- 注册带 `@Tool` 的方法后，框架可以根据方法签名生成工具描述。

## 🧩 核心概念

| 概念 | 作用 | 说明 |
| ---- | ---- | ---- |
| `AiServices.builder(接口.class)` | 生成接口实现 | 运行时代理，自动编排调用链 |
| `@SystemMessage` / `@UserMessage` | 方法上的 Prompt 模板 | 方法参数自动填入占位符 |
| `@Tool` | 暴露工具方法 | 方法名+参数描述生成 JSON Schema |
| `.chatModel(model)` | 装配模型 | 同步模型 |
| `.chatMemory(memory)` | 装配记忆 | 自动带上历史并写回 |
| `.contentRetriever(...)` | 装配 RAG | 自动检索注入上下文 |

## 💻 代码实践

下面的订单工具返回固定演示文本，不连接真实订单服务；示例省略模型配置和导入：

```java
// 带占位符的接口方法（参数自动注入）
interface SupportAgent {
    @SystemMessage("你是 {{company}} 的客服，语气专业友好")
    String answer(@V("company") String company, @UserMessage String question);
}

// 工具类：方法即工具
class OrderTool {
    @Tool("查询订单状态，返回演示结果")
    public String getOrderStatus(@P("订单号") String orderId) {
        return "演示状态：已发货";
    }
}

// 装配
SupportAgent agent = AiServices.builder(SupportAgent.class)
        .chatModel(model)
        .tools(new OrderTool())
        .build();

String reply = agent.answer("极客科技", "我的订单 12345 到哪了？");
// 模型可能选择调用 getOrderStatus("12345")，拿到结果后组织回答
```

**与 Spring AI 的心智对比**（面试常问）：

| 维度   | Spring AI                                | LangChain4j                      |
| ---- | ---------------------------------------- | -------------------------------- |
| 调用风格 | `ChatClient` 链式：`prompt().user().call()` | `AiServices` 声明式：接口 + builder    |
| 工具   | `@Tool` + `.tools(...)`                  | `@Tool` + `.tools(...)`（同为方法即工具） |
| 业务集成 | Spring Bean + 注入                         | 接口代理，也可配 Spring Boot Starter     |
| 心智模型 | 显式编排调用链                                  | 隐式代理编排                           |

## ⚠️ 工程注意事项

- 模板使用 `{{变量名}}`，对应参数可以用 `@V` 指定名称；单参数模板也可以使用 `{{it}}`。
- 注册 `@Tool` 方法时，用 `@P` 描述参数；工具描述清楚不等于完成权限校验。
- 工具异常如何反馈或终止，取决于错误处理配置；不要把含敏感信息的异常原文直接交给模型。
- 写操作需要业务权限校验和幂等控制，避免重试时重复执行。
- `AiServices` 为 Java `interface` 生成代理实现，记忆和检索组件需要明确配置。

## 🧪 最小验证

下面需要已配置模型、有效密钥和网络；这是验证步骤，并非本机运行结果。

```java
interface Greeter {
    String greet(@UserMessage String name);
}
Greeter greeter = AiServices.builder(Greeter.class)
        .chatModel(OpenAiChatModel.builder()
                .apiKey(System.getenv("OPENAI_API_KEY"))
                .modelName("gpt-4o-mini")
                .build())
        .build();
System.out.println(greeter.greet("小明"));   // 一个普通接口方法，背后是 LLM 调用
```

## ✅ 精通检查清单

- 能说出 `AiServices` 与 Spring AI `ChatClient` 的两种心智差异。
- 能写一个带 `@SystemMessage` 占位符 + `@Tool` 工具方法的完整示例。
- 能解释"接口代理"是如何自动编排记忆/工具/检索链路的。

## 🔗 关联笔记

- 本子区：[[01-LangChain4j核心概念]] · [[03-记忆与RAG]] · [[04-与Spring生态集成]]
- 对照：[[../SpringAI/06-Function-Calling工具调用|Spring AI 工具调用]]
- 选型：[[../01-SpringAI与LangChain4j对比|两框架对比]]

## 📚 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChain4j AiServices 教程](https://docs.langchain4j.dev/tutorials/ai-services)
- [LangChain4j 工具调用](https://docs.langchain4j.dev/tutorials/tools/)
