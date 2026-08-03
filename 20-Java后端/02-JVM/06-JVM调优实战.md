---
title: JVM调优实战
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/JVM
  - 阶段/02
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# JVM调优实战

> [!abstract] 一句话总结
> JVM 调优不是背参数，而是围绕业务目标，用日志、指标、dump 和压测证据逐步缩小瓶颈。

## 📌 这是什么

- 调优目标通常是吞吐、延迟、内存占用、启动速度和稳定性之间的平衡。
- 输入证据包括 GC 日志、线程栈、堆 dump、JFR、业务指标和压测曲线。
- 实战方法是一次只改一个变量，保留对照组和回滚方案。

## 🤔 解决什么问题 / 为什么需要

- 线上问题常表现为接口慢、CPU 高、内存涨、频繁 GC、线程阻塞。
- 盲目调大堆或改收集器可能掩盖问题，甚至放大停顿。
- 面试更看重排查闭环：现象、证据、假设、验证、修复、复盘。

## 🧩 核心概念

| 概念 | 含义 | 精通要点 |
| ---- | ---- | -------- |
| 吞吐 | 应用有效工作时间占比。 | 批处理可能优先。 |
| 停顿 | STW 对请求延迟的影响。 | 在线服务重点看 P99/P999。 |
| 分配率 | 单位时间创建对象量。 | 高分配率会推高 GC 压力。 |
| 存活率 | GC 后仍存活对象比例。 | 影响晋升和老年代压力。 |
| 基线 | 未改参数前的指标快照。 | 没有基线就无法证明优化有效。 |

## ⚙️ 工作原理 / 流程

```mermaid
flowchart LR
    A[明确目标] --> B[采集基线]
    B --> C[定位瓶颈]
    C --> D[提出假设]
    D --> E[单变量修改]
    E --> F[压测/灰度验证]
    F --> G[固化参数与复盘]
```

## 🔍 源码 / 原理深挖

- 统一 GC 日志 -Xlog:gc* 是 JDK 9+ 推荐入口。
- JFR 适合低开销采集分配、锁、线程、IO、方法采样等事件。
- jcmd 比很多旧工具更统一，例如 GC.heap_dump、JFR.start、VM.native_memory。

## 💻 代码实践 / 排查命令

```java
java -Xms4g -Xmx4g \
  -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
  -Xlog:gc*,safepoint:file=/var/log/app/gc.log:time,uptime,level,tags \
  -jar app.jar
```

## 🛠️ 排查 / 实践要点

- CPU 高：top 找线程，printf 转 16 进制 tid，jstack 定位栈，再用 async-profiler/JFR 验证热点。
- GC 频繁：先看分配率和对象存活，再决定调堆、优化对象创建或改业务缓存。
- 内存不释放：heap dump 看 dominator tree 和 GC Roots，不要先重启了事。

## ⚠️ 踩坑与注意事项

- 没有业务指标，只看 JVM 指标。
- 一次改多个参数，无法判断哪项生效。
- 把调优当永久方案，忽略代码内存泄漏或下游慢调用。

## 🎯 面试问题

> [!question] 高频问题
> - Q: 你怎么做 JVM 调优？
>   - A: 先明确目标和基线，采集 GC 日志、线程栈、JFR、业务延迟等证据，定位瓶颈后单变量调整，通过压测或灰度验证，最后固化和复盘。
> - Q: 什么时候调大堆，什么时候调小堆？
>   - A: 堆太小会频繁 GC，太大可能增加停顿和内存成本；要结合分配率、存活率、停顿目标和容器限制决定。
> - Q: 线上能不能直接 jmap dump？
>   - A: 大堆 dump 可能 STW 且产生大文件，要评估影响；优先在从节点、低峰期或用 JFR/采样工具，必要时做好磁盘和回滚。

## 🧭 源码入口速查

- 统一启动基线：固定 `-Xms/-Xmx`，打开 `-Xlog:gc*,safepoint`，保留 JVM flags 和容器 limit。
- JFR 入口：`jcmd <pid> JFR.start name=profile settings=profile duration=120s filename=app.jfr`。
- NMT 入口：启动加 `-XX:NativeMemoryTracking=summary`，运行时用 `jcmd <pid> VM.native_memory summary.diff`。
- CPU 火焰图：优先 async-profiler/JFR，jstack 适合看阻塞和粗粒度热点，不适合替代采样 profiler。

## 🧪 最小验证

```bash
jcmd <pid> VM.flags
jcmd <pid> JFR.start name=tune settings=profile duration=120s filename=/tmp/tune.jfr
jstat -gcutil <pid> 1000 30
jcmd <pid> Thread.print > /tmp/thread.txt
```

调优记录必须写成表：现象、基线、假设、改动、验证命令、结果、回滚方式。例如“P99 800ms，G1 Evacuation 最高 350ms，假设年轻代过小导致晋升压力，调整 Xmx 无效；JFR 显示分配热点是 JSON 大对象，修代码后 P99 回到 180ms”。

## ✅ 精通检查清单

- 能用自己的话讲清本主题解决的问题和适用边界。
- 能指出至少 2 个源码入口或底层机制。
- 能给出一个最小代码示例或一组线上排查命令。
- 能回答本页列出的高频面试题，并补充项目中的真实取舍。

## 🔗 关联笔记

- [[_index]]

## 📚 参考资料

- [JVM Specification](https://docs.oracle.com/javase/specs/jvms/se21/html/)
- [Oracle Java Documentation](https://docs.oracle.com/en/java/)
