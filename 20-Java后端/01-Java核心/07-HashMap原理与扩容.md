---
title: HashMap原理与扩容
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

# HashMap原理与扩容

> [!abstract] 一句话总结
> HashMap 用"数组 + 链表/红黑树"实现平均 O(1) 的键值访问；容量恒为 2 的幂，负载因子 0.75 是空间与时间的折中，扩容翻倍后用 `hash & oldCap` 拆链，避免重算每个节点的位置——每个设计数字背后都有明确的因果。

## 🎯 本节定位

- **本章第 7 课，集合深挖**。前置：[[04-集合框架-核心机制与选型]]（HashSet 基于 HashMap、equals/hashCode 契约）。本课把哈希表这一个数据结构讲透。
- **延伸阅读（本课不展开）**：红黑树旋转/染色细节在 [[30-算法与数据结构/01-数据结构/07-平衡树-AVL-红黑树|算法章节]]；并发版本的 `ConcurrentHashMap` 在 [[20-Java后端/03-并发编程/08-并发容器-ConcurrentHashMap|03-并发编程章节]]，本课只讲单线程语义。

## 📌 这是什么

- JDK 8 的 `HashMap` 主结构是 `Node<K,V>[] table`（桶数组），每个桶初始是链表，**冲突严重且容量足够时树化为红黑树**。
- 默认容量 16、负载因子 0.75；`threshold = capacity × loadFactor`，size 超过 threshold 触发扩容。
- 允许 `null` key（放在 table[0] 桶）和 `null` value；不保证顺序；**不是线程安全容器**。

## 🤔 解决什么问题 / 为什么需要

- 数组按下标访问 O(1)，但键不是整数 → 用哈希函数把任意对象映射成下标。
- 哈希冲突不可避免 → 链表/红黑树解决冲突；冲突严重时红黑树保证最坏 O(log n)。
- 正确设计 key 的 equals/hashCode，避免缓存、去重、分组统计出现隐蔽错误。
- 能识别高冲突、容量估算不足、并发误用造成的线上风险。

## 🧩 核心概念

| 概念 | 含义 | 精通要点 |
| ---- | ---- | -------- |
| hash 扰动 | `h ^ (h >>> 16)` 让高位参与低位索引计算。 | 配合 `(n - 1) & hash` 定位桶。 |
| 负载因子 | 元素数 / 桶容量 的比值。 | 默认 0.75：过高冲突多，过低浪费空间。 |
| 树化 | 单桶链表长度 ≥ 8 且容量 ≥ 64 时转红黑树。 | `treeifyThreshold=8`，`minTreeifyCapacity=64`。 |
| 扩容 | 容量翻倍，节点按 `hash & oldCap` 拆成 low/high 两组。 | JDK 8 不重算完整 hash，只移 `index + oldCap`。 |
| threshold | 触发扩容的阈值 = capacity × loadFactor。 | 构造时可传入容量，HashMap 会向上取 2 的幂。 |

## ⚙️ 工作原理 / 流程

```mermaid
flowchart TD
    A[put key/value] --> B[hash 扰动 h^h>>>16]
    B --> C[定位桶 index = (n-1) & hash]
    C --> D{桶是否为空}
    D -- 是 --> E[新建 Node]
    D -- 否 --> F[遍历链表或红黑树]
    F --> G{key 已存在}
    G -- 是 --> H[覆盖 value 返回旧值]
    G -- 否 --> I[尾插/树插入]
    I --> J{size > threshold}
    J -- 是 --> K[resize 容量翻倍拆链]
```

**get 流程**：`hash(key)` → 定位桶 → 检查桶首节点 → 树节点走树查找、否则遍历链表 → 用 `equals` 判断命中 → 未命中返回 null。

## 🔍 源码 / 原理深挖（每个"为什么"）

### 为什么容量是 2 的幂？

两个原因，环环相扣：
1. **位运算代替取模**：`hash % n` 在 n 是 2 的幂时等价于 `(n - 1) & hash`，位运算更快。
2. **扩容拆链**：容量翻倍后，节点新位置只看 `hash & oldCap` 这一位——为 0 留在原索引，为 1 移到 `index + oldCap`。若容量不是 2 的幂，每个节点都要重新取模，扩容成本从 O(n) 退化为更慢且无法拆分。

### 为什么要有扰动函数？

`n` 通常不大（默认 16），`(n - 1)` 只有低 4 位是 1，**直接取 `hashCode & (n-1)` 时高位完全不参与**。两个 hashCode 高位不同、低位相同的 key 会全部冲突。`h ^ (h >>> 16)` 把高 16 位"搅"到低 16 位，让分布更均匀。代价一次异或，收益显著。

### 为什么负载因子是 0.75？

这是空间与时间的折中：负载因子 0.5 时冲突更少但一半空间空置（16 容量只能放 8 个就扩容）；1.0 时空间利用率高但链表很长、查找退化。0.75 是 JDK 作者基于大量实验的经验值：**在"链长分布可控"和"空间不浪费"之间取平衡**。JDK 源码注释里给出了泊松分布推导：负载因子 0.75 下，单桶链长达到 8 的概率约为千万分之六（0.00000006），所以 8 是"几乎不可能出现"的冲突程度，用它当树化阈值很安全。

### 为什么树化要同时满足两个条件？

- 单桶链长 ≥ 8（`treeifyThreshold`）。
- 桶数组容量 ≥ 64（`minTreeifyCapacity`）。

容量 < 64 时，链长 8 说明"哈希太烂"或"容量太小"，此时**先扩容**（让元素分散）比树化更划算——扩容后链长自然变短。容量 ≥ 64 且链长仍 ≥ 8，才说明这个桶确实冲突集中，值得转红黑树。另外树节点是普通节点的 2 倍大小（多了左右父指针和红黑属性），树化有内存成本，所以阈值设得保守。

### JDK 7 → JDK 8 改了什么？（面试高频）

| 维度 | JDK 7 | JDK 8 |
| --- | --- | --- |
| 冲突结构 | 链表 | 链表 + 红黑树（≥8 且容量 ≥64） |
| 插入方式 | **头插法** | **尾插法** |
| 扩容位置 | 重算 hash 取模 | `hash & oldCap` 拆 low/high 两条链 |
| 并发问题 | 扩容时头插**可能形成环形链表**，get 死循环 | 尾插不产生环，但仍丢数据，**依然线程不安全** |

JDK 7 死循环的根因：多线程同时 resize，头插法把节点顺序反转，两个线程交错操作可能让链表的 next 指回自己，之后 get 遍历该桶就死循环。JDK 8 改尾插避免了这个具体 bug，但 put/resize 无同步保护，并发写仍会丢数据、覆盖错误——**并发场景必须用 ConcurrentHashMap**（见并发章节）。

## 💻 代码实践 / 排查命令

```java
// 1. 预估容量，减少 resize
int expectedSize = 10_000;
int capacity = (int) (expectedSize / 0.75f) + 1;
Map<Long, String> cache = new HashMap<>(capacity);

// 2. 不可变 key：参与 hashCode 的字段 final
final class UserKey {
    final long tenantId;
    final long userId;
    UserKey(long tenantId, long userId) { this.tenantId = tenantId; this.userId = userId; }
    @Override public boolean equals(Object o) {
        return o instanceof UserKey k && k.tenantId == tenantId && k.userId == userId;
    }
    @Override public int hashCode() { return Objects.hash(tenantId, userId); }
}
Map<UserKey, String> userCache = new HashMap<>();
userCache.put(new UserKey(1L, 100L), "Alice");
// 提示：Java 16+ 可以直接用 record 定义这种不可变 key，见 08 课
```

```bash
# 排查 hash 冲突：打印 key 的 hashCode 分布
# jcmd / jstack 帮不上哈希的忙，用代码统计桶长分布即可
```

## 🛠️ 排查 / 实践要点

- 大批量 put 前**预估容量**（`expectedSize / 0.75f + 1`），减少 resize 的 CPU 和临时数组成本。
- 排查命中率异常：打印 key 的 hashCode/equals 相关字段，确认字段**没有在入 map 后被修改**；跨服务场景检查序列化前后字段是否变化。
- 多线程共享写入：换 `ConcurrentHashMap`，或外层串行化写入（见并发章节）。
- 需要顺序/排序：用 `LinkedHashMap` 或 `TreeMap`（见 03 课），别依赖 HashMap 的迭代顺序。

## ⚠️ 踩坑与注意事项

- 用可变对象做 key：put 后修改参与 hashCode 的字段 → get/remove 找不到（对象还在，但桶位错了）。
- 只重写 equals 不重写 hashCode（或反之），去重/查找全乱。
- 误以为树化后所有操作都是稳定 O(log n)：树化前提苛刻，整体性能仍依赖哈希分布；普通数据量下链表就是常态。
- 在并发场景直接共享 HashMap：JDK 8 不会死循环了，但**丢数据、脏读**照样发生。
- 用 HashMap 当缓存不做容量上限：可能 OOM（本地缓存容量边界见系统设计章节）。

## 🎯 面试问题

> [!question] 高频问题
> - Q: HashMap 为什么容量是 2 的幂？
>   - A: 一是 `(n - 1) & hash` 位运算替代取模更快；二是扩容翻倍后能用 `hash & oldCap` 把节点拆成"原位置"或"原位置+oldCap"两组，不用重算每个节点。
> - Q: JDK 8 的 HashMap 相比 JDK 7 优化了什么？
>   - A: ①链表+红黑树（链长≥8 且容量≥64 时树化）；②尾插代替头插，避免 JDK 7 扩容时并发形成环形链表导致死循环；③扩容按 `hash & oldCap` 拆链不重算 hash。但并发写仍不安全。
> - Q: 为什么负载因子是 0.75？树化阈值为什么是 8？
>   - A: 0.75 是空间/时间折中的经验值；该负载因子下链长 8 的概率约千万分之六（泊松分布），几乎不可能自然出现，因此 8 作为树化阈值足够保守；容量 < 64 时优先扩容而非树化，因为扩容让元素分散更划算。
> - Q: HashMap 为什么线程不安全？
>   - A: put、resize、链表/树结构修改都没有同步保护，并发写可能丢数据、覆盖错误；JDK 7 还有扩容头插形成环的死循环问题。并发场景用 ConcurrentHashMap。

## 🧭 源码入口速查

- `HashMap#hash`：`h ^ (h >>> 16)` 扰动函数。
- `HashMap#putVal`：初始化 table、首节点命中、链表遍历、树插入、`treeifyBin`、`resize` 全在这。
- `HashMap#resize`：`oldCap/oldThr/newCap/newThr` 计算 + `loHead/hiHead` 拆链。
- `HashMap#treeifyBin`：链长 ≥ 8 但容量 < 64 时**只扩容不树化**。
- `HashMap.TreeNode#split`：扩容时红黑树节点也按 `hash & oldCap` 拆分，必要时退化回链表。
- `HashMap#getNode`：get 主流程，先查桶首节点再按树/链表查。

## 🧪 最小验证

```java
// 1. 高冲突演示：所有 key 哈希到同一桶，观察链表 → 树化
final class BadKey {
    final int id;
    BadKey(int id) { this.id = id; }
    public int hashCode() { return 1; }                       // 全部冲突
    public boolean equals(Object o) { return o instanceof BadKey k && k.id == id; }
}
Map<BadKey, Integer> map = new HashMap<>(64);
for (int i = 0; i < 9; i++) map.put(new BadKey(i), i);
// 断点进 treeifyBin，观察链表转 TreeNode 的条件（链长 8、容量 64）

// 2. 扩容拆链验证：断点进 resize，观察 low/high 两条链
//    一个节点的 hash & oldCap == 0 → 留原位；== 1 → 移到 index + oldCap
```

线上缓存命中异常时，优先检查 key：参与 `equals/hashCode` 的字段是否可变、是否包含数组、跨服务序列化后语义是否变化。HashMap 本身很少"随机丢数据"，大多是 key 语义或并发写入破坏了查找路径。

## ✅ 精通检查清单

- 能用自己的话讲清本主题解决的问题和适用边界。
- 能指出至少 2 个源码入口或底层机制。
- 能给出一个最小代码示例或一组线上排查命令。
- 能回答本页列出的高频面试题，并补充项目中的真实取舍。

## 🔗 关联笔记

- 本课内：上一课 [[04-集合框架-核心机制与选型]]；应用见 [[10-Lambda与Stream]]
- 延伸：[[30-算法与数据结构/01-数据结构/07-平衡树-AVL-红黑树|红黑树原理]]；[[20-Java后端/03-并发编程/08-并发容器-ConcurrentHashMap|ConcurrentHashMap]]

## 📚 参考资料

- [Dev.java Tutorials](https://dev.java/learn/)
- [Oracle Java Documentation](https://docs.oracle.com/en/java/)
- [JDK 源码 HashMap.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/HashMap.java)
