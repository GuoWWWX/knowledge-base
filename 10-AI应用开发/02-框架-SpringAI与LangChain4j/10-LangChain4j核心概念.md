---
title: LangChain4j 核心概念
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/LangChain4j
  - 阶段/02
created: 2026-07-04
updated: 2026-08-03
status: 已完成
mastery: ⭐☆☆☆☆
---

# 🔗 LangChain4j 核心概念

> [!abstract] 一句话总结
> LangChain4j 是 Java 版 LLM 应用开发框架，核心是把模型、记忆、工具、RAG 和 AI Service 组织成可组合的 Java 抽象。

## 🎯 本节定位

- **本章第 10 课，LangChain4j 对照**。前置：前 9 课建立的 Spring AI 心智；本节以对照视角看 LangChain4j 的核心抽象（`ChatLanguageModel`、`AiServices` 等）。
- **延伸**：选型结论在下一课 [[11-SpringAI与LangChain4j对比]]。

> [!info] 版本现状（2026-08）
> LangChain4j 当前 **1.18.1**（2026-07 发布），1.x 持续稳定迭代；`OpenAiChatModel.builder()`、`AiServices`、`@Tool` 等核心 API 自 1.0 GA 起保持稳定，本课示例可直接使用。

## 📌 这是什么

LangChain4j 面向 Java 开发者，目标是简化 LLM 应用开发。它不像 Spring AI 那样强绑定 Spring 生态，而是提供一组普通 Java 抽象，也能和 Spring Boot 集成。

```text
ChatLanguageModel
  + AiServices
  + ChatMemory
  + Tools
  + EmbeddingStore / ContentRetriever
  = Java AI 应用
```

## 🧩 核心抽象

| 概念 | 作用 | Spring AI 对照 |
| ---- | ---- | ---- |
| `ChatLanguageModel` | 同步聊天模型 | `ChatModel` |
| `StreamingChatLanguageModel` | 流式聊天模型 | `ChatClient.stream()` |
| `AiServices` | 用接口声明 AI 服务 | `ChatClient` + 业务 Service |
| `ChatMemory` | 对话记忆 | `ChatMemory` |
| `@Tool` | 工具调用 | `@Tool` / Tool Calling |
| `EmbeddingModel` | 文本向量化 | `EmbeddingModel` |
| `EmbeddingStore` | 向量存储 | `VectorStore` |
| `ContentRetriever` | RAG 检索器 | Advisor / Retriever |

---

## 💻 最小调用

```java
ChatLanguageModel model = OpenAiChatModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("gpt-4.1-mini")
        .build();

String answer = model.chat("用一句话解释 LangChain4j");
```

## 🧠 AiServices：接口即 AI 服务

LangChain4j 很有辨识度的能力是 `AiServices`：用 Java 接口定义 AI 能力。

```java
interface Assistant {
    String chat(String userMessage);
}

Assistant assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .build();

String answer = assistant.chat("解释 RAG");
```

这种写法适合把 AI 能力包装成领域服务，例如：

```java
interface ResumeReviewer {
    ReviewResult review(String resumeText);
}
```

---

## 🛠️ 工具调用

```java
class WeatherTools {

    @Tool("根据城市查询天气")
    String weather(String city) {
        return city + "：晴，28℃";
    }
}

Assistant assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .tools(new WeatherTools())
        .build();
```

工具调用心智和 Spring AI 一样：模型决定工具和参数，Java 代码负责执行。

---

## 📚 RAG 概念

LangChain4j 中 RAG 常见链路：

```text
Document
  → DocumentSplitter
  → EmbeddingModel
  → EmbeddingStore
  → ContentRetriever
  → AiServices
```

它把“文档处理、向量存储、检索、注入模型”拆得比较清楚，适合学习 RAG 工程化。

---

## ✅ 学习检查点

- [ ] 能说出 `ChatLanguageModel` 和 `AiServices` 的关系
- [ ] 能用接口声明一个 AI 服务
- [ ] 能解释 `EmbeddingStore` 和 Spring AI `VectorStore` 的对应关系
- [ ] 能说明 LangChain4j 更偏普通 Java 抽象

## ⚠️ 常见坑

- [ ] **把 LangChain4j 当 Python LangChain 原样迁移**：生态和 API 不是一回事。
- [ ] **接口方法太模糊**：AiServices 方法名、参数名、注解要清楚。
- [ ] **忽略 Spring Boot 集成边界**：在 Spring 项目里要统一配置和 Bean 生命周期。
- [ ] **RAG 只看生成**：同样要评估检索质量。

## 🎯 面试速记

> - **Q: LangChain4j 的特色是什么？**
>   - A: 普通 Java 抽象清晰，`AiServices` 可以用接口定义 AI 能力，工具、记忆、RAG 组合方便。
> - **Q: 它和 Spring AI 最大差异？**
>   - A: Spring AI 更 Spring Boot 原生；LangChain4j 更框架中立，接口式 AI Service 很突出。

## 🔗 关联阅读

- [[11-SpringAI与LangChain4j对比]]
- [[02-ChatClient与ChatModel]]
- [[09-VectorStore向量存储]]
- [[_index]]

## 📚 参考资料

- [LangChain4j Documentation](https://docs.langchain4j.dev/)
- [LangChain4j Tutorials](https://docs.langchain4j.dev/category/tutorials/)
