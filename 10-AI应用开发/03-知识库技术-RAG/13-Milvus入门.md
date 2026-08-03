---
title: Milvus入门
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/向量数据库
  - 阶段/03
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# Milvus入门

> [!abstract] 一句话总结
> Milvus 是专门为大规模向量检索设计的向量数据库，适合百万到亿级向量、独立向量服务和高并发 RAG。

## 🎯 本节定位

- **本章第 13 课，向量数据库（一）：专用向量库**。前置 [[05-Embedding模型选型]]、[[06-向量检索与TopK]]（向量与检索参数）；本篇讲 Milvus 的核心概念与表结构。
- **延伸阅读（本课不展开）**：与 pgvector 的对比见 [[14-pgvector入门]]，最终选型见 [[15-向量库选型对比]]。

## 📌 这是什么

Milvus / Zilliz 生态是向量数据库方向的主流选择之一。它把向量、标量字段、索引、集合管理、过滤查询组合起来，给 RAG 提供高性能召回能力。

## 🧩 核心概念

| 概念 | 说明 |
| ---- | ---- |
| Collection | 类似表，存一类向量数据 |
| Field | 字段，包含主键、向量、metadata |
| Index | 向量索引，如 HNSW、IVF |
| Partition | 分区，按租户或业务拆数据 |
| Search | 向量相似度查询 |

## ⚙️ RAG 表结构

```text
id: string
document_id: string
chunk_id: string
content: string
embedding: float_vector
source: string
page: int
tenant_id: string
```

## 🎯 适用场景

- 数据量大，PostgreSQL 内置向量能力不够。
- 需要独立扩容向量检索服务。
- 向量检索 QPS 高，对延迟敏感。
- 需要更丰富的向量索引和集合管理。

## ⚠️ 踩坑与注意事项

- [ ] 向量维度必须和 Embedding 模型一致。
- [ ] 索引参数会影响召回率和延迟。
- [ ] metadata 过滤字段要提前设计。
- [ ] 生产环境要关注备份、压测和版本升级。

## 🎯 面试问题

> [!question] 高频问题
> - **Q: 什么时候选 Milvus？**
>   - A: 向量规模大、QPS 高、需要专用向量服务和丰富索引能力时。
> - **Q: Milvus 和 pgvector 最大区别？**
>   - A: Milvus 是专用向量数据库，pgvector 是 PostgreSQL 扩展，后者更适合已有 PG 业务的小中规模场景。

## 🔗 关联笔记

- [[06-向量检索与TopK]]
- [[05-Embedding模型选型]]
- [[14-pgvector入门]]
- [[15-向量库选型对比]]
- [[_index]]

## 📚 参考资料

- [Milvus Documentation](https://milvus.io/docs)
- [Milvus: Vector Index](https://milvus.io/docs/index.md)
- [PyMilvus Documentation](https://milvus.io/docs/pymilvus.md)
