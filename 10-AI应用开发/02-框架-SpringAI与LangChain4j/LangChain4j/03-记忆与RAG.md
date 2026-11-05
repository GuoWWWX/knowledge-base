---
title: 记忆与RAG
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

# 记忆与RAG

> [!abstract] 一句话总结
> LangChain4j 用 `ChatMemory` 保存对话上下文，用 `EmbeddingStore` 和 `ContentRetriever` 检索知识；两者可以配置到 `AiServices`，但身份、数据权限和结果质量仍需应用负责。

## 🎯 本节定位

本篇接着 [[01-LangChain4j核心概念]] 和 [[02-AiServices声明式AI服务]]，介绍接入会话记忆与检索的组件；可以对照 Spring AI 的 [[../SpringAI/08-ChatMemory记忆|ChatMemory]] 和 [[../SpringAI/09-VectorStore向量存储|VectorStore]]。文档切分、向量化和重排等流程见 [[../../03-知识库技术-RAG/_index|RAG 专题]]。

## 📌 这是什么

### 记忆：ChatMemory / MessageWindowChatMemory

```java
// model 为已配置的 ChatModel；MessageWindowChatMemory 按消息条数（Message 数量）维护滑动窗口
interface Assistant {
    String chat(@MemoryId String conversationId, @UserMessage String message);
}

Assistant assistant = AiServices.builder(Assistant.class)
        .chatModel(model)
        .chatMemoryProvider(id -> MessageWindowChatMemory.withMaxMessages(20))
        .build();

String reply1 = assistant.chat("session-1001", "我叫小李");
String reply2 = assistant.chat("session-1001", "我叫什么？");
String reply3 = assistant.chat("session-1002", "我叫什么？");
```

**多会话隔离机制**：通过方法参数上的 `@MemoryId` 注解配合 `.chatMemoryProvider(...)`，框架会按会话 ID 为不同用户或会话分配独立的 `ChatMemory` 实例（若仅调用 `.chatMemory(...)` 传入固定实例，则所有调用共享同一份记忆）。在生产环境中，`conversationId` 应由服务端结合登录态生成并校验，且针对同一会话 ID 的并发请求需做好串行化控制。

### RAG：EmbeddingStore + ContentRetriever

假设 `store` 和 `embeddingModel` 已配置、文档片段已经入库。下面只展示检索器和 AI 服务的组装，省略模型、向量库依赖与认证，未在本机运行。

```java
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(store).embeddingModel(embeddingModel)
        .maxResults(3).build();
Assistant assistant = AiServices.builder(Assistant.class)
        .chatModel(model).contentRetriever(retriever).build();
```

入库时仍需解析文档、切分并生成向量；应用应保留来源信息并检查用户权限。检索到的资料可能不相关，回答也可能误用资料。

## 🤔 解决什么问题 / 为什么需要

- **记忆**：LLM 无状态，多轮对话必须由框架侧保存并注入历史——窗口记忆是"够用且简单"的默认方案（摘要/语义记忆是进阶，见 04-多轮对话章）。
- **RAG**：模型不知道你的私有知识，检索注入可为回答提供资料；应用仍要检查检索结果、引用和权限。

## 🧩 核心概念

| 组件 | 作用 | Spring AI 对照 |
| ---- | ---- | ---- |
| `ChatMemory` / `MessageWindowChatMemory` | 多轮上下文窗口 | `ChatMemory` / `MessageWindowChatMemory` |
| `@MemoryId` + `chatMemoryProvider` | 会话隔离 | `ChatMemory.CONVERSATION_ID` |
| `Document` / `TextSegment` | 文档与切分片段 | `Document` |
| `DocumentSplitters.recursive` | 递归切分器 | `TextSplitter`（切分规则不同） |
| `EmbeddingModel` / `EmbeddingStore` | 向量化 / 存储检索 | `EmbeddingModel` / `VectorStore` |
| `EmbeddingStoreContentRetriever` | 检索插槽 | `QuestionAnswerAdvisor` |

## 💻 代码实践 / 排查要点

问题排查时先检查解析出的片段和检索结果，再检查模型回答；不要把模型回答错误直接归因于生成模型。
- 检索结果太少 → 检查解析、切分、过滤条件和召回，评测调整 `maxResults` 的效果；噪声多时可以评估重排（见 RAG 章）。
- 向量库选型（Redis/Milvus/PGVector）见 [[../../03-知识库技术-RAG/01-RAG核心原理与全链路实战|RAG 章选型]]。
- 记忆"记不住" → 检查 `maxMessages` 是否太小、`chatMemoryId` 是否每次都不同。

## ⚠️ 踩坑与注意事项

- `InMemoryEmbeddingStore` 只适合学习 demo，**进程重启即丢**；生产用持久化向量库。
- 切分大小与重叠（500/100 之类的参数）直接影响检索质量，别照抄数字，按文档类型调（RAG 章有完整策略）。
- 标注 `@MemoryId` 的参数用于记忆路由，框架识别后**不会传给模型**——别在业务参数里混用。
- 记忆与检索同时装配时，Prompt 会同时包含历史与知识片段，注意总量别撑爆上下文窗口（见 01 章上下文工程）。

## 🧪 最小验证

用相同会话连续提问，再换一个经认证的会话 ID，检查上下文是否隔离。准备只存在于知识库中的事实，分别检查召回片段与实际回答；不能因为回答正确就假定引用也正确。

> [!question]- 会话记忆能代替 RAG 查找最新退货制度吗？
> 不能简单等同。记忆保存对话信息，RAG 从外部资料检索；制度更新后还要检查索引是否同步、用户是否有权访问。

## ✅ 精通检查清单

- 能写出 `MessageWindowChatMemory` + `@MemoryId` + `chatMemoryProvider` 的多会话示例。
- 能说清 `Document → TextSegment → EmbeddingStore → ContentRetriever` 的 RAG 组件链。
- 能对照 Spring AI 的记忆/向量组件说出两边概念映射。

## 🔗 关联笔记

- 本子区：[[01-LangChain4j核心概念]] · [[02-AiServices声明式AI服务]] · [[04-与Spring生态集成]]
- 对照：[[../SpringAI/08-ChatMemory记忆|Spring AI 记忆]] · [[../SpringAI/09-VectorStore向量存储|Spring AI 向量库]]
- 深化：[[../../03-知识库技术-RAG/_index|03-知识库技术-RAG]]

## 📚 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [AI Services：记忆隔离与并发边界](https://docs.langchain4j.dev/tutorials/ai-services/)
- [LangChain4j RAG 教程](https://docs.langchain4j.dev/tutorials/rag)
