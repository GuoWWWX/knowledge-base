---
title: LangChain-Python读代码笔记
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/Python
  - 阶段/07
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# LangChain-Python读代码笔记

> [!abstract] 一句话总结
> 读 LangChain Python 代码时，重点不是背 API，而是看懂模型、Prompt、Retriever、Tool 和 Chain/Graph 如何串成一个 AI 应用流程。

## 📌 这是什么

LangChain 是 Python AI 生态里常见的应用编排库，适合学习 RAG、工具调用、Agent 和多步骤 LLM 流程。

读代码时优先找这些入口：

- `ChatOpenAI` / 其他模型类：模型调用封装。
- `PromptTemplate` / `ChatPromptTemplate`：提示词结构。
- `retriever`：从向量库或搜索系统取上下文。
- `Runnable` / `chain`：把输入、Prompt、模型、解析器串起来。
- `tool` / `agent`：让模型选择并调用外部能力。

## 🧠 Java 对照

| Java AI 生态 | LangChain Python | 记忆点 |
| ------------ | ---------------- | ------ |
| Spring AI `ChatClient` | `ChatOpenAI` | 模型客户端 |
| LangChain4j `PromptTemplate` | `ChatPromptTemplate` | Prompt 参数化 |
| `EmbeddingModel` | embeddings | 文本向量化 |
| `EmbeddingStore` | vector store | 向量库存取 |
| `ContentRetriever` | retriever | 检索上下文 |
| service / pipeline | chain / runnable | 编排步骤 |

## ⚙️ 环境命令

```bash
uv init langchain-demo
cd langchain-demo
uv add langchain langchain-openai python-dotenv
uv run python main.py
```

`.env` 常见内容：

```text
OPENAI_API_KEY=sk-...
```

## 💻 最小示例

```python
from dotenv import load_dotenv
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI

load_dotenv()

prompt = ChatPromptTemplate.from_messages([
    ("system", "你是一个简洁的 AI 学习助手。"),
    ("user", "用 Java 开发者能懂的话解释：{topic}"),
])

model = ChatOpenAI(model="gpt-4.1-mini", temperature=0)
chain = prompt | model

result = chain.invoke({"topic": "RAG"})
print(result.content)
```

阅读这段代码时，把 `prompt | model` 理解成管道：输入字典 -> 渲染 Prompt -> 调模型 -> 返回消息。

## 🤖 AI 生态落点

- RAG：`loader -> splitter -> embeddings -> vectorstore -> retriever -> prompt -> model`。
- Agent：`tools -> agent -> executor`，模型根据工具描述决定调用哪个函数。
- 结构化输出：模型返回 JSON，再用 Pydantic 或 parser 校验。
- 观测：复杂链路要记录每一步输入输出，否则线上回答错了很难复盘。

## ⚠️ 踩坑与注意事项

- LangChain 版本变化较快，示例代码要和当前 `pyproject.toml` 里的版本对齐。
- 不要只看 `chain.invoke` 最后一行，要追 `prompt`、`retriever`、`parser` 分别做了什么。
- Retriever 返回内容太长会撑爆上下文，必须限制 `k`、做去重或压缩。
- Agent 工具描述太宽泛会导致乱调用，参数 schema 要写清楚。
- 生产项目不要把 LangChain 回调、trace、重试和超时留空。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: LangChain 主要解决什么？
>   - A: 它把模型调用、Prompt、检索、工具和输出解析组合成可复用流程，方便搭 RAG 和 Agent 原型。
> - Q: LangChain 和直接调用模型 SDK 的区别？
>   - A: 直接 SDK 更轻；LangChain 适合多步骤编排、检索和工具调用，但要接受抽象层带来的调试成本。
> - Q: 读一个 LangChain RAG 项目从哪里开始？
>   - A: 先找入口脚本，再顺着 `retriever`、`prompt`、`model`、`parser` 看数据流。

项目里可以用它快速验证：知识库问答、工具调用 Agent、文档摘要流水线，再把稳定流程沉淀成服务。

## ✅ 精通检查清单

- 能看懂 `prompt | model | parser` 的数据流。
- 能说出 RAG 中 loader、splitter、embedding、retriever 的位置。
- 能解释什么时候用 LangChain，什么时候直接用模型 SDK。
- 能排查 Prompt、检索结果和模型输出中的问题。

## 🔗 关联笔记

- [[01-Python速查-Java视角]]
- [[02-uv包管理与环境]]
- [[04-FastAPI起服务]]
- [[03-知识库技术-RAG/_index]]

## 📚 参考资料

- [LangChain Python Docs](https://python.langchain.com/)
- [LangChain4j Docs](https://docs.langchain4j.dev/)
