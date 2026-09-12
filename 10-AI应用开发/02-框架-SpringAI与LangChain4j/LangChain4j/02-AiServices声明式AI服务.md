---
title: AiServices声明式AI服务
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/LangChain4j
  - 阶段/02
created: 2026-08-04
updated: 2026-08-04
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# AiServices声明式AI服务

> [!abstract] 一句话总结
> `AiServices` 是 LangChain4j 的最大特色：**定义一个普通 Java 接口，框架自动生成实现**——把"模型调用、工具执行、记忆注入、RAG 检索"全部编排好，业务代码只写接口和 `@Tool` 方法。

## 🎯 本节定位

- **LangChain4j 子区第 2 课**。前置：[[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/01-LangChain4j核心概念]]（框架定位与核心抽象）。
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
        .chatLanguageModel(model)        // 模型
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
| `.chatLanguageModel(model)` | 装配模型 | 同步模型 |
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
        .chatLanguageModel(model)
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

## ⚠️ 踩坑与注意事项

- `@SystemMessage` 里的占位符用 `{{xxx}}`，参数用 `@V("xxx")` 注入——写错占位符不会编译报错，但运行期替换失败。
- `@Tool` 方法**必须是 public**，且参数/返回值要能被序列化成 JSON Schema 的简单类型。
- 工具方法抛异常时，把错误信息返回给模型比直接抛更稳（模型可以换个参数重试）。
- `AiServices` 生成的代理对 `final` 类/方法无效（同动态代理限制）。

## 🧪 最小验证

```java
interface Greeter {
    String greet(@UserMessage String name);
}
Greeter greeter = AiServices.builder(Greeter.class)
        .chatLanguageModel(OpenAiChatModel.builder()
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

- 本子区：[[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/01-LangChain4j核心概念]] · [[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/03-记忆与RAG]] · [[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/04-与Spring生态集成]]
- 对照：[[10-AI应用开发/02-框架-SpringAI与LangChain4j/SpringAI/06-Function-Calling工具调用|Spring AI 工具调用]]
- 选型：[[10-AI应用开发/02-框架-SpringAI与LangChain4j/01-SpringAI与LangChain4j对比|两框架对比]]

## 📚 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChain4j AiServices 教程](https://docs.langchain4j.dev/tutorials/ai-services)


