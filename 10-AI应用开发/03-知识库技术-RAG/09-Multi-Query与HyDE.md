---
title: Multi-Query与HyDE
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/RAG
  - 阶段/03
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# Multi-Query与HyDE

> [!abstract] 一句话总结
> Multi-Query 和 HyDE 都是在检索前改造查询，让系统更容易召回答案所在的 chunk。

## 📌 这是什么

用户问题往往太短、太口语化，直接向量化可能召回不稳。Query Transformation 的目标是把“用户问法”改造成“更适合检索的表达”。

## 🧩 两种方法

| 方法 | 做法 | 适合 |
| ---- | ---- | ---- |
| Multi-Query | 让 LLM 改写出多个等价问题，多路检索后合并 | 问法模糊、同义表达多 |
| HyDE | 让 LLM 先生成一段假设性答案，再用这段答案检索 | 问题很短、缺少上下文 |

## ⚙️ Multi-Query 示例

```text
原问题：公司报销住宿费有什么限制？
改写 1：差旅住宿报销标准是什么？
改写 2：员工出差酒店费用上限是多少？
改写 3：住宿发票报销有哪些要求？
```

多路检索后合并去重，再进入 Rerank。

## ⚙️ HyDE 流程

```mermaid
flowchart LR
    A[用户问题] --> B[LLM 生成假设答案]
    B --> C[对假设答案做 Embedding]
    C --> D[向量检索]
    D --> E[真实文档候选]
```

HyDE 的假设答案不直接给用户，只作为检索向量。

## ⚠️ 踩坑与注意事项

- [ ] Query 改写会增加一次 LLM 调用成本。
- [ ] HyDE 可能把错误假设带入检索方向。
- [ ] 多路检索必须去重，否则上下文会重复。
- [ ] 最终答案必须依据真实文档，不依据假设答案。

## 🎯 面试问题

> [!question] 高频问题
> - **Q: Multi-Query 解决什么问题？**
>   - A: 解决用户问法单一导致召回不全的问题，通过多个语义等价查询扩大召回面。
> - **Q: HyDE 为什么有效？**
>   - A: 假设答案通常比原问题更接近知识库文档的表达，向量检索更容易命中相关内容。

## 🔗 关联笔记

- [[06-向量检索与TopK]]
- [[07-Rerank重排序]]
- [[20-Agentic-RAG智能体RAG]]
- [[_index]]

## 📚 参考资料

- [Precise Zero-Shot Dense Retrieval without Relevance Labels](https://arxiv.org/abs/2212.10496)
- [LangChain MultiQueryRetriever API](https://reference.langchain.com/python/langchain-classic/retrievers/multi_query/MultiQueryRetriever)
- [LlamaIndex HyDE Query Transform](https://docs.llamaindex.ai/en/stable/examples/query_transformations/HyDEQueryTransformDemo/)
