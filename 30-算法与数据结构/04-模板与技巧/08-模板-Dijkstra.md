---
title: 模板-Dijkstra
tags: [领域/算法, 类型/模板, 算法/最短路]
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# 模板-Dijkstra

> [!abstract] 一句话总结
> Dijkstra 用优先队列每次扩展当前距离最短的节点，解决非负权图单源最短路。

## Java 模板

```java
import java.util.*;

class DijkstraTemplate {
    static class Edge {
        int to;
        int w;

        Edge(int to, int w) {
            this.to = to;
            this.w = w;
        }
    }

    int[] dijkstra(List<Edge>[] graph, int src) {
        int n = graph.length;
        int inf = Integer.MAX_VALUE / 2;
        int[] dist = new int[n];
        Arrays.fill(dist, inf);
        dist[src] = 0;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[] {src, 0});

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int u = cur[0], d = cur[1];
            if (d != dist[u]) continue;

            for (Edge e : graph[u]) {
                if (dist[e.to] > d + e.w) {
                    dist[e.to] = d + e.w;
                    pq.offer(new int[] {e.to, dist[e.to]});
                }
            }
        }
        return dist;
    }
}
```

## 使用条件

- 边权不能为负。
- 图用邻接表存储。
- 稠密图可考虑朴素 Dijkstra。

## 关联

- [[../01-数据结构/09-图的表示]]
- [[../01-数据结构/08-堆与优先队列]]

## 📚 参考资料

- 《算法导论》：数据结构、排序、图算法、动态规划等基础章节。
- LeetCode 官方题库与 Explore：按专题训练对应题型。
- CP-Algorithms：算法模板、复杂度与竞赛实现参考。

