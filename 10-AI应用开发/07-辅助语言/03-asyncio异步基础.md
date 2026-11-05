---
title: asyncio异步基础
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/Python
  - 阶段/07
created: 2026-07-04
updated: 2026-10-30
status: 已完成
mastery: ⭐☆☆☆☆
---

# asyncio异步基础

> [!abstract] 一句话总结
> `asyncio` 通常在一个线程中的事件循环里进行协作式并发调度，适合并发等待网络 I/O；AI 应用里常用于并发调用模型、Embedding、检索服务和外部工具。

## 🎯 本节定位

本篇接着 [[01-Python速查-Java视角]] 中的 async/await，解释协程和事件循环；后续可结合 [[04-FastAPI起服务]] 的异步路由及 [[05-LangChain-Python读代码笔记]] 的并发调用理解。

## 📌 这是什么

一个事件循环通常在其所属线程中调度协程；协程等待网络 I/O 时，循环可以运行其他任务。是否提升吞吐量还取决于上游服务和并发限制。
Python 写法通常是：
- `async def` 声明协程函数。
- `await` 等待一个异步操作完成。
- `asyncio.gather` 并发等待多个协程。
- `asyncio.run` 在脚本入口启动事件循环。

## ⏱️ 为什么等待时能做别的事？

假设同时请求文档检索和网页搜索，两次调用都要等网络。协程 A 执行到 `await` 等响应时，事件循环可以先运行协程 B；B 也开始等待后，循环再在响应到达时恢复对应协程。若两次请求都约需 2 秒，并且上游允许并发，总耗时可能接近 2 秒而不是顺序等待的 4 秒，但实际结果还受网络和限流影响。
关键是等待的库必须支持异步。如果协程内调用同步阻塞请求，事件循环所在的线程会被占住，其他协程也不能继续运行。

## 🧠 Java 对照

| Java | Python asyncio | 记忆点 |
| ---- | -------------- | ------ |
| `CompletableFuture<T>` | coroutine / `Task` | 都表示未来完成的结果 |
| `future.get()` | `await task` | 前者阻塞线程；后者在等待时可让出事件循环 |
| `CompletableFuture.allOf` | `asyncio.gather` | 并发等待多个任务 |
| Servlet 线程池 | event loop | asyncio 主要靠事件循环调度 |
| 阻塞 HTTP Client | `httpx.AsyncClient` | 异步代码要配异步库 |

## ⚙️ 环境命令

先安装依赖并将下方示例保存为 `main.py`，再运行最后一条命令。

```bash
uv init --no-package asyncio-demo # 示例使用项目根目录的 main.py
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

async def fetch_preview(url: str) -> str:
    async with httpx.AsyncClient(timeout=10) as client:
        response = await client.get(url)
        response.raise_for_status()
        return response.text[:30]

async def main() -> None:
    urls = [
        "https://example.com",
        "https://www.python.org",
    ]
    previews = await asyncio.gather(*(fetch_preview(url) for url in urls))
    print(previews)

asyncio.run(main())
```

关键点：直接调用 `async def` 函数会返回一个协程对象，需通过 `await` 或封装为 `Task` 才会进入事件循环执行。当遇到未完成的异步 I/O 操作时，`await` 会挂起当前协程并让出执行权；纯 CPU 计算循环则始终占用当前线程，需通过进程池等方式处理。

## 🤖 AI 生态落点

- 并发生成 Embedding：一次处理多个文档 chunk，但要受限于模型 API 的 QPS。
- 并发检索：同时查向量库、关键词检索、数据库，再合并结果。
- Agent 工具调用：多个互不依赖的外部 API 可以并行调用。
- FastAPI 路由：`async def` 路由里调用异步 SDK，避免阻塞服务吞吐。

以下是 AI 应用中的流程片段，`search_vector_store` 和 `search_web` 由项目自行实现。

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
- `asyncio.gather` 默认遇到首个异常时会向调用方抛出，其余未完成任务仍会继续运行。
- 设置 `return_exceptions=True` 可将异常作为结果处理；需要在任务失败时取消同组其他任务，可使用 Python 3.11+ 的 `asyncio.TaskGroup`。
- 超时或取消时，在 `finally` 中释放资源并向上传递 `CancelledError`。
- 本地取消协程不一定终止远端模型生成，服务端仍可能继续计费。
- CPU 密集型任务应交由进程池（`ProcessPoolExecutor`）、任务队列或独立计算服务处理。

## 🎯 面试 / 项目用途

> [!question]- Python 的 `asyncio` 适合什么场景？
> 适合网络 I/O 密集场景，例如模型调用、HTTP 工具、数据库和向量库请求。

> [!question]- `asyncio` 和多线程有什么区别？
> `asyncio` 是单线程事件循环协作调度；多线程由系统调度。前者适合大量 I/O 等待，后者更像阻塞代码的并发执行。

> [!question]- RAG 服务里怎么用异步？
> 可以并发进行向量检索和关键词检索，候选结果返回后再重排；同时要控制 API 限流和超时。

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
