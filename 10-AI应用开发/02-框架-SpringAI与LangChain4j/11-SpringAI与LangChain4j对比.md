---
title: Spring AI 与 LangChain4j 对比
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/SpringAI
  - 主题/LangChain4j
  - 阶段/02
created: 2026-07-04
updated: 2026-08-03
status: 已完成
mastery: ⭐☆☆☆☆
---

# ⚖️ Spring AI 与 LangChain4j 对比

> [!abstract] 一句话总结
> Spring Boot 企业项目优先 Spring AI；普通 Java、接口式 AI 服务、框架中立场景可以考虑 LangChain4j。先选一个做项目，比长期纠结框架更重要。

## 🎯 本节定位

- **本章第 11 课，收尾与选型**。前置：[[10-LangChain4j核心概念]]；本节给出 Spring AI 与 LangChain4j 的对比结论与选型建议。
- **延伸**：下一章进入 [[10-AI应用开发/03-知识库技术-RAG/_index|知识库技术 RAG]]，在 `VectorStore` 与 Advisor 基础上深入 RAG 工程。

## 📌 核心定位

| 维度 | Spring AI | LangChain4j |
| ---- | ---- | ---- |
| 背景 | Spring 官方生态 | 社区驱动 Java AI 框架 |
| 主要用户 | Spring Boot 开发者 | Java / JVM 开发者 |
| 风格 | Starter、AutoConfiguration、Bean | Builder、接口、普通 Java 抽象 |
| 强项 | 企业 Spring 集成、配置体系、Advisor | AiServices、框架中立、RAG 抽象清晰 |
| 学习心智 | 像 Spring Data / WebClient | 像 Java 版 LangChain，但不是照搬 |

---

## 🧩 抽象对照

| 能力 | Spring AI | LangChain4j |
| ---- | ---- | ---- |
| 聊天模型 | `ChatModel` | `ChatLanguageModel` |
| 业务调用入口 | `ChatClient` | `AiServices` / Model |
| 流式输出 | `ChatClient.stream()` | `StreamingChatLanguageModel` |
| 提示词模板 | `PromptTemplate` | PromptTemplate / 注解提示 |
| 工具调用 | `@Tool` / `.tools()` | `@Tool` / `.tools()` |
| 记忆 | `ChatMemory` + Advisor | `ChatMemory` |
| 向量库 | `VectorStore` | `EmbeddingStore` |
| RAG 注入 | Advisor | ContentRetriever / AiServices |
| Spring 集成 | 原生 | 可集成 |

---

## 🤔 怎么选

### 优先 Spring AI

- 项目本身就是 Spring Boot
- 团队熟悉 Spring 配置、Bean、Starter
- 需要和 Actuator、配置中心、企业中间件统一治理
- 希望 AI 能力像 Spring Data 一样纳入工程体系

### 优先 LangChain4j

- 项目不是 Spring Boot，或只想用普通 Java
- 喜欢用接口声明 AI 服务
- 想更直接地学习 RAG / Tool / Memory 组合
- 希望框架依赖更轻，不绑定 Spring 生命周期

### 都可以

- 简单聊天
- 工具调用
- RAG 知识库
- 多轮记忆
- 流式输出

---

## ⚙️ 示例对比

### Spring AI

```java
String answer = chatClient.prompt()
        .system("你是 Java AI 助手")
        .user("解释 RAG")
        .call()
        .content();
```

### LangChain4j

```java
interface Assistant {
    String chat(String message);
}

Assistant assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .build();

String answer = assistant.chat("解释 RAG");
```

Spring AI 更像“调用链 Builder”；LangChain4j 更像“声明一个 AI 接口”。

---

## 🧠 学习路线建议

```text
如果目标是 Java AI 应用开发 / 企业后端：
  Spring AI 主线
  LangChain4j 对照

如果目标是框架研究 / 多技术栈：
  两者都跑通
  抽象层做对照表
```

不要同时用两个框架做同一个小项目。学习时可以对照，生产中要避免重复抽象和依赖冲突。

---

## ✅ 学习检查点

- [ ] 能说出两者定位差异
- [ ] 能完成核心抽象对照
- [ ] 能按项目背景做选型
- [ ] 能解释为什么不要在同一模块混用两套 AI 编排框架

## ⚠️ 常见坑

- [ ] **框架对比内耗**：先用一个完成端到端项目。
- [ ] **混用导致复杂化**：同一条调用链里混两套 Memory / RAG 很难维护。
- [ ] **只看 Demo 不看治理**：企业项目还要看配置、观测、安全、测试。
- [ ] **忽略版本变化**：AI 框架迭代快，关键 API 以官方文档和项目锁定版本为准。

## 🎯 面试速记

> - **Q: Spring AI 和 LangChain4j 怎么选？**
>   - A: Spring Boot 企业项目优先 Spring AI；框架中立或偏接口式 AI Service 可选 LangChain4j。
> - **Q: 两者最大差异？**
>   - A: Spring AI 强在 Spring 生态集成和 Advisor 链；LangChain4j 强在普通 Java 抽象和 AiServices。

## 🔗 关联阅读

- [[10-LangChain4j核心概念]]
- [[02-ChatClient与ChatModel]]
- [[09-VectorStore向量存储]]
- [[_index]]

## 📚 参考资料

- [Spring AI Reference Documentation](https://docs.spring.io/spring-ai/reference/)
- [LangChain4j Documentation](https://docs.langchain4j.dev/)

