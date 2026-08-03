---
title: 03 · 并发编程（索引）
tags: [类型/索引, 领域/Java后端, 主题/并发]
created: 2026-05-31
updated: 2026-07-04
---

# 🔀 03 · 并发编程

> [!info] 本区说明
> 线程基础、JMM、锁、JUC 工具、线程池、并发容器、并发设计模式。

## 📚 知识笔记清单
- [x] [[01-线程基础与生命周期]]
- [x] [[02-Java内存模型JMM]]
- [x] [[03-volatile与synchronized]]
- [x] [[04-CAS与原子类]]
- [x] [[05-AQS原理]]
- [x] [[06-ReentrantLock与读写锁]]
- [x] [[07-线程池原理与参数]]
- [x] [[08-并发容器-ConcurrentHashMap]]
- [x] [[09-CompletableFuture异步编程]]

## 🧭 精通路线

1. 先读 [[01-线程基础与生命周期]] 和 [[02-Java内存模型JMM]]，用线程状态、interrupt、happens-before 判断并发代码是否有可见性和发布问题。
2. 再读 [[03-volatile与synchronized]]、[[04-CAS与原子类]]、[[05-AQS原理]]、[[06-ReentrantLock与读写锁]]，把锁、CAS、队列、park/unpark 连接到 JUC 源码。
3. 最后读 [[07-线程池原理与参数]]、[[08-并发容器-ConcurrentHashMap]]、[[09-CompletableFuture异步编程]]，重点训练生产参数、线程池隔离、缓存击穿和异步异常治理。

## 🧪 验证目标

- 能用 `jstack` 区分 `BLOCKED`、`WAITING`、`TIMED_WAITING` 对应的代码位置和锁对象。
- 能解释 `ThreadPoolExecutor.execute`、`AQS.acquire/release`、`ConcurrentHashMap.putVal` 的关键分支。
- 能写出有超时、降级、专属线程池、异常汇总的 `CompletableFuture` 聚合调用。

## 🧾 本章复习

- [[99-本章总结与复习]]

## 🧩 跨模块串联

- [[20-Java后端/10-知识串联与专题/01-JMM到JUC并发工具的完整链路]]
- [[20-Java后端/10-知识串联与专题/06-线程池-CompletableFuture-MQ异步链路]]
- [[20-Java后端/10-知识串联与专题/08-线上故障排查全链路]]

## 🔗 相关
- 上级：[[20-Java后端/00-总览/学习仪表盘|Java 学习仪表盘]]
