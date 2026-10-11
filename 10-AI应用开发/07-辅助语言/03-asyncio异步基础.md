---
title: asyncio异步基础
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/Python
  - 阶段/07
created: 2026-07-04
updated: 2026-09-28
status: 已完成
mastery: ⭐☆☆☆☆
---

# asyncio异步基础

> [!abstract] 一句话总结
> `asyncio` 是 Python 的单线程协作式并发模型，适合并发等待网络 I/O；AI 应用里常用于并发调用模型、Embedding、检索服务和外部工具。

## 🎯 本节定位

- **本章第 3 课**：把 [[01-Python速查-Java视角]] 里的 async/await 讲透，建立事件循环心智。
- **延伸**：[[04-FastAPI起服务]] 的 async 路由、[[05-LangChain-Python读代码笔记]] 的并发调用都依赖它。

## 📌 这是什么

`asyncio` 的核心机制是在单线程内通过事件循环复用网络 I/O 等待时间，从而提升并发吞吐量。

Python 写法通常是：

- `async def` 声明协程函数。
- `await` 等待一个异步操作完成。
- `asyncio.gather` 并发等待多个协程。
- `asyncio.run` 在脚本入口启动事件循环。

## 🧠 Java 对照

| Java | Python asyncio | 记忆点 |
| ---- | -------------- | ------ |
| `CompletableFuture<T>` | coroutine / `Task` | 都表示未来完成的结果 |
| `future.get()` | `await task` | 前者阻塞线程；后者在等待时可让出事件循环 |
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
        response.raise_for_status()
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

关键点：直接调用 `async def` 函数会返回一个协程对象，需通过 `await` 或封装为 `Task` 才会进入事件循环执行。当遇到未完成的异步 I/O 操作时，`await` 会挂起当前协程并让出执行权；纯 CPU 计算循环则始终占用当前线程，需通过进程池等方式处理。

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

- `async def` 里调用同步阻塞库（如 `requests.get`）会阻塞整个事件循环，应改用 `httpx.AsyncClient` 等异步客户端。
- Jupyter、FastAPI 内部已经运行着事件循环，在协程内直接 `await` 即可，无需再次调用 `asyncio.run(...)`。
- 调用模型或 Embedding API 时需配合 `asyncio.Semaphore` 限制最大并发数，避免触发上游限流。
- `asyncio.gather` 在默认模式下遇到首个异常即向调用方抛出，其余未完成任务仍会继续运行；设置 `return_exceptions=True` 可将异常作为结果返回以便逐项处理。若需在单个任务失败时自动取消同组其余任务，推荐使用 Python 3.11+ 的 `asyncio.TaskGroup`。
- 处理超时或任务取消时，应在 `finally` 块中释放资源并向上传递 `CancelledError`。注意客户端本地取消协程仅中断本地等待，远端模型服务可能仍在继续生成并计费。
- CPU 密集型任务应交由进程池（`ProcessPoolExecutor`）、任务队列或独立计算服务处理。

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
