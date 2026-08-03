---
title: 字典树Trie
tags: [领域/算法, 类型/知识, 数据结构/Trie]
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# 字典树 Trie

> [!abstract] 一句话总结
> Trie 用树路径表示字符串前缀，适合前缀查询、自动补全和字符串集合匹配。

## 节点结构

```java
class TrieNode {
    TrieNode[] children = new TrieNode[26];
    boolean end;
}
```

## 插入与查询

```java
void insert(String word) {
    TrieNode cur = root;
    for (char c : word.toCharArray()) {
        int i = c - 'a';
        if (cur.children[i] == null) cur.children[i] = new TrieNode();
        cur = cur.children[i];
    }
    cur.end = true;
}
```

查询完整单词和查询前缀要分开：

```java
boolean search(String word) {
    TrieNode node = find(word);
    return node != null && node.end;
}

boolean startsWith(String prefix) {
    return find(prefix) != null;
}
```

## 适用场景

- 判断单词是否存在。
- 判断前缀是否存在。
- 单词搜索、敏感词过滤。
- 最大异或值可用 0/1 Trie。

## 复杂度

- 插入、查询时间是 O(L)，`L` 是字符串长度。
- 空间取决于字符集大小和节点数量；数组 children 查询快但占空间，`Map` children 更灵活。

## 面试讲法

Trie 的本质是用“路径共享”节省公共前缀。它不是替代哈希表做普通精确查询，而是在前缀查询、批量匹配、字典搜索里更有优势。

## 易错点

- `end` 表示完整单词结束，不等于有前缀。
- 字符集不一定只有小写字母，工程中可用 `Map<Character, TrieNode>`。
- 删除单词要考虑是否还能被其他单词共享前缀，不能粗暴删除整条路径。

## 关联

- [[09-图的表示]]
- [[../02-算法思想/06-DFS深度优先]]

## 📚 参考资料

- 《算法导论》：数据结构、排序、图算法、动态规划等基础章节。
- LeetCode 官方题库与 Explore：按专题训练对应题型。
- CP-Algorithms：算法模板、复杂度与竞赛实现参考。

