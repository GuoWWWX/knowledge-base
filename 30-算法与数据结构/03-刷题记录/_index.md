---
title: 03 · 刷题记录（索引）
tags: [类型/索引, 领域/算法]
created: 2026-05-31
updated: 2026-10-27
---

# 📝 03 · 刷题记录

> [!info] 本区说明
> 按题号记录的高频经典手撕题解与大厂面试必杀题。点击“在线做题”可直达 LeetCode 官网，点击“本地题解”可查看深度解析。

---

## 🛠️ 大厂白板高频手撕专项（数据结构设计与高频算法）

面试官最喜欢要求“不借助任何现成工具库，从零纯手工手撕代码”的标杆题目集中在此：

| 题号 | 题目名称 (在线做题 ↗) | 手撕考点 | 难度 | 掌握度 | 本地手撕题解笔记 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **146** | [LRU 缓存机制 ↗](https://leetcode.cn/problems/lru-cache/) | 双向链表 + 哈希表，双虚拟哨兵 | 中等 | ⭐⭐⭐⭐⭐ | [[14-LC0146-LRU缓存]] |
| **706** | [设计哈希映射 (手撕HashMap) ↗](https://leetcode.cn/problems/design-hashmap/) | 大质数取模 + 链表桶拉链法 | 简单 | ⭐⭐⭐⭐⭐ | [[26-LC0706-设计哈希映射]] |
| **460** | [LFU 缓存机制 ↗](https://leetcode.cn/problems/lfu-cache/) | 双哈希表 + 频次双向链表，代码最长设计题 | 困难 | ⭐⭐⭐⭐⭐ | [[29-LC0460-手撕LFU缓存]] |
| **208** | [实现 Trie (前缀树) ↗](https://leetcode.cn/problems/implement-trie-prefix-tree/) | 26叉树节点设计、前缀匹配与单词查找 | 中等 | ⭐⭐⭐⭐⭐ | [[28-LC0208-手撕前缀树Trie]] |
| **912** | [手撕快速排序与归并排序 ↗](https://leetcode.cn/problems/sort-an-array/) | 随机基准值双指针对撞 vs 二分分治合并 | 中等 | ⭐⭐⭐⭐⭐ | [[27-LC0912-手撕快速排序与归并排序]] |
| **1188** | [手撕有限阻塞队列 ↗](https://leetcode.cn/problems/design-bounded-blocking-queue/) | ReentrantLock + 双 Condition，生产者消费者 | 中等 | ⭐⭐⭐⭐⭐ | [[30-LC1188-手撕阻塞队列与生产者消费者]] |

---

## 🗂️ 全量经典算法题解库

| 题号 | 题目名称 (在线做题 ↗) | 专题 | 难度 | 掌握度 | 本地题解笔记 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **1** | [两数之和 ↗](https://leetcode.cn/problems/two-sum/) | 哈希表 | 简单 | ⭐☆☆☆☆ | [[01-LC0001-两数之和]] |
| **15** | [三数之和 ↗](https://leetcode.cn/problems/3sum/) | 双指针 | 中等 | ⭐☆☆☆☆ | [[02-LC0015-三数之和]] |
| **20** | [有效的括号 ↗](https://leetcode.cn/problems/valid-parentheses/) | 栈 | 简单 | ⭐☆☆☆☆ | [[03-LC0020-有效的括号]] |
| **21** | [合并两个有序链表 ↗](https://leetcode.cn/problems/merge-two-sorted-lists/) | 链表 | 简单 | ⭐☆☆☆☆ | [[04-LC0021-合并两个有序链表]] |
| **33** | [搜索旋转排序数组 ↗](https://leetcode.cn/problems/search-in-rotated-sorted-array/) | 二分查找 | 中等 | ⭐☆☆☆☆ | [[05-LC0033-搜索旋转排序数组]] |
| **42** | [接雨水 ↗](https://leetcode.cn/problems/trapping-rain-water/) | 双指针-单调栈 | 困难 | ⭐⭐⭐⭐⭐ | [[22-LC0042-接雨水]] |
| **46** | [全排列 ↗](https://leetcode.cn/problems/permutations/) | 回溯 | 中等 | ⭐☆☆☆☆ | [[06-LC0046-全排列]] |
| **53** | [最大子数组和 ↗](https://leetcode.cn/problems/maximum-subarray/) | 动态规划 | 中等 | ⭐☆☆☆☆ | [[07-LC0053-最大子数组和]] |
| **56** | [合并区间 ↗](https://leetcode.cn/problems/merge-intervals/) | 排序 | 中等 | ⭐☆☆☆☆ | [[08-LC0056-合并区间]] |
| **70** | [爬楼梯 ↗](https://leetcode.cn/problems/climbing-stairs/) | 动态规划 | 简单 | ⭐☆☆☆☆ | [[09-LC0070-爬楼梯]] |
| **94** | [二叉树中序遍历 ↗](https://leetcode.cn/problems/binary-tree-inorder-traversal/) | 二叉树 | 简单 | ⭐☆☆☆☆ | [[10-LC0094-二叉树中序遍历]] |
| **102** | [二叉树层序遍历 ↗](https://leetcode.cn/problems/binary-tree-level-order-traversal/) | BFS | 中等 | ⭐☆☆☆☆ | [[11-LC0102-二叉树层序遍历]] |
| **110** | [平衡二叉树 ↗](https://leetcode.cn/problems/balanced-binary-tree/) | 二叉树-DFS | 简单 | ⭐⭐⭐⭐⭐ | [[24-LC0110-平衡二叉树]] |
| **121** | [买卖股票的最佳时机 ↗](https://leetcode.cn/problems/best-time-to-buy-and-sell-stock/) | 贪心 | 简单 | ⭐☆☆☆☆ | [[12-LC0121-买卖股票的最佳时机]] |
| **128** | [最长连续序列 ↗](https://leetcode.cn/problems/longest-consecutive-sequence/) | 哈希表 | 中等 | ⭐⭐⭐⭐⭐ | [[25-LC0128-最长连续序列]] |
| **141** | [环形链表 ↗](https://leetcode.cn/problems/linked-list-cycle/) | 双指针 | 简单 | ⭐☆☆☆☆ | [[13-LC0141-环形链表]] |
| **146** | [LRU 缓存 (手撕LRU) ↗](https://leetcode.cn/problems/lru-cache/) | 手撕数据结构 | 中等 | ⭐⭐⭐⭐⭐ | [[14-LC0146-LRU缓存]] |
| **200** | [岛屿数量 ↗](https://leetcode.cn/problems/number-of-islands/) | DFS-BFS | 中等 | ⭐☆☆☆☆ | [[15-LC0200-岛屿数量]] |
| **206** | [反转链表 ↗](https://leetcode.cn/problems/reverse-linked-list/) | 链表 | 简单 | ⭐☆☆☆☆ | [[16-LC0206-反转链表]] |
| **208** | [实现 Trie (前缀树) ↗](https://leetcode.cn/problems/implement-trie-prefix-tree/) | 手撕数据结构 | 中等 | ⭐⭐⭐⭐⭐ | [[28-LC0208-手撕前缀树Trie]] |
| **215** | [数组中的第K个最大元素 ↗](https://leetcode.cn/problems/kth-largest-element-in-an-array/) | 堆-快速选择 | 中等 | ⭐☆☆☆☆ | [[17-LC0215-数组中的第K个最大元素]] |
| **236** | [二叉树的最近公共祖先 ↗](https://leetcode.cn/problems/lowest-common-ancestor-of-a-binary-tree/) | 二叉树 | 中等 | ⭐☆☆☆☆ | [[18-LC0236-二叉树的最近公共祖先]] |
| **239** | [滑动窗口最大值 ↗](https://leetcode.cn/problems/sliding-window-maximum/) | 滑动窗口-单调队列 | 困难 | ⭐⭐⭐⭐⭐ | [[23-LC0239-滑动窗口最大值]] |
| **300** | [最长递增子序列 ↗](https://leetcode.cn/problems/longest-increasing-subsequence/) | 动态规划-二分 | 中等 | ⭐☆☆☆☆ | [[19-LC0300-最长递增子序列]] |
| **322** | [零钱兑换 ↗](https://leetcode.cn/problems/coin-change/) | 动态规划 | 中等 | ⭐☆☆☆☆ | [[20-LC0322-零钱兑换]] |
| **460** | [LFU 缓存 (手撕LFU) ↗](https://leetcode.cn/problems/lfu-cache/) | 手撕数据结构 | 困难 | ⭐⭐⭐⭐⭐ | [[29-LC0460-手撕LFU缓存]] |
| **560** | [和为K的子数组 ↗](https://leetcode.cn/problems/subarray-sum-equals-k/) | 前缀和 | 中等 | ⭐☆☆☆☆ | [[21-LC0560-和为K的子数组]] |
| **706** | [设计哈希映射 (手撕HashMap) ↗](https://leetcode.cn/problems/design-hashmap/) | 手撕数据结构 | 简单 | ⭐⭐⭐⭐⭐ | [[26-LC0706-设计哈希映射]] |
| **912** | [排序数组 (手撕快排/归并) ↗](https://leetcode.cn/problems/sort-an-array/) | 手撕算法 | 中等 | ⭐⭐⭐⭐⭐ | [[27-LC0912-手撕快速排序与归并排序]] |
| **1188** | [设计有限阻塞队列 ↗](https://leetcode.cn/problems/design-bounded-blocking-queue/) | 手撕并发结构 | 中等 | ⭐⭐⭐⭐⭐ | [[30-LC1188-手撕阻塞队列与生产者消费者]] |

## 🔥 错题集（需重刷）

- 二刷时把未掌握题加入这里。

## 🔗 相关
- 上级：[[../00-总览/学习仪表盘]]
- 求职白板手撕：[[../../40-求职准备/16-面试-手撕代码|40 · 面试白板手撕代码]]
