---
title: 面试-Java核心
tags:
  - 领域/求职
  - 领域/Java后端
  - 类型/面试
  - 主题/Java核心
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# 面试-Java核心

> [!abstract] 一句话总结
> Java 核心面试要把集合、泛型、反射、IO、新特性讲到“源码机制 + 使用边界 + 项目取舍”。

## Q1: HashMap 的 put 流程是什么？

> 先计算扰动 hash，用 `(n - 1) & hash` 定位桶；桶为空直接插入，桶非空先判断 key 是否相等，若不相等则链表或红黑树插入；插入后 size 超过 threshold 触发 resize。JDK 8 后链表过长且容量达到阈值会树化，降低极端冲突下的查询复杂度。

追问要点：

- 容量为什么是 2 的幂。
- 扩容时节点为什么只会留原位或移动到 `oldIndex + oldCap`。
- 为什么并发写不能用 HashMap。

## Q2: ArrayList 和 LinkedList 怎么选？

> ArrayList 底层数组，随机访问 O(1)，尾部追加均摊 O(1)，中间插入删除需要搬移元素；LinkedList 是双向链表，随机访问 O(n)，已定位节点后的插入删除 O(1)，但对象开销和缓存局部性差。业务中绝大多数读多写少列表优先 ArrayList。

## Q3: 泛型类型擦除是什么？

> Java 泛型主要在编译期做类型检查，运行期大多擦除为原始类型或边界类型。这解释了为什么不能 `new T()`、不能创建泛型数组、`List<String>` 和 `List<Integer>` 运行时 class 相同。

## Q4: 反射和动态代理在框架里怎么用？

> 反射用于运行期读取类、方法、字段并调用；动态代理用于在不修改目标对象的情况下增强行为。Spring AOP、声明式事务、MyBatis Mapper 都大量使用代理思想。JDK 动态代理基于接口，CGLIB 基于子类。

## Q5: Java 8+ 常用新特性怎么落地？

> Lambda 和 Stream 提高集合处理表达力，但不要在复杂业务中过度链式；Record 适合不可变数据载体；sealed class 适合限制继承层级；虚拟线程适合高并发阻塞 IO，但不等于 CPU 计算变快。

## 项目表达模板

```text
我在项目里用 HashMap/ConcurrentHashMap 做过本地索引或缓存。
当时关注点不是“能不能存”，而是 key 的 hashCode/equals 是否稳定、
容量是否会频繁扩容、是否存在并发写、是否需要过期淘汰。
如果是多线程场景，我会优先选 ConcurrentHashMap 或外部缓存。
```

## 关联笔记

- [[../20-Java后端/01-Java核心/04-集合框架/04-HashMap原理与扩容]]
- [[../20-Java后端/01-Java核心/04-集合框架/01-核心机制与选型]]
- [[../20-Java后端/01-Java核心/01-泛型与类型擦除]]
- [[40-求职准备/_index|求职准备索引]]

## 📚 参考资料

- 目标公司官网、招聘 JD 与技术博客。
- Java、Spring、MySQL、Redis、Kafka 等官方文档。
- 已沉淀项目文档与面试复盘记录。

