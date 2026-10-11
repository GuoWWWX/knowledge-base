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
> `AiServices` 是 LangChain4j 的最大特色：**定义一个普通 Java 接口，框架自动生成实现**——把"模型调用、工具执行、记忆注入、RAG 检索"全部编排好，业务代码只写接口和 `@Tool` 方法。

## 🎯 本节定位

- **LangChain4j 子区第 2 课**。前置：[[01-LangChain4j核心概念]]（框架定位与核心抽象）。
- 对照：Spring AI 的 `ChatClient` 是"链式风格"（`prompt().user().call()`），LangChain4j 的 `AiServices` 是"**声明式接口**"风格——两者是 Java AI 框架的两种心智模型，本课重点讲清这个差异。
- 延伸：工具方法（`@Tool`）的完整语义见 05-Agent-MCP-Skills 章。

## 📌 这是什么

`AiServices` 通过"接口 + 注解 + builder"三件套把 AI 能力声明出来：

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

**核心机制**：`AiServices.builder()` 在运行期为接口生成代理实现——调用 `chat(...)` 时自动完成"取记忆 → 组装 Prompt → 调模型 → 需要时执行工具 → 写回记忆"整条链路。

## 🤔 解决什么问题 / 为什么需要

- **把 AI 调用藏进业务接口**：上层代码不知道"这是 AI"，接口就是普通服务——方便测试（可换 mock）、方便替换实现。
- **声明式组合**：模型、工具、记忆、RAG 是"插槽"，builder 按需装配，而不是在业务代码里手写编排。
- **工具即方法**：`@Tool` 标注的 public 方法自动暴露给模型调用，不用手写 JSON Schema（框架生成）。

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

```java
// 带占位符的接口方法（参数自动注入）
interface SupportAgent {
    @SystemMessage("你是 {{company}} 的客服，语气专业友好")
    String answer(@V("company") String company, @UserMessage String question);
}

// 工具类：方法即工具
class OrderTool {
    @Tool("查询订单状态，返回 JSON")
    public String getOrderStatus(@ToolParam("订单号") String orderId) {
        return "{\"orderId\":\"" + orderId + "\",\"status\":\"已发货\"}";
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

- **模板占位符绑定**：`@SystemMessage` 与 `@UserMessage` 中的占位符采用 `{{xxx}}` 语法，需与方法参数上的 `@V("xxx")` 名称保持一致（单参数且未标注 `@V` 时默认绑定 `@UserMessage`），建议通过单元测试覆盖模板渲染。
- **工具方法签名与描述**：建议使用职责单一、签名清晰的 `public` 方法标注 `@Tool`，并在注解与 `@P` / `@ToolParam` 中明确说明参数格式与适用条件，以便框架准确生成 JSON Schema。
- **工具异常处理与写操作幂等**：对于参数校验不通过等可恢复业务错误，可返回脱敏后的提示文本供模型修正参数；对于权限不足或底层系统异常，应直接抛出异常中止调用链。涉及写操作的工具方法需保证业务幂等性。
- **基于接口的动态代理**：`AiServices` 基于 JDK 动态代理（`java.lang.reflect.Proxy`）在运行期生成实现，服务契约需声明为 Java `interface`。

## 🧪 最小验证

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
