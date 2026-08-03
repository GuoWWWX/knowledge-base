---
title: 04 · 集合框架（索引）
tags: [类型/索引, 领域/Java后端, 主题/Java核心]
created: 2026-08-03
updated: 2026-08-03
---

# 🧰 04 · 集合框架

> [!info] 本区说明
> 集合知识按"共同机制一篇 + 各实现分篇贴源码"组织：先学总纲（家族树、选型、公共源码机制），再逐个实现看源码与面试题。对应全章第 4~9 课，接在 [[20-Java后端/01-Java核心/03-String与字符串常量池|03 · String]] 之后，衔接 [[20-Java后端/01-Java核心/10-Lambda与Stream|10 · Lambda 与 Stream]]。

## 📚 知识笔记清单（按学习顺序编号，顺着编号读即可）

- [ ] [[01-核心机制与选型]] — 集合总纲：家族树、选型表、公共源码机制（契约/fail-fast/迭代器/工具类）
- [ ] [[02-List实现-ArrayList与LinkedList]] — 数组 vs 链表源码、扩容 1.5 倍、对比表
- [ ] [[03-Set实现-HashSet与TreeSet]] — 委托 Map 源码、Comparable vs Comparator
- [ ] [[04-HashMap原理与扩容]] — 哈希深挖：扰动、负载因子、树化、扩容拆链
- [ ] [[05-Map实现-LinkedHashMap与TreeMap]] — 保序/LRU/范围查询源码
- [ ] [[06-Queue实现-ArrayDeque与PriorityQueue]] — 循环数组、二叉堆、TopK

## 🧭 学习路线

1. **先读总纲 [[01-核心机制与选型]]**：建立家族树全貌、背下选型表，理解 equals/hashCode 契约、modCount/fail-fast、迭代器这些公共机制——后面各篇直接引用，不再重复。
2. **再按实现分篇**：[[02-List实现-ArrayList与LinkedList]] → [[03-Set实现-HashSet与TreeSet]] → [[04-HashMap原理与扩容]] → [[05-Map实现-LinkedHashMap与TreeMap]] → [[06-Queue实现-ArrayDeque与PriorityQueue]]。每篇都把关键 JDK 源码贴进文档逐段讲解，附高频面试题。
3. **面试导向**：学完每篇能回答该篇"面试问题"小节（高频 + 追问），并能写出最小验证代码。

## 🧪 验证目标

- 看到需求能直接说出用哪个集合类并写出代码（选型总表见 01 篇）。
- 能默写并讲清 ArrayList 扩容、HashSet 委托、HashMap 拆链、LinkedHashMap LRU、PriorityQueue 上浮的关键源码段。
- 能答出四族面试题：List 选型、Set 去重、Map 顺序、Queue 优先级。

## 🔗 相关

- 上级：[[20-Java后端/01-Java核心/_index|01 · Java 核心]]
- 前置：[[20-Java后端/01-Java核心/01-泛型与类型擦除|泛型]] · [[20-Java后端/01-Java核心/03-String与字符串常量池|String]]
- 后续：[[20-Java后端/01-Java核心/10-Lambda与Stream|Lambda 与 Stream]]（集合处理）
- 延伸：[[30-算法与数据结构/01-数据结构/07-平衡树-AVL-红黑树|红黑树原理]]（算法章节）；[[20-Java后端/03-并发编程/08-并发容器-ConcurrentHashMap|并发容器]]（并发章节）
