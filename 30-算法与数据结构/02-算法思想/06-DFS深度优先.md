---
title: DFS深度优先
tags: [领域/算法, 类型/知识, 算法/DFS]
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# DFS 深度优先

> [!abstract] 一句话总结
> DFS 沿一条路径走到底再回退，适合连通块、路径搜索、树遍历和回溯。

## 模板

```java
void dfs(int u) {
    visited[u] = true;
    for (int v : graph[u]) {
        if (!visited[v]) dfs(v);
    }
}
```

## 网格 DFS

```java
void dfs(int r, int c) {
    if (!inArea(r, c) || grid[r][c] != '1') return;
    grid[r][c] = '0';
    for (int[] d : dirs) dfs(r + d[0], c + d[1]);
}
```

## 适用场景

- 连通块统计：岛屿数量、省份数量。
- 路径搜索：从起点到终点是否存在路径。
- 树/图遍历：前序、中序、后序、拓扑相关搜索。
- 回溯枚举：排列、组合、子集。

## 递归栈写法

当图很深时，递归 DFS 可能栈溢出，可以改成显式栈。

```java
Deque<Integer> stack = new ArrayDeque<>();
stack.push(start);
visited[start] = true;
while (!stack.isEmpty()) {
    int u = stack.pop();
    for (int v : graph[u]) {
        if (visited[v]) continue;
        visited[v] = true;
        stack.push(v);
    }
}
```

## 复杂度

- 邻接表图 DFS：时间 O(V + E)，空间 O(V)。
- 网格 DFS：时间 O(mn)，空间最坏 O(mn)。
- 回溯类 DFS 的复杂度通常是指数级，要结合分支数和递归深度说明。

## 易错点

- 访问标记要在入栈/进入递归时做，避免重复。
- 递归深度过大可能栈溢出，可改迭代。
- 网格题注意边界和是否允许斜向。
- 如果题目需要恢复现场，离开递归前要撤销选择；普通连通块染色则不用恢复。

## 关联

- [[../03-刷题记录/15-LC0200-岛屿数量]]
- [[07-BFS广度优先]]

## 📚 参考资料

- 《算法导论》：数据结构、排序、图算法、动态规划等基础章节。
- LeetCode 官方题库与 Explore：按专题训练对应题型。
- CP-Algorithms：算法模板、复杂度与竞赛实现参考。

