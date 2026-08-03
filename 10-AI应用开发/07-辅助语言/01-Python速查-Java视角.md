---
title: Python速查-Java视角
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/Python
  - 阶段/07
created: 2026-07-04
updated: 2026-08-03
status: 已完成
mastery: ⭐☆☆☆☆
---

# Python速查-Java视角

> [!abstract] 一句话总结
> 不是转 Python，而是能看懂 GitHub 上的 Python AI 项目，能用 uv 跑通 FastAPI / LangChain / RAG 示例。

## 🎯 本节定位

- **本章第 1 课，Python 子区起点**：建立 Java → Python 的语法与心智对照，后面 [[02-uv包管理与环境]]（环境）、[[04-FastAPI起服务]]（服务）都建立在本课基础上。
- **前置**：Java 基础与阶段三 RAG 概念；**延伸**：[[05-LangChain-Python读代码笔记]]。

## 🎯 学习目标

- [ ] 看懂 Python 基础语法、类型注解和项目结构
- [ ] 会用 `uv` 创建环境、安装依赖、运行脚本
- [ ] 理解 `async / await`、FastAPI 路由和 Pydantic DTO
- [ ] 能把 Python AI 代码对照到 Spring AI / LangChain4j 心智里

## 🧠 Java 到 Python 对照

| Java | Python | 记忆点 |
| ---- | ------ | ------ |
| `String name` | `name: str` | 类型写在变量后面，且通常可省略 |
| `List<String>` | `list[str]` | Python 3.9+ 推荐写法 |
| `Map<String, Object>` | `dict[str, Any]` | `Any` 来自 `typing` |
| `null` | `None` | 判断用 `is None` |
| `true / false` | `True / False` | 首字母大写 |
| `&& / || / !` | `and / or / not` | 逻辑运算符更像英文 |
| `try-catch` | `try-except` | 捕获异常用 `except` |
| `record / DTO` | `dataclass` / `BaseModel` | FastAPI 常用 Pydantic |
| `CompletableFuture` | `async def` + `await` | 异步模型不同，但目的类似 |

## 📦 环境与包管理

```bash
uv init ai-demo
cd ai-demo
uv add fastapi uvicorn pydantic openai
uv run python main.py
uv run uvicorn main:app --reload
```

| 文件 | 作用 |
| ---- | ---- |
| `pyproject.toml` | 类似 `pom.xml` / `package.json` |
| `.venv/` | 当前项目虚拟环境 |
| `main.py` | 常见入口文件 |
| `.env` | 本地模型 Key、数据库连接等 |
| `requirements.txt` | 老项目常见依赖清单 |

## ✍️ 语法速查

```python
from typing import Any

name: str = "rag-demo"
top_k: int = 5
metadata: dict[str, Any] = {"source": "doc.md"}

def build_prompt(question: str, context: str) -> str:
    return f"基于资料回答：{context}\n问题：{question}"
```

```python
docs = ["a.md", "b.pdf", "c.md"]
markdown_docs = [x for x in docs if x.endswith(".md")]

for doc in markdown_docs:
    print(doc)

if top_k <= 0:
    top_k = 1
elif top_k > 20:
    top_k = 20
```

## 🧩 Pydantic：Python 里的 DTO

```python
from pydantic import BaseModel

class ChatRequest(BaseModel):
    user_id: str
    message: str
    stream: bool = True

class ChatResponse(BaseModel):
    answer: str
    sources: list[str] = []
```

> [!tip] FastAPI 会用 `BaseModel` 自动做 JSON 解析、参数校验和接口文档生成，很像 Spring Controller + DTO + Validation 的组合。

## ⚡ async / await

```python
import httpx

async def call_llm(prompt: str) -> str:
    async with httpx.AsyncClient() as client:
        response = await client.post("/chat", json={"prompt": prompt})
        return response.json()["answer"]
```

| Python 异步 | Java 对照 |
| ----------- | --------- |
| `async def` | 声明异步方法 |
| `await` | 等待异步结果 |
| `asyncio.gather` | 并发等待多个任务 |
| `async with` | 异步资源管理 |

## 🚀 FastAPI 最小服务

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class ChatRequest(BaseModel):
    message: str

@app.post("/chat")
async def chat(req: ChatRequest):
    return {"answer": f"收到：{req.message}"}
```

运行：

```bash
uv run uvicorn main:app --reload
```

| FastAPI | Spring Boot |
| ------- | ----------- |
| `FastAPI()` | `@SpringBootApplication` |
| `@app.post("/chat")` | `@PostMapping("/chat")` |
| `BaseModel` | DTO / record |
| `uvicorn` | Web 容器 |

## 🤖 AI 项目常见阅读入口

| 关键词 | 作用 |
| ------ | ---- |
| `loader` | 文档加载 |
| `splitter` | 文本切分 |
| `embedding` | 向量化模型 |
| `vectorstore` / `retriever` | 向量库与检索 |
| `prompt` | 提示词模板 |
| `chain` / `graph` | 编排流程 |
| `tool` | Agent 可调用工具 |

RAG 代码一般就是：

```text
加载文档 -> 切分文本 -> Embedding -> 向量库入库
用户问题 -> 向量检索 -> 拼 Prompt -> LLM 生成 -> 引用来源
```

## 🧪 常用检查命令

```bash
uv run python --version
uv run python main.py
uv run pytest
uv run ruff check .
```

> [!warning] 跑 Python AI 项目前，先看 `README.md`、`pyproject.toml`、`.env.example`。很多失败不是代码问题，而是模型 Key、向量库、代理或 Python 版本没对上。

## 🔗 关联笔记

- [[02-uv包管理与环境]]
- [[03-asyncio异步基础]]
- [[04-FastAPI起服务]]
- [[05-LangChain-Python读代码笔记]]
- [[10-AI应用开发/03-知识库技术-RAG/_index]]

## 📚 参考资料

- [Python Documentation](https://docs.python.org/3/)
- [FastAPI Documentation](https://fastapi.tiangolo.com/)
- [uv Documentation](https://docs.astral.sh/uv/)
