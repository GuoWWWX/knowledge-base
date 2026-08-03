---
title: RAG评估-Ragas
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

# RAG评估-Ragas

> [!abstract] 一句话总结
> Ragas 是常用的 RAG 评估框架，用指标拆开看“检索是否找对、答案是否忠实、回答是否有用”。

## 📌 这是什么

RAG 不能只靠主观感觉验收。评估要把链路拆开：检索结果是否覆盖答案，生成是否基于上下文，最终回答是否解决问题。

## 🧩 核心指标

| 指标 | 关注点 |
| ---- | ---- |
| Context Precision | 召回上下文里相关内容排得是否靠前 |
| Context Recall | 标准答案所需信息是否被召回 |
| Faithfulness | 答案是否忠实于上下文 |
| Answer Relevancy | 答案是否回应了用户问题 |

## ⚙️ 评估流程

```mermaid
flowchart LR
    A[测试问题集] --> B[RAG 系统输出]
    B --> C[收集 answer + contexts]
    C --> D[Ragas 计算指标]
    D --> E[定位问题]
```

## 🎯 测试集怎么建

```text
1. 从真实文档中抽 30~100 个高频问题。
2. 每题标注标准答案和来源片段。
3. 覆盖事实问答、列表总结、多跳问题、拒答问题。
4. 每次调参后固定测试集回归。
```

## 🔍 如何定位问题

| 现象 | 优先排查 |
| ---- | ---- |
| Context Recall 低 | 文档解析、切分、Embedding、TopK |
| Context Precision 低 | Rerank、混合检索融合、去重 |
| Faithfulness 低 | Prompt 约束、引用格式、模型幻觉 |
| Answer Relevancy 低 | Query 理解、问题改写、生成指令 |

## ⚠️ 踩坑与注意事项

- [ ] LLM-as-judge 有波动，关键问题仍要人工抽检。
- [ ] 只看总分没意义，要按指标定位到召回或生成。
- [ ] 测试集太干净会高估生产效果。
- [ ] 评估数据不要和调参样本完全重合。

## 🎯 面试问题

> [!question] 高频问题
> - **Q: RAG 怎么评估？**
>   - A: 拆成检索指标和生成指标，看 context recall、context precision、faithfulness、answer relevancy。
> - **Q: Faithfulness 低说明什么？**
>   - A: 答案中有内容没有被上下文支持，可能是幻觉或 Prompt 约束不足。

## 🔗 关联笔记

- [[01-RAG基础与全链路]]
- [[06-向量检索与TopK]]
- [[07-Rerank重排序]]
- [[11-引用溯源]]
- [[_index]]

## 📚 参考资料

- [Ragas Documentation](https://docs.ragas.io/)
- [RAGAS: Automated Evaluation of Retrieval Augmented Generation](https://arxiv.org/abs/2309.15217)
- [TruLens RAG Triad](https://www.trulens.org/getting_started/core_concepts/rag_triad/)
