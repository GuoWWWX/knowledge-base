---
title: BFS广度优先
tags: [领域/算法, 类型/知识, 算法/BFS]
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# BFS 广度优先

> [!abstract] 一句话总结
> BFS 按层扩展状态，适合无权图最短路、层序遍历和最少步数问题。

## 模板

```java
Queue<Integer> q = new ArrayDeque<>();
q.offer(start);
visited[start] = true;
int step = 0;
while (!q.isEmpty()) {
    int size = q.size();
    for (int i = 0; i < size; i++) {
        int u = q.poll();
        for (int v : graph[u]) {
            if (visited[v]) continue;
            visited[v] = true;
            q.offer(v);
        }
    }
    step++;
}
```

## 精通重点

- BFS 第一次到达某状态通常就是最短步数。
- 状态可能不只是节点，也可能是 `(位置,钥匙集合)` 这类复合状态。
- 入队时标记 visited，避免重复入队。

## 常见变体

### 多源 BFS

多个起点同时入队，适合“离最近的 0/陆地/腐烂橘子距离”这类题。

```java
for (int s : starts) {
    q.offer(s);
    visited[s] = true;
}
```

### 双向 BFS

起点和终点同时扩展，每次扩展较小的集合，适合状态空间大但目标明确的问题，如单词接龙。

### 0-1 BFS

边权只有 0 和 1 时，用双端队列：权重 0 的状态放队头，权重 1 的状态放队尾。

## 复杂度

- 邻接表无权图：时间 O(V + E)，空间 O(V)。
- 网格 BFS：时间 O(mn)，空间 O(mn)。
- 状态压缩 BFS：复杂度等于“位置数 × 状态数”，要先估算是否可跑。

## 关联

- [[../03-刷题记录/11-LC0102-二叉树层序遍历]]
- [[06-DFS深度优先]]

## 📚 参考资料

- 《算法导论》：数据结构、排序、图算法、动态规划等基础章节。
- LeetCode 官方题库与 Explore：按专题训练对应题型。
- CP-Algorithms：算法模板、复杂度与竞赛实现参考。

