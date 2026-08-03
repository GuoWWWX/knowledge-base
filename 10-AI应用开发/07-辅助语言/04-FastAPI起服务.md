---
title: FastAPI起服务
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

# FastAPI起服务

> [!abstract] 一句话总结
> FastAPI 是 Python 里最常见的 AI 后端接口框架，用它可以把模型调用、RAG 检索、Embedding 入库封装成 HTTP API。

## 🎯 本节定位

- **本章第 4 课**：前置 [[01-Python速查-Java视角]]、[[02-uv包管理与环境]]，本篇把 Python AI 能力封装成 HTTP 服务（对应 Java 的 Spring Boot）。
- **延伸**：[[05-LangChain-Python读代码笔记]] 讲如何读基于它的项目。

## 📌 这是什么

FastAPI 对 Java 开发者最友好的理解方式是：`@RestController` + DTO 校验 + Swagger 文档。

一个最小服务包含：

- `FastAPI()`：应用对象。
- `@app.get` / `@app.post`：路由。
- `pydantic.BaseModel`：请求和响应 DTO。
- `uvicorn`：本地 ASGI 服务容器。

## 🧠 Java 对照

| Spring Boot | FastAPI | 记忆点 |
| ----------- | ------- | ------ |
| `@SpringBootApplication` | `app = FastAPI()` | 应用入口 |
| `@RestController` | 路由函数 | 函数就是接口处理器 |
| `@PostMapping` | `@app.post` | HTTP 方法和路径 |
| DTO / record | `BaseModel` | JSON 解析和校验 |
| Bean Validation | Pydantic 字段约束 | 参数错误自动返回 422 |
| Swagger UI | `/docs` | 自动生成接口文档 |

## ⚙️ 环境命令

```bash
uv init fastapi-ai-demo
cd fastapi-ai-demo
uv add fastapi uvicorn pydantic
uv run uvicorn main:app --reload
```

访问：

```text
http://127.0.0.1:8000/docs
```

## 💻 最小示例

`main.py`：

```python
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI()

class ChatRequest(BaseModel):
    message: str
    stream: bool = False

class ChatResponse(BaseModel):
    answer: str

@app.post("/chat", response_model=ChatResponse)
async def chat(req: ChatRequest) -> ChatResponse:
    return ChatResponse(answer=f"收到：{req.message}")
```

测试：

```bash
curl -X POST http://127.0.0.1:8000/chat ^
  -H "Content-Type: application/json" ^
  -d "{\"message\":\"解释 RAG\"}"
```

## 🤖 AI 生态落点

- `/chat`：封装 LLM 对话接口，前端或 Java 服务都可以调用。
- `/embedding`：接收文本，返回向量或写入向量库。
- `/rag/query`：查询向量库，把检索结果和问题拼成 Prompt。
- `/health`：给部署平台或网关做健康检查。
- `/docs`：给前端、Java 后端同事调试接口。

常见项目结构：

```text
app/
  main.py
  routers/chat.py
  schemas.py
  services/llm.py
  services/retriever.py
```

## ⚠️ 踩坑与注意事项

- 路由文件拆分后要记得 `app.include_router(...)`，否则接口不会注册。
- `async def` 路由里不要调用同步慢操作；同步 SDK 会阻塞事件循环。
- Pydantic 默认会校验请求体，422 往往是字段名或类型不对。
- 生产环境不要只用 `--reload`，它是开发热重载参数。
- 模型 API Key 不要写死在代码里，放 `.env` 或部署平台环境变量。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: FastAPI 相比 Flask 的优势是什么？
>   - A: 类型注解、Pydantic 校验、自动 OpenAPI 文档和异步支持更适合接口型 AI 服务。
> - Q: FastAPI 怎么和 Java 主系统协作？
>   - A: Java 保持主业务和权限，FastAPI 承载 Python AI 能力，通过 HTTP 或 gRPC（另一种远程调用协议）互通。
> - Q: 线上 Chat 接口要补哪些工程能力？
>   - A: 超时、限流、鉴权、日志 trace、请求体大小限制、模型错误映射和健康检查。

项目里可以用 FastAPI 快速交付：RAG 原型服务、Embedding 入库接口、模型网关适配层。

## ✅ 精通检查清单

- 能写一个带 `BaseModel` 的 POST 接口。
- 能用 `uvicorn main:app --reload` 启动并访问 `/docs`。
- 能解释 422、500、超时分别从哪里排查。
- 能说清 FastAPI 在 Java + AI 架构里的边界。

## 🔗 关联笔记

- [[01-Python速查-Java视角]]
- [[02-uv包管理与环境]]
- [[03-asyncio异步基础]]

## 📚 参考资料

- [FastAPI Docs](https://fastapi.tiangolo.com/)
- [Pydantic Docs](https://docs.pydantic.dev/)
