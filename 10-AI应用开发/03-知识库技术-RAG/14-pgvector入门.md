---
title: pgvector入门
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

# pgvector入门

> [!abstract] 一句话总结
> pgvector 是 PostgreSQL 的向量扩展，让业务数据、权限字段、文档 metadata 和向量检索放在同一个数据库里。

## 📌 这是什么

对 Java 后端来说，pgvector 的优势是工程简单：不用额外维护一套向量数据库，直接在 PostgreSQL 里存 chunk、metadata 和 embedding。

## 🧩 基本表结构

```sql
create table kb_chunk (
  id bigserial primary key,
  document_id text not null,
  content text not null,
  embedding vector(1536),
  tenant_id text not null,
  source text,
  page int
);
```

## ⚙️ 查询思路

```sql
select id, content, source, page
from kb_chunk
where tenant_id = :tenantId
order by embedding <=> :queryEmbedding
limit 10;
```

`<=>` 常用于 cosine distance，具体距离操作符取决于索引和配置。

## 🎯 适用场景

- 已经在用 PostgreSQL。
- 数据量在小中规模，先快速上线 RAG。
- 权限过滤、事务、业务表 Join 很重要。
- 团队不想额外维护 Milvus / Elasticsearch 集群。

## ⚠️ 踩坑与注意事项

- [ ] 大规模高 QPS 下要压测，不要默认 PG 一定扛得住。
- [ ] 建索引前确认距离类型和向量维度。
- [ ] metadata 过滤和向量索引可能互相影响查询计划。
- [ ] 删除和更新文档时要同步清理 chunk。

## 🎯 面试问题

> [!question] 高频问题
> - **Q: pgvector 的优势是什么？**
>   - A: 架构简单，和业务数据、权限控制、事务能力复用 PostgreSQL。
> - **Q: pgvector 的短板是什么？**
>   - A: 超大规模向量、高并发和复杂向量索引能力不如专用向量数据库。

## 🔗 关联笔记

- [[13-Milvus入门]]
- [[15-向量库选型对比]]
- [[08-混合检索-Hybrid-Search]]
- [[_index]]

## 📚 参考资料

- [pgvector GitHub](https://github.com/pgvector/pgvector)
- [Supabase Vector Documentation](https://supabase.com/docs/guides/ai)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
