---
title: 模板-DFS-BFS
tags: [领域/算法, 类型/模板, 算法/DFS, 算法/BFS]
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# 模板-DFS-BFS

> [!abstract] 一句话总结
> DFS 适合深度搜索和连通块，BFS 适合层序遍历和无权最短路。

## DFS 网格模板

```java
class GridDfsTemplate {
    private final int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    void dfs(char[][] grid, int r, int c) {
        if (r < 0 || r >= grid.length || c < 0 || c >= grid[0].length || grid[r][c] != '1') {
            return;
        }
        grid[r][c] = '0';
        for (int[] d : dirs) {
            dfs(grid, r + d[0], c + d[1]);
        }
    }
}
```

## BFS 模板

```java
import java.util.*;

class GridBfsTemplate {
    private final int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    int shortestPath(boolean[][] blocked, int sr, int sc, int tr, int tc) {
        int m = blocked.length, n = blocked[0].length;
        boolean[][] visited = new boolean[m][n];
        Queue<int[]> q = new ArrayDeque<>();
        q.offer(new int[] {sr, sc});
        visited[sr][sc] = true;
        int step = 0;

        while (!q.isEmpty()) {
            int size = q.size();
            for (int i = 0; i < size; i++) {
                int[] cur = q.poll();
                if (cur[0] == tr && cur[1] == tc) return step;
                for (int[] d : dirs) {
                    int nr = cur[0] + d[0], nc = cur[1] + d[1];
                    if (nr >= 0 && nr < m && nc >= 0 && nc < n && !blocked[nr][nc] && !visited[nr][nc]) {
                        visited[nr][nc] = true;
                        q.offer(new int[] {nr, nc});
                    }
                }
            }
            step++;
        }
        return -1;
    }
}
```

## 选择建议

- 求所有方案/连通块：DFS。
- 求最短步数/按层输出：BFS。
- 图有权重：考虑 Dijkstra。

## 关联

- [[../02-算法思想/06-DFS深度优先]]
- [[../02-算法思想/07-BFS广度优先]]

## 📚 参考资料

- 《算法导论》：数据结构、排序、图算法、动态规划等基础章节。
- LeetCode 官方题库与 Explore：按专题训练对应题型。
- CP-Algorithms：算法模板、复杂度与竞赛实现参考。

