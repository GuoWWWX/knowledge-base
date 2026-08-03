---
title: IO与NIO
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Java核心
  - 阶段/01
created: 2026-07-04
updated: 2026-08-03
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# IO与NIO

> [!abstract] 一句话总结
> Java IO 面向流（同步阻塞），NIO 面向 Buffer/Channel/Selector（同步非阻塞 + 多路复用）：核心差异是"数据怎么移动"和"一个线程能管多少连接"；Buffer 状态机（position/limit/capacity）、零拷贝和文件描述符泄漏是工程重点。

## 🎯 本节定位

- **本章第 7 课，数据移动**。前置：[[02-异常处理最佳实践]]（所有 IO 的关闭与异常处理都靠 try-with-resources + suppressed）、[[06-Lambda与Stream]]（流的函数式思维对比）。
- **延伸阅读（本课不展开）**：Netty 的 Reactor 模型、Kafka/RocketMQ 网络层基于 NIO 的完整实现属于中间件/框架章节；epoll 等操作系统机制只在本课做概念级说明。

## 📌 这是什么

- **传统 IO（BIO）**：以 `InputStream/OutputStream`、`Reader/Writer` 为核心，**同步阻塞**——read 没数据就卡住线程。
- **NIO**：以 `Buffer`（数据容器）、`Channel`（双向通道）、`Selector`（多路复用器）为核心，**一个线程可以管理成千上万个连接**。
- 三者关系一句话：**Channel 读写 Buffer，Selector 监听 Channel 的读写事件**。

## 🤔 解决什么问题 / 为什么需要

- **BIO 的线程模型撑不住高并发**：一个连接一个线程，1 万连接要 1 万线程，线程栈（默认 1MB/个）先爆内存，上下文切换先打爆 CPU。
- **NIO 用事件驱动解决**：一个线程 select 等待"哪个连接有数据可读"，只处理就绪的连接——线程数 = 核数级别也能扛海量连接。
- 文件处理要关注：缓冲区大小、字符集、资源关闭、大文件内存占用。
- 理解 NIO 是理解 Netty、Kafka 网络层的前置（它们是 NIO 的生产级封装）。

## 🧩 核心概念

| 概念 | 含义 | 精通要点 |
| ---- | ---- | -------- |
| Stream | 单向字节/字符流。 | API 简单，但阻塞模型下线程成本高。 |
| Channel | 可双向读写的通道。 | `FileChannel`、`SocketChannel`、`ServerSocketChannel`。 |
| Buffer | 数据容器，三个指针：position/limit/capacity。 | flip/clear/compact 是高频易错点。 |
| Selector | 多路复用器，监听多个 Channel 的事件。 | 底层对应 epoll（Linux）/kqueue（macOS）/select。 |
| 阻塞/非阻塞 | 线程是否等待数据就绪。 | 阻塞省心费线程，非阻塞费心省线程。 |

## ⚙️ 工作原理 / 流程

```mermaid
flowchart LR
    A[SocketChannel 注册到 Selector] --> B[select 等待就绪事件]
    B --> C[返回就绪的 SelectionKey 集合]
    C --> D[read 到 Buffer]
    D --> E[flip 切读模式]
    E --> F[业务解码处理]
    F --> G[clear 后 write 响应]
```

**BIO/NIO/AIO 对比**（面试必背）：

| 模型 | 线程模型 | 适用场景 | 代表 |
| --- | --- | --- | --- |
| BIO | 同步阻塞，一连接一线程 | 连接数少、低并发 | 传统 Socket |
| NIO | 同步非阻塞 + 多路复用，一线程多连接 | 高并发连接、IO 密集 | Netty 默认 |
| AIO | 异步非阻塞，完成后回调 | 极少使用（Linux 实现不成熟） | NIO.2 AsynchronousSocketChannel |

## 🔍 源码 / 原理深挖

### Buffer 状态机：position / limit / capacity

三个指针是 NIO 最容易翻车的地方，必须背熟：

```java
ByteBuffer b = ByteBuffer.allocate(8);
// 初始: position=0, limit=8(capacity), capacity=8
b.put((byte) 1).put((byte) 2);
// 写 2 字节后: position=2, limit=8
b.flip();                 // 写→读: limit=position(2), position=0
// 读模式: position=0, limit=2 —— 只能读这 2 个字节
b.get();                  // position=1
b.clear();                // 读→写(全部清): position=0, limit=capacity —— 丢弃未读数据
// 若想保留未读数据: b.compact() —— 把未读内容搬到头部, position=未读数, limit=capacity
```

- **flip**：写模式 → 读模式（`limit = position; position = 0`）。
- **clear**：读模式 → 写模式，丢弃全部未读内容。
- **compact**：读模式 → 写模式，**保留未读内容**（挪到头部继续写）。
- 经典 bug：写完忘 flip 就读 → 读到 position 之后全是 0/旧数据；读完忘 clear/compact → 下次写入覆盖异常。

### 零拷贝：数据少搬几次

传统读文件再发网络：`磁盘 → 内核缓冲区 → 用户态缓冲区 → 内核 socket 缓冲区 → 网卡`，**用户态和内核态之间拷贝 2 次**。`FileChannel.transferTo/transferFrom` 在 Linux 上走 `sendfile` 系统调用，`磁盘 → 内核缓冲区 → 网卡`，**用户态 0 拷贝**。对"文件传输"类服务（下载、图片、视频）提升明显。

### 堆外（直接）缓冲区

`ByteBuffer.allocateDirect(cap)` 在堆外分配内存，读写时**省去"堆内 ↔ 系统调用"之间的一次拷贝**（JNI 可以直接访问地址），适合长连接、大数据量读写；代价是分配/回收成本高（由 GC 管理 DirectByteBuffer 的 Cleaner 回收），**不宜频繁创建**。

### Selector 为什么能省线程

`Selector.select()` 阻塞到**至少一个已注册 Channel 有事件就绪**，返回就绪集合后，线程逐个处理。Linux 底层是 epoll（O(1) 查找就绪事件、水平/边缘触发），相比"每连接一个阻塞线程"，线程数从 O(连接数) 降到 O(核数)。

## 💻 代码实践 / 排查命令

```java
// 正确读文件：try-with-resources + 循环读 + flip/clear
try (FileChannel channel = FileChannel.open(path, StandardOpenOption.READ)) {
    ByteBuffer buffer = ByteBuffer.allocate(8192);
    while (channel.read(buffer) != -1) {
        buffer.flip();                    // 写→读
        while (buffer.hasRemaining()) {
            buffer.get();
        }
        buffer.clear();                   // 读→写，丢弃已处理内容
    }
}
```

```java
// 零拷贝文件下载（伪代码）
try (FileChannel in = FileChannel.open(src); SocketChannel out = ...) {
    in.transferTo(0, in.size(), out);     // 内核态直达，用户态零拷贝
}
```

```bash
# 文件句柄泄漏排查
lsof -p <pid> | wc -l                                    # 句柄总量是否持续上涨
lsof -p <pid> | awk '{print $9}' | sort | uniq -c | sort -nr | head   # 热点路径
# 配合: 检查代码里所有流是否 try-with-resources / finally 关闭
```

## 🛠️ 排查 / 实践要点

- **句柄泄漏**：`lsof -p <pid>` 看数量趋势；Java 侧回到未关闭的流、目录遍历、HTTP 响应体、Netty ByteBuf 生命周期。
- **乱码问题**：统一 Charset（`Files.newBufferedReader(path, StandardCharsets.UTF_8)`），不要依赖平台默认编码（Windows GBK / Linux UTF-8 不一致）。
- **线程阻塞定位**：`jstack` 看线程栈卡在 `read`/`write`/`connect`（IO 阻塞）还是业务代码——NIO 服务线程全卡住时优先怀疑阻塞调用混入。
- **大文件**：`Files.readAllBytes` 直接读 1GB 文件 = 1GB 堆内存，必须流式/分块处理。

## ⚠️ 踩坑与注意事项

- `readAllBytes` 读大文件导致内存暴涨。
- ByteBuffer 写完忘记 `flip`，读完忘记 `clear/compact`。
- 以为 NIO 一定更快：**低并发简单文件读写中传统 IO 更直接**，NIO 的复杂度只有在连接数/数据量大时才回本。
- 在 NIO 事件循环线程里做阻塞操作（DB、锁、大计算）——整个 Selector 被卡住，等于所有连接超时。
- 句柄用完不关（`Files.list`、`Stream`、`Channel` 都是资源，都要 try-with-resources）。

## 🎯 面试问题

> [!question] 高频问题
> - Q: BIO、NIO、AIO 区别？
>   - A: BIO 同步阻塞、一连接一线程；NIO 同步非阻塞 + 多路复用、一线程多连接；AIO 异步非阻塞、完成回调。高并发网络服务选 NIO（Netty），AIO 因 Linux 实现问题很少用。
> - Q: Buffer 的 flip 做什么？clear 和 compact 区别？
>   - A: flip 把 limit 设为当前 position、position 归零，写模式切读模式；clear 丢弃未读数据清空，compact 保留未读数据挪到头部继续写。
> - Q: 零拷贝是什么？
>   - A: 减少数据在用户态/内核态之间的拷贝次数。FileChannel.transferTo 走 sendfile，文件数据从内核缓冲区直达网卡，用户态零拷贝。
> - Q: 为什么 NIO 能支撑高并发连接？
>   - A: Selector 多路复用：一个线程阻塞等待"就绪事件集合"，只处理就绪的连接（底层 epoll），线程数从 O(连接数) 降到 O(核数)。

## 🧭 源码入口速查

- `FileInputStream#readBytes`、`SocketInputStream#socketRead0`：传统 IO 最终进入 native read。
- `java.nio.Buffer#flip/clear/compact`：position/limit/capacity 状态机核心。
- `Selector#select`、`SelectionKey#readyOps`：多路复用事件循环入口。
- `FileChannel#transferTo/transferFrom`：零拷贝入口。
- `java.nio.ByteBuffer#allocateDirect`：堆外缓冲区分配。

## 🧪 最小验证

```java
// 1. Buffer 状态机
ByteBuffer b = ByteBuffer.allocate(4);
b.put((byte) 1).put((byte) 2);
System.out.println(b.position() + "/" + b.limit()); // 2/4
b.flip();
System.out.println(b.position() + "/" + b.limit()); // 0/2
b.get();
System.out.println(b.position() + "/" + b.limit()); // 1/2
b.compact();
System.out.println(b.position() + "/" + b.limit()); // 1/4（保留了 1 个未读字节）

// 2. 句柄泄漏最小复现：反复 open 不关闭
for (int i = 0; i < 100000; i++) {
    FileChannel ch = FileChannel.open(path, StandardOpenOption.READ);
    // 不关闭 → 句柄数持续上涨，最终 IOException: Too many open files
}
```

线上文件句柄泄漏的判断链路：`lsof -p <pid> | wc -l` 看数量，`lsof -p <pid> | awk '{print $9}' | sort | uniq -c | sort -nr | head` 看热点路径，Java 侧回到未关闭的流、目录遍历、HTTP 响应体或 Netty ByteBuf 生命周期。

## ✅ 精通检查清单

- 能用自己的话讲清本主题解决的问题和适用边界。
- 能指出至少 2 个源码入口或底层机制。
- 能给出一个最小代码示例或一组线上排查命令。
- 能回答本页列出的高频面试题，并补充项目中的真实取舍。

## 🔗 关联笔记

- 本课内：上一课 [[06-Lambda与Stream]]；下一课 [[08-反射与动态代理]]
- 延伸：[[20-Java后端/02-JVM/08-常用工具-jstat-jmap-jstack-Arthas|JVM 排查工具]]（jstack 定位 IO 阻塞）；Netty 网络层属于中间件章节

## 📚 参考资料

- [Dev.java Tutorials](https://dev.java/learn/)
- [Oracle Java Documentation](https://docs.oracle.com/en/java/)
- [JDK 源码 java.nio.Buffer](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/nio/Buffer.java)
