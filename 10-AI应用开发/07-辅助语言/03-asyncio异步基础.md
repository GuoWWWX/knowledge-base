---
title: asyncio异步基础
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

# asyncio异步基础

> [!abstract] 一句话总结
> `asyncio` 是 Python 的单线程协作式并发模型，适合并发等待网络 I/O；AI 应用里常用于并发调用模型、Embedding、检索服务和外部工具。

## 📌 这是什么

`asyncio` 的核心不是“让 CPU 算得更快”，而是在一个线程里把大量等待网络返回的时间利用起来。

Python 写法通常是：

- `async def` 声明协程函数。
- `await` 等待一个异步操作完成。
- `asyncio.gather` 并发等待多个协程。
- `asyncio.run` 在脚本入口启动事件循环。

## 🧠 Java 对照

| Java | Python asyncio | 记忆点 |
| ---- | -------------- | ------ |
| `CompletableFuture<T>` | coroutine / `Task` | 都表示未来完成的结果 |
| `future.get()` | `await task` | 等待结果 |
| `CompletableFuture.allOf` | `asyncio.gather` | 并发等待多个任务 |
| Servlet 线程池 | event loop | asyncio 主要靠事件循环调度 |
| 阻塞 HTTP Client | `httpx.AsyncClient` | 异步代码要配异步库 |

## ⚙️ 环境命令

```bash
uv init asyncio-demo
cd asyncio-demo
uv add httpx
uv run python main.py
```

如果放进 FastAPI：

```bash
uv add fastapi uvicorn httpx
uv run uvicorn main:app --reload
```

## 💻 最小示例

```python
import asyncio
import httpx

async def fetch_title(url: str) -> str:
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.get(url)
        return response.text[:30]

async def main() -> None:
    urls = [
        "https://example.com",
        "https://www.python.org",
    ]
    titles = await asyncio.gather(*(fetch_title(url) for url in urls))
    print(titles)

asyncio.run(main())
```

关键点：只有 `await client.get(...)` 这种 I/O 等待可以让出执行权；普通 CPU 循环不会自动并发。

## 🤖 AI 生态落点

- 并发生成 Embedding：一次处理多个文档 chunk，但要受限于模型 API 的 QPS。
- 并发检索：同时查向量库、关键词检索、数据库，再合并结果。
- Agent 工具调用：多个互不依赖的外部 API 可以并行调用。
- FastAPI 路由：`async def` 路由里调用异步 SDK，避免阻塞服务吞吐。

```python
async def answer(question: str) -> dict[str, object]:
    docs_task = search_vector_store(question)
    web_task = search_web(question)
    docs, web = await asyncio.gather(docs_task, web_task)
    return {"docs": docs, "web": web}
```

## ⚠️ 踩坑与注意事项

- `async def` 里调用阻塞库不会变快，例如同步 `requests.get` 会卡住事件循环。
- Jupyter、FastAPI 已经有事件循环，内部不要随手再写 `asyncio.run(...)`。
- 并发不是无限开；调用模型或 Embedding API 时要加信号量限制并发数。
- 异常会从 `await` 处抛出，`gather` 中一个任务失败会影响整体结果，必要时用 `return_exceptions=True`。
- CPU 密集任务应考虑进程池、任务队列或专门服务，不要硬塞进 asyncio。

## 🎯 面试 / 项目用途

> [!question] 高频问题
> - Q: Python 的 `asyncio` 适合什么场景？
>   - A: 适合网络 I/O 密集场景，例如模型调用、HTTP 工具、数据库和向量库请求。
> - Q: `asyncio` 和多线程有什么区别？
>   - A: `asyncio` 是单线程事件循环协作调度；多线程由系统调度。前者适合大量 I/O 等待，后者更像阻塞代码的并发执行。
> - Q: RAG 服务里怎么用异步？
>   - A: 可以并发做检索、重排、模型调用前置准备，但要控制 API 限流和超时。

项目里可以用它优化：批量 Embedding 入库、并发工具调用、FastAPI Chat 接口吞吐。

## ✅ 精通检查清单

- 能写出 `async def`、`await`、`asyncio.gather` 的最小示例。
- 能说明异步并不适合 CPU 密集任务。
- 能在 FastAPI 中选择异步 HTTP 客户端。
- 能给模型 API 并发调用加超时和限流。

## 🔗 关联笔记

- [[01-Python速查-Java视角]]
- [[04-FastAPI起服务]]
- [[05-LangChain-Python读代码笔记]]

## 📚 参考资料

- [asyncio Docs](https://docs.python.org/3/library/asyncio.html)
- [httpx Async Support](https://www.python-httpx.org/async/)
