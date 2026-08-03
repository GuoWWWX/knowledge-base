---
title: Java内存模型JMM
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/并发
  - 阶段/03
created: 2026-07-04
updated: 2026-07-14
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# Java内存模型JMM

> [!abstract] 一句话总结
> JMM 定义线程之间如何通过主内存交互，核心是可见性、原子性、有序性和 happens-before。

> [!tip] 串联阅读
> 学完本页后读 [[20-Java后端/10-知识串联与专题/01-JMM到JUC并发工具的完整链路|JMM 到 JUC 并发工具的完整链路]]，把 volatile、CAS、AQS、锁、线程池和 Future 放回同一套内存语义中理解。

## 📌 这是什么

- JMM 是 Java 层面对编译器、CPU 缓存和重排序的抽象规范。
- 线程间共享变量必须通过正确同步建立 happens-before，才能保证读到期望值。
- volatile、synchronized、final、线程启动/结束、锁释放/获取都提供特定内存语义。

## 🤔 解决什么问题 / 为什么需要

- 解释为什么一个线程改了变量另一个线程可能看不到。
- 判断双重检查锁、发布对象、停止标志、单例初始化是否安全。
- 面试并发问题通常从 JMM 开始串联 volatile、锁、CAS、AQS。

## 🧩 核心概念

| 概念 | 含义 | 精通要点 |
| ---- | ---- | -------- |
| 可见性 | 一个线程写入何时对其他线程可见。 | volatile/锁释放获取可建立。 |
| 原子性 | 操作是否不可分割。 | i++ 不是原子操作。 |
| 有序性 | 编译器和 CPU 可能重排。 | 单线程语义不变不代表多线程安全。 |
| happens-before | 判断内存可见性的规则。 | 前一个操作结果对后一个可见。 |
| 安全发布 | 对象构造完成后以正确同步方式让其他线程访问。 | final 字段有额外初始化安全性。 |

## ⚙️ 工作原理 / 流程

```mermaid
flowchart LR
    A[线程A写共享变量] --> B{是否有同步关系}
    B -- volatile写/锁释放 --> C[happens-before]
    C --> D[线程B volatile读/锁获取]
    D --> E[看到A之前写入]
    B -- 无 --> F[数据竞争/结果不确定]
```

## 🔍 源码 / 原理深挖

- volatile 写具有 release 语义，volatile 读具有 acquire 语义。
- monitorenter/monitorexit 对应 synchronized 的锁获取和释放，释放先行发生于后续同一锁的获取。
- Thread.start 先行发生于被启动线程中的动作；线程中动作先行发生于其他线程 join 成功返回。

## 💻 代码实践 / 排查命令

```java
class StopFlag {
    private volatile boolean stop;

    void requestStop() { stop = true; }

    void runLoop() {
        while (!stop) {
            doWork();
        }
    }
}
```

## 🛠️ 排查 / 实践要点

- 并发 bug 难复现时先找共享可变状态，再看是否存在 happens-before。
- 对象发布给其他线程前，避免 this 逃逸，例如构造方法中启动线程或注册监听器。
- 用 jcstress 或压力循环验证并发假设，不要依赖单次本地运行。

## ⚠️ 踩坑与注意事项

- 认为 int 读写原子就一定线程安全，忽略可见性和复合操作。
- 用普通 boolean 做跨线程停止标志。
- 构造期间 this 逃逸导致其他线程看到半初始化对象。

## 🎯 面试问题

> [!question] 高频问题
> - Q: JMM 解决什么问题？
>   - A: 它定义多线程下共享变量读写的可见性、有序性和原子性规则，让 Java 程序在不同 CPU 和编译器优化下有统一语义。
> - Q: happens-before 是什么？
>   - A: 如果 A happens-before B，那么 A 的结果对 B 可见，且 A 的执行顺序在语义上先于 B。
> - Q: volatile 能保证什么？
>   - A: 保证可见性和禁止相关重排序，但不保证复合操作原子性。

## 🧭 源码入口速查

- JLS 17.4 定义 memory model 和 happens-before，面试回答应落到这些规则。
- `volatile` 字段在 class 文件中带 `ACC_VOLATILE`，JIT 负责生成平台相关屏障。
- `monitorenter/monitorexit` 字节码对应 synchronized，锁释放 happens-before 后续同一锁获取。
- `Thread#start`、`Thread#join`、`Future#get`、`CountDownLatch#await/countDown` 都能建立常见同步边界。

## 🧪 最小验证

```java
class Broken {
    boolean ready;
    int data;
    void writer() { data = 42; ready = true; }
    void reader() { if (ready && data != 42) throw new AssertionError(); }
}
```

这类代码没有同步关系，测试不失败也不能证明正确。并发假设要用 jcstress 或长时间压力循环验证，但最终仍以 happens-before 规则判断，而不是以“我本机没复现”为准。

## ✅ 精通检查清单

- 能用自己的话讲清本主题解决的问题和适用边界。
- 能指出至少 2 个源码入口或底层机制。
- 能给出一个最小代码示例或一组线上排查命令。
- 能回答本页列出的高频面试题，并补充项目中的真实取舍。

## 🔗 关联笔记

- [[_index]]
- [[03-volatile与synchronized]]
- [[04-CAS与原子类]]
- [[05-AQS原理]]
- [[20-Java后端/10-知识串联与专题/01-JMM到JUC并发工具的完整链路]]

## 📚 参考资料

- [Dev.java Tutorials](https://dev.java/learn/)
- [Oracle Java Documentation](https://docs.oracle.com/en/java/)
