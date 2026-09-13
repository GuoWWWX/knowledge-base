---
title: 记忆与RAG
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

# 记忆与RAG

> [!abstract] 一句话总结
> LangChain4j 的记忆与 RAG 组件和 Spring AI 一一对应：`ChatMemory`（`MessageWindowChatMemory`）管多轮上下文，`EmbeddingStore` + `ContentRetriever` 管知识检索——装配进 `AiServices` 即可自动生效，不需要手写编排。

## 🎯 本节定位

- **LangChain4j 子区第 3 课**。前置：[[01-LangChain4j核心概念]]、[[02-AiServices声明式AI服务]]（本课组件都是往 `AiServices` 里装配的"插槽"）。
- 对照：Spring AI 的 [[../SpringAI/08-ChatMemory记忆|ChatMemory]] 与 [[../SpringAI/09-VectorStore向量存储|VectorStore]]——两边概念同源，API 不同。
- 延伸：RAG 全链路（切分/向量化/检索/重排）详见 03-知识库技术-RAG 章，本课只讲框架侧组件。

## 📌 这是什么

### 记忆：ChatMemory / MessageWindowChatMemory

```java
// 窗口记忆（与 Spring AI 2.0 的 MessageWindowChatMemory 同名同思路）
ChatMemory chatMemory = MessageWindowChatMemory.builder()
        .maxMessages(20)      // 保留最近 20 条
        .build();

// 装配进 AiServices：每轮对话自动带上历史并写回
Assistant assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .chatMemory(chatMemory)
        .build();

// 多会话：用 chatMemoryId 区分不同会话
String reply1 = assistant.chat("user-1001", "我叫小李");
String reply2 = assistant.chat("user-1001", "我叫什么？");   // 记得住
String reply3 = assistant.chat("user-1002", "我叫什么？");   // 新会话，不知道
```

**注意**：LangChain4j 的 `AiServices` 接口方法若带 `chatMemoryId` 参数，框架自动用它做会话隔离——这对应 Spring AI 2.0 显式 `conversationId` 的设计，但写法更省事。

### RAG：EmbeddingStore + ContentRetriever

```java
// ① 离线侧：切分文档 → 向量化 → 存入向量库
Document document = Document.from("...");
List<TextSegment> segments = DocumentSplitters.recursive(500, 0).split(document);

EmbeddingModel embeddingModel = OpenAiEmbeddingModel.builder()
        .apiKey(System.getenv("OPENAI_API_KEY"))
        .modelName("text-embedding-3-small")
        .build();

EmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();  // 生产换 Redis/Milvus/PGVector 等
for (TextSegment segment : segments) {
    store.add(embeddingModel.embed(segment).content(), segment);
}

// ② 在线侧：ContentRetriever 把向量库包成"检索插槽"
ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
        .embeddingStore(store)
        .embeddingModel(embeddingModel)
        .maxResults(3)
        .build();

// ③ 装配进 AiServices：问答时自动检索并注入上下文
Assistant assistant = AiServices.builder(Assistant.class)
        .chatLanguageModel(model)
        .contentRetriever(retriever)
        .build();
```

## 🤔 解决什么问题 / 为什么需要

- **记忆**：LLM 无状态，多轮对话必须由框架侧保存并注入历史——窗口记忆是"够用且简单"的默认方案（摘要/语义记忆是进阶，见 04-多轮对话章）。
- **RAG**：模型不知道你的私有知识，检索注入让回答"有据可依"——框架把"切分→向量化→检索→注入"封装成插槽，业务代码零编排。

## 🧩 核心概念

| 组件 | 作用 | Spring AI 对照 |
| ---- | ---- | ---- |
| `ChatMemory` / `MessageWindowChatMemory` | 多轮上下文窗口 | `ChatMemory` / `MessageWindowChatMemory` |
| `chatMemoryId` 方法参数 | 会话隔离 | `ChatMemory.CONVERSATION_ID` |
| `Document` / `TextSegment` | 文档与切分片段 | `Document` |
| `DocumentSplitters.recursive` | 递归切分器 | 无内置等价（用 LangChain4j 概念） |
| `EmbeddingModel` / `EmbeddingStore` | 向量化 / 存储检索 | `EmbeddingModel` / `VectorStore` |
| `EmbeddingStoreContentRetriever` | 检索插槽 | `QuestionAnswerAdvisor` |

## 💻 代码实践 / 排查要点

```java
// 完整链路：切分 → 向量化 → 入库 → 检索问答
// 离线（一次性）：
List<Document> docs = docs.stream()
        .map(d -> DocumentSplitters.recursive(500, 100).split(d))
        .flatMap(List::stream)
        .map(seg -> { store.add(embeddingModel.embed(seg).content(), seg); return seg; })
        .toList();

// 在线（每次问答）：
String answer = assistant.chat("根据知识库回答：什么是 CAG？");
```

- 检索结果太少 → 调大 `maxResults` / 换更好的 embedding 模型；太多 → 调小或加 rerank（见 RAG 章）。
- 向量库选型（Redis/Milvus/PGVector）见 [[../../03-知识库技术-RAG/15-向量库选型对比|RAG 章选型]]。
- 记忆"记不住" → 检查 `maxMessages` 是否太小、`chatMemoryId` 是否每次都不同。

## ⚠️ 踩坑与注意事项

- `InMemoryEmbeddingStore` 只适合学习 demo，**进程重启即丢**；生产用持久化向量库。
- 切分大小与重叠（500/100 之类的参数）直接影响检索质量，别照抄数字，按文档类型调（RAG 章有完整策略）。
- `chatMemoryId` 是接口方法的特殊参数，框架识别后**不会传给模型**——别在业务参数里混用。
- 记忆与检索同时装配时，Prompt 会同时包含历史与知识片段，注意总量别撑爆上下文窗口（见 01 章上下文工程）。

## 🧪 最小验证

```java
// 记忆验证：同一 chatMemoryId 能记住，不同 id 互相隔离
// RAG 验证：问一个只存在于文档中的事实，看回答是否引用文档内容
// 断点看 Prompt：AiServices 内部把检索片段注入到了哪条消息里
```

## ✅ 精通检查清单

- 能写出 `MessageWindowChatMemory` + `chatMemoryId` 的多会话示例。
- 能说清 `Document → TextSegment → EmbeddingStore → ContentRetriever` 的 RAG 组件链。
- 能对照 Spring AI 的记忆/向量组件说出两边概念映射。

## 🔗 关联笔记

- 本子区：[[01-LangChain4j核心概念]] · [[02-AiServices声明式AI服务]] · [[04-与Spring生态集成]]
- 对照：[[../SpringAI/08-ChatMemory记忆|Spring AI 记忆]] · [[../SpringAI/09-VectorStore向量存储|Spring AI 向量库]]
- 深化：[[../../03-知识库技术-RAG/_index|03-知识库技术-RAG]]

## 📚 参考资料

- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
- [LangChain4j RAG 教程](https://docs.langchain4j.dev/tutorials/rag)


