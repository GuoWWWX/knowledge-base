---
title: IO与NIO
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Java核心
  - 阶段/01
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# IO与NIO

> [!abstract] 一句话总结
> Java IO 面向流，NIO 面向 Buffer、Channel、Selector，核心差异是数据移动方式和并发连接处理模型。

## 📌 这是什么

- 传统 IO 以 InputStream/OutputStream、Reader/Writer 为核心，适合同步阻塞读写。
- NIO 以 Buffer、Channel、Selector 为核心，可用单线程管理多个非阻塞通道。
- 文件、网络、编码、缓冲、零拷贝和文件描述符泄漏是实践重点。

## 🤔 解决什么问题 / 为什么需要

- 高并发网络服务不能简单用一个连接一个线程无限扩展。
- 文件处理要关注缓冲区大小、字符集、资源关闭和大文件内存占用。
- 理解 NIO 是理解 Netty、Kafka、RocketMQ 网络层的前置知识。

## 🧩 核心概念

| 概念 | 含义 | 精通要点 |
| ---- | ---- | -------- |
| Stream | 单向字节/字符流。 | API 简单，但阻塞模型下线程成本高。 |
| Channel | 可双向读写的通道。 | FileChannel、SocketChannel 是常见实现。 |
| Buffer | NIO 数据容器，有 position/limit/capacity。 | flip/clear/compact 是高频易错点。 |
| Selector | 多路复用器，监听多个 Channel 的事件。 | 底层对应 epoll/kqueue/select 等机制。 |

## ⚙️ 工作原理 / 流程

```mermaid
flowchart LR
    A[SocketChannel] --> B[注册Selector]
    B --> C[select等待事件]
    C --> D[SelectionKey]
    D --> E[read到Buffer]
    E --> F[业务解码处理]
    F --> G[write响应]
```

## 🔍 源码 / 原理深挖

- FileInputStream.read 最终会进入本地方法读取文件描述符。
- FileChannel.transferTo/transferFrom 可利用操作系统零拷贝能力减少用户态拷贝。
- Selector 维护已注册 Channel 和就绪事件，Netty Reactor 模型建立在类似机制上。

## 💻 代码实践 / 排查命令

```java
ByteBuffer buffer = ByteBuffer.allocate(8192);
try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
    while (channel.read(buffer) != -1) {
        buffer.flip();
        while (buffer.hasRemaining()) {
            buffer.get();
        }
        buffer.clear();
    }
}
```

## 🛠️ 排查 / 实践要点

- 文件句柄泄漏：Linux 看 lsof -p <pid>，Java 侧检查流是否 try-with-resources。
- 乱码问题先统一 Charset，不要依赖平台默认编码。
- 网络线程阻塞时用 jstack 看线程卡在 read/write/connect 还是业务处理。

## ⚠️ 踩坑与注意事项

- readAllBytes 直接读大文件导致内存暴涨。
- ByteBuffer 写完忘记 flip，读完忘记 clear/compact。
- 以为 NIO 一定更快；低并发简单文件读写中传统 IO 更直接。

## 🎯 面试问题

> [!question] 高频问题
> - Q: BIO、NIO、AIO 区别？
>   - A: BIO 是同步阻塞，一个连接常对应一个线程；NIO 是同步非阻塞加多路复用；AIO 是异步 IO，完成后回调。
> - Q: Buffer 的 flip 做什么？
>   - A: 把 limit 设为当前 position，再把 position 置 0，从写模式切换到读模式。
> - Q: 零拷贝是什么？
>   - A: 减少数据在内核态和用户态之间的拷贝次数，例如 FileChannel.transferTo 可让文件数据更直接地进入 socket。

## 🧭 源码入口速查

- `FileInputStream#readBytes`、`SocketInputStream#socketRead0`：传统 IO 最终进入 native read。
- `java.nio.Buffer#flip/clear/compact`：position、limit、capacity 状态机的核心方法。
- `Selector#select`、`SelectionKey#readyOps`：多路复用事件循环入口。
- `FileChannel#transferTo/transferFrom`：文件到 socket 或 channel 间零拷贝常用入口。
- Netty 可顺着 `NioEventLoop#run`、`AbstractNioByteChannel.NioByteUnsafe#read` 理解 Reactor。

## 🧪 最小验证

```java
ByteBuffer b = ByteBuffer.allocate(4);
b.put((byte) 1).put((byte) 2);
System.out.println(b.position() + "/" + b.limit()); // 2/4
b.flip();
System.out.println(b.position() + "/" + b.limit()); // 0/2
```

线上文件句柄泄漏的判断链路：`lsof -p <pid> | wc -l` 看数量，`lsof -p <pid> | awk '{print $9}' | sort | uniq -c | sort -nr | head` 看热点路径，Java 侧回到未关闭的流、目录遍历、HTTP 响应体或 Netty ByteBuf 生命周期。

## ✅ 精通检查清单

- 能用自己的话讲清本主题解决的问题和适用边界。
- 能指出至少 2 个源码入口或底层机制。
- 能给出一个最小代码示例或一组线上排查命令。
- 能回答本页列出的高频面试题，并补充项目中的真实取舍。

## 🔗 关联笔记

- [[_index]]
- [[05-异常处理最佳实践]]
- [[08-常用工具-jstat-jmap-jstack-Arthas]]

## 📚 参考资料

- [Dev.java Tutorials](https://dev.java/learn/)
- [Oracle Java Documentation](https://docs.oracle.com/en/java/)
