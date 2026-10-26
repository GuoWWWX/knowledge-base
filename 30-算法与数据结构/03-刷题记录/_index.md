---
title: 03 · LeetCode Hot 100 全量刷题与通关看板
tags: [类型/索引, 领域/算法, 主题/Hot100]
created: 2026-05-31
updated: 2026-10-27
---

# 🎯 LeetCode Hot 100 权威全量刷题与通关打卡看板

> [!abstract] 刷题通关核心法则
> 1. **千万不要按题号从 1 到 100 乱序刷**！那是最容易被 Hard 题劝退的错误姿势；
> 2. **必须按下方整理好的“16 个知识专题 + 难度循序渐进”集中突击**！同一个专题连刷 3~5 道，才能形成肌肉记忆和套路反射；
> 3. **做一道勾选一道**：每完成一题，直接在下方点击复选框 `- [ ]` 变为 `- [x]`，记录属于你的大厂通关轨迹！

---

## 🛠️ 大厂白板高频手撕专项（数据结构设计与并发实战）

面试官最喜欢要求“不借助任何现成工具库，从零纯手工手撕代码”的标杆题目集中在此：

| 题号 | 题目名称 (在线做题 ↗) | 核心手撕考点与架构 | 难度 | 掌握度 | 本地手撕题解笔记 |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **146** | [LRU 缓存机制 ↗](https://leetcode.cn/problems/lru-cache/) | 双向链表 + 哈希表，双虚拟哨兵 | 中等 | ⭐⭐⭐⭐⭐ | [[14-LC0146-LRU缓存]] |
| **706** | [设计哈希映射 (手撕HashMap) ↗](https://leetcode.cn/problems/design-hashmap/) | 大质数取模 + 链表桶拉链法 | 简单 | ⭐⭐⭐⭐⭐ | [[26-LC0706-设计哈希映射]] |
| **460** | [LFU 缓存机制 ↗](https://leetcode.cn/problems/lfu-cache/) | 双哈希表 + 频次双向链表，代码最长设计题 | 困难 | ⭐⭐⭐⭐⭐ | [[29-LC0460-手撕LFU缓存]] |
| **208** | [实现 Trie (前缀树) ↗](https://leetcode.cn/problems/implement-trie-prefix-tree/) | 26叉树节点设计、前缀匹配与单词查找 | 中等 | ⭐⭐⭐⭐⭐ | [[28-LC0208-手撕前缀树Trie]] |
| **912** | [手撕快速排序与归并排序 ↗](https://leetcode.cn/problems/sort-an-array/) | 随机基准值双指针对撞 vs 二分分治合并 | 中等 | ⭐⭐⭐⭐⭐ | [[27-LC0912-手撕快速排序与归并排序]] |
| **1188** | [手撕有限阻塞队列 ↗](https://leetcode.cn/problems/design-bounded-blocking-queue/) | ReentrantLock + 双 Condition，生产者消费者 | 中等 | ⭐⭐⭐⭐⭐ | [[30-LC1188-手撕阻塞队列与生产者消费者]] |

---

## 🗺️ LeetCode Hot 100 官方分类打卡全清单 (支持点击复选框勾选)

### 1. 哈希 (Hash)
> 💡 *大厂面试第一道开胃菜，空间换时间、快速定位与去重*

- [ ] **LC 1**. [两数之和 ↗](https://leetcode.cn/problems/two-sum/) `[简单]` ｜ 📖 本地题解：[[01-LC0001-两数之和]]
- [ ] **LC 49**. [字母异位词分组 ↗](https://leetcode.cn/problems/group-anagrams/) `[中等]`
- [ ] **LC 128**. [最长连续序列 ↗](https://leetcode.cn/problems/longest-consecutive-sequence/) `[中等]` ｜ 📖 本地题解：[[25-LC0128-最长连续序列]]

### 2. 双指针 (Two Pointers)
> 💡 *对撞指针与快慢指针，常用于数组原地操作与盛水容积短板收敛*

- [ ] **LC 283**. [移动零 ↗](https://leetcode.cn/problems/move-zeroes/) `[简单]`
- [ ] **LC 11**. [盛最多水的容器 ↗](https://leetcode.cn/problems/container-with-most-water/) `[中等]`
- [ ] **LC 15**. [三数之和 ↗](https://leetcode.cn/problems/3sum/) `[中等]` ｜ 📖 本地题解：[[02-LC0015-三数之和]]
- [ ] **LC 42**. [接雨水 ↗](https://leetcode.cn/problems/trapping-rain-water/) `[困难]` ｜ 📖 本地题解：[[22-LC0042-接雨水]]

### 3. 滑动窗口 (Sliding Window)
> 💡 *同向双指针单调滑行，将 O(N^2) 子串/子数组问题降维至 O(N)*

- [ ] **LC 3**. [无重复字符的最长子串 ↗](https://leetcode.cn/problems/longest-substring-without-repeating-characters/) `[中等]`
- [ ] **LC 438**. [找到字符串中所有字母异位词 ↗](https://leetcode.cn/problems/find-all-anagrams-in-a-string/) `[中等]`

### 4. 子串与子数组 (Subarray)
> 💡 *前缀和与单调队列进阶，连续区间问题的杀手锏*

- [ ] **LC 560**. [和为 K 的子数组 ↗](https://leetcode.cn/problems/subarray-sum-equals-k/) `[中等]` ｜ 📖 本地题解：[[21-LC0560-和为K的子数组]]
- [ ] **LC 239**. [滑动窗口最大值 ↗](https://leetcode.cn/problems/sliding-window-maximum/) `[困难]` ｜ 📖 本地题解：[[23-LC0239-滑动窗口最大值]]
- [ ] **LC 76**. [最小覆盖子串 ↗](https://leetcode.cn/problems/minimum-window-substring/) `[困难]`

### 5. 普通数组 (Ordinary Array)
> 💡 *区间合并、前缀后缀积与数组原地置换技巧*

- [ ] **LC 53**. [最大子数组和 ↗](https://leetcode.cn/problems/maximum-subarray/) `[中等]` ｜ 📖 本地题解：[[07-LC0053-最大子数组和]]
- [ ] **LC 56**. [合并区间 ↗](https://leetcode.cn/problems/merge-intervals/) `[中等]` ｜ 📖 本地题解：[[08-LC0056-合并区间]]
- [ ] **LC 189**. [轮转数组 ↗](https://leetcode.cn/problems/rotate-array/) `[中等]`
- [ ] **LC 238**. [除自身以外数组的乘积 ↗](https://leetcode.cn/problems/product-of-array-except-self/) `[中等]`
- [ ] **LC 41**. [缺失的第一个正数 ↗](https://leetcode.cn/problems/first-missing-positive/) `[困难]`

### 6. 矩阵 (Matrix)
> 💡 *二维网格模拟、螺旋遍历与对角线翻转技巧*

- [ ] **LC 73**. [矩阵置零 ↗](https://leetcode.cn/problems/set-matrix-zeroes/) `[中等]`
- [ ] **LC 54**. [螺旋矩阵 ↗](https://leetcode.cn/problems/spiral-matrix/) `[中等]`
- [ ] **LC 48**. [旋转图像 ↗](https://leetcode.cn/problems/rotate-image/) `[中等]`
- [ ] **LC 240**. [搜索二维矩阵 II ↗](https://leetcode.cn/problems/search-a-2d-matrix-ii/) `[中等]`

### 7. 链表 (Linked List)
> 💡 *大厂面试手撕出现率第一的专题！双指针、快慢指针与虚拟哨兵*

- [ ] **LC 160**. [相交链表 ↗](https://leetcode.cn/problems/intersection-of-two-linked-lists/) `[简单]`
- [ ] **LC 206**. [反转链表 ↗](https://leetcode.cn/problems/reverse-linked-list/) `[简单]` ｜ 📖 本地题解：[[16-LC0206-反转链表]]
- [ ] **LC 234**. [回文链表 ↗](https://leetcode.cn/problems/palindrome-linked-list/) `[简单]`
- [ ] **LC 141**. [环形链表 ↗](https://leetcode.cn/problems/linked-list-cycle/) `[简单]` ｜ 📖 本地题解：[[13-LC0141-环形链表]]
- [ ] **LC 142**. [环形链表 II ↗](https://leetcode.cn/problems/linked-list-cycle-ii/) `[中等]`
- [ ] **LC 21**. [合并两个有序链表 ↗](https://leetcode.cn/problems/merge-two-sorted-lists/) `[简单]` ｜ 📖 本地题解：[[04-LC0021-合并两个有序链表]]
- [ ] **LC 2**. [两数相加 ↗](https://leetcode.cn/problems/add-two-numbers/) `[中等]`
- [ ] **LC 19**. [删除链表的倒数第 N 个结点 ↗](https://leetcode.cn/problems/remove-nth-node-from-end-of-list/) `[中等]`
- [ ] **LC 24**. [两两交换链表中的节点 ↗](https://leetcode.cn/problems/swap-nodes-in-pairs/) `[中等]`
- [ ] **LC 25**. [K 个一组翻转链表 ↗](https://leetcode.cn/problems/reverse-nodes-in-k-group/) `[困难]`
- [ ] **LC 138**. [随机链表的复制 ↗](https://leetcode.cn/problems/copy-list-with-random-pointer/) `[中等]`
- [ ] **LC 148**. [排序链表 ↗](https://leetcode.cn/problems/sort-list/) `[中等]`
- [ ] **LC 23**. [合并 K 个升序链表 ↗](https://leetcode.cn/problems/merge-k-sorted-lists/) `[困难]`
- [ ] **LC 146**. [LRU 缓存 ↗](https://leetcode.cn/problems/lru-cache/) `[中等]` ｜ 📖 本地题解：[[14-LC0146-LRU缓存]]

### 8. 二叉树 (Binary Tree)
> 💡 *递归与分治的天然载体，前中后序、层序遍历与最近公共祖先*

- [ ] **LC 94**. [二叉树的中序遍历 ↗](https://leetcode.cn/problems/binary-tree-inorder-traversal/) `[简单]` ｜ 📖 本地题解：[[10-LC0094-二叉树中序遍历]]
- [ ] **LC 104**. [二叉树的最大深度 ↗](https://leetcode.cn/problems/maximum-depth-of-binary-tree/) `[简单]`
- [ ] **LC 226**. [翻转二叉树 ↗](https://leetcode.cn/problems/invert-binary-tree/) `[简单]`
- [ ] **LC 101**. [对称二叉树 ↗](https://leetcode.cn/problems/symmetric-tree/) `[简单]`
- [ ] **LC 543**. [二叉树的直径 ↗](https://leetcode.cn/problems/diameter-of-binary-tree/) `[简单]`
- [ ] **LC 102**. [二叉树的层序遍历 ↗](https://leetcode.cn/problems/binary-tree-level-order-traversal/) `[中等]` ｜ 📖 本地题解：[[11-LC0102-二叉树层序遍历]]
- [ ] **LC 108**. [将有序数组转换为二叉搜索树 ↗](https://leetcode.cn/problems/convert-sorted-array-to-binary-search-tree/) `[简单]`
- [ ] **LC 98**. [验证二叉搜索树 ↗](https://leetcode.cn/problems/validate-binary-search-tree/) `[中等]`
- [ ] **LC 230**. [二叉搜索树中第 K 小的元素 ↗](https://leetcode.cn/problems/kth-smallest-element-in-a-bst/) `[中等]`
- [ ] **LC 199**. [二叉树的右视图 ↗](https://leetcode.cn/problems/binary-tree-right-side-view/) `[中等]`
- [ ] **LC 114**. [二叉树展开为链表 ↗](https://leetcode.cn/problems/flatten-binary-tree-to-linked-list/) `[中等]`
- [ ] **LC 105**. [从前序与中序遍历序列构造二叉树 ↗](https://leetcode.cn/problems/construct-binary-tree-from-preorder-and-inorder-traversal/) `[中等]`
- [ ] **LC 437**. [路径总和 III ↗](https://leetcode.cn/problems/path-sum-iii/) `[中等]`
- [ ] **LC 236**. [二叉树的最近公共祖先 ↗](https://leetcode.cn/problems/lowest-common-ancestor-of-a-binary-tree/) `[中等]` ｜ 📖 本地题解：[[18-LC0236-二叉树的最近公共祖先]]
- [ ] **LC 124**. [二叉树中的最大路径和 ↗](https://leetcode.cn/problems/binary-tree-maximum-path-sum/) `[困难]`

### 9. 图论与搜索 (Graph & Search)
> 💡 *网格沉岛 DFS、水波扩散 BFS、拓扑排序与 Trie 树*

- [ ] **LC 200**. [岛屿数量 ↗](https://leetcode.cn/problems/number-of-islands/) `[中等]` ｜ 📖 本地题解：[[15-LC0200-岛屿数量]]
- [ ] **LC 994**. [腐烂的橘子 ↗](https://leetcode.cn/problems/rotting-oranges/) `[中等]`
- [ ] **LC 207**. [课程表 ↗](https://leetcode.cn/problems/course-schedule/) `[中等]`
- [ ] **LC 208**. [实现 Trie (前缀树) ↗](https://leetcode.cn/problems/implement-trie-prefix-tree/) `[中等]` ｜ 📖 本地题解：[[28-LC0208-手撕前缀树Trie]]

### 10. 回溯算法 (Backtracking)
> 💡 *多叉决策树搜索，先选后退，排列/组合/子集与树层去重*

- [ ] **LC 46**. [全排列 ↗](https://leetcode.cn/problems/permutations/) `[中等]` ｜ 📖 本地题解：[[06-LC0046-全排列]]
- [ ] **LC 78**. [子集 ↗](https://leetcode.cn/problems/subsets/) `[中等]`
- [ ] **LC 17**. [电话号码的字母组合 ↗](https://leetcode.cn/problems/letter-combinations-of-a-phone-number/) `[中等]`
- [ ] **LC 39**. [组合总和 ↗](https://leetcode.cn/problems/combination-sum/) `[中等]`
- [ ] **LC 22**. [括号生成 ↗](https://leetcode.cn/problems/generate-parentheses/) `[中等]`
- [ ] **LC 79**. [单词搜索 ↗](https://leetcode.cn/problems/word-search/) `[中等]`
- [ ] **LC 131**. [分割回文串 ↗](https://leetcode.cn/problems/palindrome-partitioning/) `[中等]`
- [ ] **LC 51**. [N 皇后 ↗](https://leetcode.cn/problems/n-queens/) `[困难]`

### 11. 二分查找 (Binary Search)
> 💡 *二段性本质，闭区间红蓝染色，首个满足与二分答案*

- [ ] **LC 35**. [搜索插入位置 ↗](https://leetcode.cn/problems/search-insert-position/) `[简单]`
- [ ] **LC 34**. [在排序数组中查找元素的第一个和最后一个位置 ↗](https://leetcode.cn/problems/find-first-and-last-position-of-element-in-sorted-array/) `[中等]`
- [ ] **LC 33**. [搜索旋转排序数组 ↗](https://leetcode.cn/problems/search-in-rotated-sorted-array/) `[中等]` ｜ 📖 本地题解：[[05-LC0033-搜索旋转排序数组]]
- [ ] **LC 153**. [寻找旋转排序数组中的最小值 ↗](https://leetcode.cn/problems/find-minimum-in-rotated-sorted-array/) `[中等]`
- [ ] **LC 4**. [寻找两个正序数组的中位数 ↗](https://leetcode.cn/problems/median-of-two-sorted-arrays/) `[困难]`

### 12. 栈与单调栈 (Stack)
> 💡 *括号匹配 LIFO，单调递增/递减栈寻找左右首个更小更大元素*

- [ ] **LC 20**. [有效的括号 ↗](https://leetcode.cn/problems/valid-parentheses/) `[简单]` ｜ 📖 本地题解：[[03-LC0020-有效的括号]]
- [ ] **LC 155**. [最小栈 ↗](https://leetcode.cn/problems/min-stack/) `[中等]`
- [ ] **LC 394**. [字符串解码 ↗](https://leetcode.cn/problems/decode-string/) `[中等]`
- [ ] **LC 739**. [每日温度 ↗](https://leetcode.cn/problems/daily-temperatures/) `[中等]`
- [ ] **LC 84**. [柱状图中最大的矩形 ↗](https://leetcode.cn/problems/largest-rectangle-in-histogram/) `[困难]`

### 13. 堆与优先队列 (Heap)
> 💡 *Top K 高频元素维护，对顶堆动态中位数计算*

- [ ] **LC 215**. [数组中的第K个最大元素 ↗](https://leetcode.cn/problems/kth-largest-element-in-an-array/) `[中等]` ｜ 📖 本地题解：[[17-LC0215-数组中的第K个最大元素]]
- [ ] **LC 347**. [前 K 个高频元素 ↗](https://leetcode.cn/problems/top-k-frequent-elements/) `[中等]`
- [ ] **LC 295**. [数据流的中位数 ↗](https://leetcode.cn/problems/find-median-from-data-stream/) `[困难]`

### 14. 贪心算法 (Greedy)
> 💡 *局部最优推出全局最优，跳跃覆盖与区间划分*

- [ ] **LC 121**. [买卖股票的最佳时机 ↗](https://leetcode.cn/problems/best-time-to-buy-and-sell-stock/) `[简单]` ｜ 📖 本地题解：[[12-LC0121-买卖股票的最佳时机]]
- [ ] **LC 55**. [跳跃游戏 ↗](https://leetcode.cn/problems/jump-game/) `[中等]`
- [ ] **LC 45**. [跳跃游戏 II ↗](https://leetcode.cn/problems/jump-game-ii/) `[中等]`
- [ ] **LC 763**. [划分字母区间 ↗](https://leetcode.cn/problems/partition-labels/) `[中等]`

### 15. 动态规划 (Dynamic Programming)
> 💡 *重叠子问题与状态转移，从一维线性、背包到二维坐标与最长子序列*

- [ ] **LC 70**. [爬楼梯 ↗](https://leetcode.cn/problems/climbing-stairs/) `[简单]` ｜ 📖 本地题解：[[09-LC0070-爬楼梯]]
- [ ] **LC 118**. [杨辉三角 ↗](https://leetcode.cn/problems/pascals-triangle/) `[简单]`
- [ ] **LC 198**. [打家劫舍 ↗](https://leetcode.cn/problems/house-robber/) `[中等]`
- [ ] **LC 279**. [完全平方数 ↗](https://leetcode.cn/problems/perfect-squares/) `[中等]`
- [ ] **LC 322**. [零钱兑换 ↗](https://leetcode.cn/problems/coin-change/) `[中等]` ｜ 📖 本地题解：[[20-LC0322-零钱兑换]]
- [ ] **LC 139**. [单词拆分 ↗](https://leetcode.cn/problems/word-break/) `[中等]`
- [ ] **LC 300**. [最长递增子序列 ↗](https://leetcode.cn/problems/longest-increasing-subsequence/) `[中等]` ｜ 📖 本地题解：[[19-LC0300-最长递增子序列]]
- [ ] **LC 152**. [乘积最大子数组 ↗](https://leetcode.cn/problems/maximum-product-subarray/) `[中等]`
- [ ] **LC 416**. [分割等和子集 ↗](https://leetcode.cn/problems/partition-equal-subset-sum/) `[中等]`
- [ ] **LC 32**. [最长有效括号 ↗](https://leetcode.cn/problems/longest-valid-parentheses/) `[困难]`
- [ ] **LC 62**. [不同路径 ↗](https://leetcode.cn/problems/unique-paths/) `[中等]`
- [ ] **LC 64**. [最小路径和 ↗](https://leetcode.cn/problems/minimum-path-sum/) `[中等]`
- [ ] **LC 5**. [最长回文子串 ↗](https://leetcode.cn/problems/longest-palindromic-substring/) `[中等]`
- [ ] **LC 647**. [回文子串 ↗](https://leetcode.cn/problems/palindromic-substrings/) `[中等]`
- [ ] **LC 1143**. [最长公共子序列 ↗](https://leetcode.cn/problems/longest-common-subsequence/) `[中等]`
- [ ] **LC 72**. [编辑距离 ↗](https://leetcode.cn/problems/edit-distance/) `[困难]`

### 16. 技巧与核心杂项 (Techniques & Misc)
> 💡 *位运算消除、摩尔投票法、下一个排列与快慢指针判圈*

- [ ] **LC 136**. [只出现一次的数字 ↗](https://leetcode.cn/problems/single-number/) `[简单]`
- [ ] **LC 169**. [多数元素 ↗](https://leetcode.cn/problems/majority-element/) `[简单]`
- [ ] **LC 75**. [颜色分类 ↗](https://leetcode.cn/problems/sort-colors/) `[中等]`
- [ ] **LC 31**. [下一个排列 ↗](https://leetcode.cn/problems/next-permutation/) `[中等]`
- [ ] **LC 287**. [寻找重复数 ↗](https://leetcode.cn/problems/find-the-duplicate-number/) `[中等]`

---

## 🔗 相关导航
- 仪表盘看板：[[../00-总览/学习仪表盘|学习仪表盘]]
- 通用算法模板：[[../04-模板与技巧/_index|04 · 模板与实用技巧]]
- 求职手撕白板：[[../../40-求职准备/16-面试-手撕代码|40 · 面试白板手撕代码]]
