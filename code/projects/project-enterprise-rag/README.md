# Project Enterprise RAG (企业级 RAG 知识库落地工程)

[![Java](https://img.shields.io/badge/JDK-21-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)

## 📖 项目简介

本项目为《10-AI应用开发/08-项目实战》中的企业级 RAG 知识库实战工程。核心聚焦解决企业非结构化制度与知识检索中的**多租户数据隔离**、**来源页码精准引用溯源**、**防幻觉受控严格 Prompt 约束**与**置信度门限安全防御**。

## 🏗️ 核心架构与设计

```mermaid
flowchart TD
    A[文档上传 / 分块录入] --> B[KnowledgeStoreService]
    B --> C[(内存向量与词元特征库)]
    D[用户问答请求 /api/rag/ask] --> E[租户隔离过滤]
    E --> F[Hybrid Search 检索: 语义向量余弦 + 关键词覆盖]
    F --> G{置信度 >= 门限?}
    G -- 否 (低于门限 / 无关问题) --> H[受控防御: 未检索到相关企业规范]
    G -- 是 (有效召回) --> I[组装严格防幻觉 Prompt]
    I --> J[精准答案合成 + [出处: 文档名, P.页码] 溯源]
    J --> K[返回 RagAskResponse]
```

## 🚀 核心组件说明

- **`DocumentChunk`**：文档分块模型，包含 `id`、`docName`、`pageNumber`、`content`、`embedding`、`tenantId`、`score`。
- **`KnowledgeStoreService`**：
  - 支持多租户硬隔离存储（`tenantStore` 并发哈希表）；
  - 特征向量抽取（`computeEmbedding`）与 L2 范数归一化；
  - 混合检索（Hybrid Search）：融合向量余弦相似度与词元覆盖率；
  - 支持 Top-K 动态截取与相似度降序排列。
- **`RagQueryService`**：
  - 置信度门限拦截防御（未达到阈值直接受控回复“未检索到相关企业规范”）；
  - 严格 Prompt 构建（System Prompt 防幻觉规则与参考知识上下文注入）；
  - 引用来源注入与结构化溯源元数据装配。
- **`RagController`**：
  - `POST /api/rag/documents`：上传/录入企业制度文本并自动切分入库；
  - `POST /api/rag/ask`：问答交互，输出包含回答、置信度与溯源出处。

## 🧪 自动化测试验证

集成测试类 `EnterpriseRagApplicationTest` 覆盖以下场景：
1. **准确命中与引用**：录入差旅报销制度后，提问“普通员工北京住宿上限是多少？”，验证精准返回 550 元及第 3 页出处引用；
2. **无关问题门限兜底**：提问“公司食堂星期天中午开门供餐吗？”，验证触发置信度门限拦截并返回受控防御话术；
3. **多租户隔离防越权**：未授权租户提问相同问题，验证数据完全隔离无法召回。
