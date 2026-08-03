---
title: 从类加载到对象回收与 JVM 排障
tags:
  - 领域/Java后端
  - 类型/专题
  - 主题/知识串联
  - 主题/JVM
  - 阶段/10
created: 2026-07-14
updated: 2026-07-14
status: 已完成
mastery: ⭐☆☆☆☆
---

# 从类加载到对象回收与 JVM 排障

> [!abstract] 一句话总结
> JVM 问题要沿着“类如何进入、对象如何分配、引用如何存活、垃圾如何回收、证据如何采集”分析，而不是看到内存高就直接调大堆。

## 对象生命周期主线

```mermaid
flowchart LR
    A["类加载与链接"] --> B["类初始化"]
    B --> C["对象分配"]
    C --> D["构造与安全发布"]
    D --> E["业务持有与逃逸"]
    E --> F["可达性分析"]
    F --> G["GC 回收或晋升"]
    G --> H["指标 Dump JFR 分析"]
```

## 知识连接

| 阶段 | 单点知识 | 串联时要看什么 |
| --- | --- | --- |
| 类进入 JVM | [[20-Java后端/02-JVM/05-类加载机制与双亲委派]] | ClassLoader 边界、重复加载、元空间和静态初始化 |
| 对象产生 | [[20-Java后端/02-JVM/02-对象创建与内存布局]] | TLAB、对象头、字段布局、逃逸与分配速率 |
| 内存承载 | [[20-Java后端/02-JVM/01-JVM内存区域]] | 堆、栈、元空间、直接内存分别对应什么故障 |
| 垃圾识别 | [[20-Java后端/02-JVM/03-垃圾回收算法]] | GC Roots、可达性、引用类型和晋升 |
| 收集器执行 | [[20-Java后端/02-JVM/04-垃圾收集器-G1-ZGC]] | 延迟目标、并发阶段、暂停和内存余量 |
| 故障定位 | [[20-Java后端/02-JVM/07-OOM问题排查]]、[[20-Java后端/02-JVM/08-常用工具-jstat-jmap-jstack-Arthas]] | 先保留现场，再分类验证，不凭单一指标猜根因 |

## JMM 在对象生命周期中的位置

对象构造完成不等于其他线程一定看到完整状态。若对象通过普通共享字段发布且没有同步关系，另一个线程可能观察到不安全状态。安全发布可通过不可变对象、volatile 引用、锁、线程安全容器或任务提交边界建立，详见 [[01-JMM到JUC并发工具的完整链路]]。

## 从现象到内存区域

| 现象 | 优先怀疑 | 第一批证据 |
| --- | --- | --- |
| Full GC 频繁且老年代持续增长 | 堆内存泄漏或缓存无界 | GC 日志、类直方图、堆 Dump |
| `Metaspace` OOM | 类加载器泄漏、动态生成类过多 | `VM.classloader_stats`、类加载统计 |
| `Direct buffer memory` OOM | NIO/Netty 直接内存未释放或上限不足 | Native Memory Tracking、直接内存指标 |
| 线程数持续增长 | 线程池失控或线程泄漏 | `jcmd Thread.print`、进程线程数 |
| CPU 高但 GC 正常 | 热循环、锁自旋、序列化或业务计算 | `jcmd`、JFR、async-profiler CPU 火焰图 |
| 停顿高但堆未满 | 分配速率、Humongous 对象、系统资源争用 | GC phase、分配采样、容器 CPU 配额 |

## 标准排障流程

1. 明确时间窗口、影响范围和最近变更。
2. 保存 GC 日志、线程 Dump、JFR、类直方图；OOM 时保留堆 Dump。
3. 先分类 CPU、堆、元空间、直接内存、线程还是外部依赖。
4. 用时间线关联流量、分配速率、GC、线程和接口延迟。
5. 找到持有链或热点栈后再改代码或参数。
6. 用相同流量模型回归，确认根因指标和用户指标同时恢复。

## 参数调优的边界

- 调大堆能延后 OOM，但不能修复对象被错误持有。
- 降低暂停目标可能增加并发 GC CPU 消耗，必须同时看吞吐和延迟。
- 容器内存限制不仅包含 Java 堆，还包含元空间、线程栈、代码缓存和直接内存。
- 先减少无效分配、无界缓存和超大对象，再讨论收集器参数。

## 面试讲法

用“现象 → 内存区域 → 证据 → 根因 → 修复 → 回归”讲一次案例。只有参数没有证据不算排障；只有堆 Dump 没有时间线，也可能错过流量或下游抖动。

## 关联笔记

- [[20-Java后端/02-JVM/_index|JVM 索引]]
- [[20-Java后端/09-项目实战/05-项目-JVM故障排查演练]]
- [[08-线上故障排查全链路]]

## 参考资料

- [Java Virtual Machine Specification](https://docs.oracle.com/javase/specs/jvms/se21/html/)
- [JDK Troubleshooting Guide](https://docs.oracle.com/en/java/javase/21/troubleshoot/)
- [Java Flight Recorder](https://docs.oracle.com/en/java/javase/21/jfapi/)

