---
title: 行业场景-AI客服-AI搜索-办公助手-数据分析Agent
tags: [领域/AI应用, 类型/项目, 主题/行业场景]
created: 2026-07-05
updated: 2026-07-05
status: 已完成
mastery: ⭐☆☆☆☆
---

# 行业场景：AI 客服 / AI 搜索 / 办公助手 / 数据分析 Agent

> [!abstract] 一句话总结
> 招聘看重的不是“我会 RAG/Agent”，而是你能把技术映射到业务场景：降本、提效、减少人工、提升知识命中率。

## 场景一：AI 客服

| 模块 | 技术点 |
| ---- | ------ |
| 意图识别 | 分类、路由、置信度 |
| 知识问答 | RAG、引用溯源、无答案拒答 |
| 工单动作 | Tool Calling、权限、审批 |
| 多轮上下文 | 会话状态、摘要记忆 |
| 质量评估 | 解决率、转人工率、幻觉率 |

简历表达：

> 设计 AI 客服多轮对话系统，支持意图识别、RAG 知识问答、工单工具调用和人工转接，通过 trace 和 badcase 数据集持续优化回答质量。

## 场景二：AI 搜索

| 模块 | 技术点 |
| ---- | ------ |
| Query Rewrite | Multi-Query、HyDE |
| 召回 | BM25 + Vector Hybrid |
| 排序 | Rerank、业务权重 |
| 答案生成 | 引用溯源、证据约束 |
| 权限 | metadata filter、租户隔离 |

简历表达：

> 构建企业 AI 搜索，融合关键词检索、向量检索和 Rerank，支持权限过滤和引用溯源，降低内部知识查找成本。

## 场景三：办公助手

| 模块 | 技术点 |
| ---- | ------ |
| 文档总结 | 长上下文、分层摘要 |
| 邮件/周报 | 模板化生成、风格控制 |
| 日程/待办 | Tool Calling、MCP |
| 审批辅助 | 信息抽取、风险提示 |
| 企业集成 | 权限、审计、SSO |

注意：办公助手经常涉及隐私和内部数据，必须把 [[21-Agent权限治理与分级自治]] 放进设计里。

## 场景四：数据分析 Agent

| 模块 | 技术点 |
| ---- | ------ |
| 指标解释 | 业务术语库、RAG |
| SQL 生成 | schema linking、只读权限 |
| 图表生成 | 工具调用、代码执行沙箱 |
| 结果校验 | SQL explain、数值核对 |
| 交互追问 | 多轮状态和上下文压缩 |

高风险点：

- SQL 必须只读。
- 大查询要限流和超时。
- 生成图表前要校验字段含义。
- 结论要附数据来源和查询条件。

## 项目组合建议

面试前至少准备两个项目：

1. **企业知识库 RAG 系统**：证明你会检索、Rerank、引用、权限和评估。
2. **业务 Agent 系统**：证明你会工具调用、流程编排、审批、安全和可观测。

如果时间够，再补一个 **数据分析 Agent**，它能体现 Java 后端 + SQL + Agent 的复合能力。

## 面试讲法

> 我会先从业务闭环选场景，而不是先选框架。AI 客服关注解决率和转人工率，AI 搜索关注召回和引用可信度，办公助手关注权限和审计，数据分析 Agent 关注 SQL 安全和结果校验。不同场景对应不同的 RAG、Agent、工具和评估方案。

## 关联

- [[03-项目-企业知识库RAG系统]]
- [[04-项目-智能客服多轮对话系统]]
- [[07-项目-业务Agent系统]]
- [[09-AI应用评估数据集与回归测试]]

## 📚 参考资料

- [LangGraph 官方文档](https://docs.langchain.com/oss/python/langgraph/overview)
- [OpenAI Agents SDK](https://openai.github.io/openai-agents-python/)
- [Langfuse Documentation](https://langfuse.com/docs)
- [Dify Documentation](https://docs.dify.ai/)

