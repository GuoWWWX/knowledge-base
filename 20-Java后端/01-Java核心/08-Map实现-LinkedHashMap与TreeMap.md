---
title: Map实现-LinkedHashMap与TreeMap
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Java核心
  - 阶段/01
created: 2026-08-03
updated: 2026-08-03
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# Map实现-LinkedHashMap与TreeMap

> [!abstract] 一句话总结
> HashMap 之外的两个 Map 变体解决"顺序"问题：LinkedHashMap 用**额外双向链表**保住插入序/访问序（也是 LRU 缓存的官方底座），TreeMap 用**红黑树**让 key 有序且支持范围查询——本课把"多出来的那部分源码"贴出来讲，HashMap 本身的哈希机制见 [[07-HashMap原理与扩容]]。

## 🎯 本节定位

- **本章第 8 课，集合实现篇（四）**。前置：[[07-HashMap原理与扩容]]（两者的哈希存储部分完全复用 HashMap，本课只讲**增量**：链表/红黑树）、[[04-集合框架-核心机制与选型]]。
- 与 [[06-Set实现-HashSet与TreeSet]] 互为镜像：LinkedHashSet/TreeSet 的底层就是本课的 Map。
- **延伸阅读**：红黑树旋转/染色完整原理在 [[30-算法与数据结构/01-数据结构/07-平衡树-AVL-红黑树|算法章节]]，本课只讲"为什么有序、为什么 O(log n)"。

## 📌 这是什么

| 实现 | 结构 | 顺序 | 允许 null | 复杂度 | 使用场景 |
| ---- | ---- | ---- | ---- | ---- | ---- |
| HashMap | 数组+链表/红黑树 | 无序 | key/value 均可 | O(1) 平均 | 默认键值（07 课） |
| **LinkedHashMap** | HashMap + **双向链表** | 插入序 / 访问序 | 允许 | O(1) 平均 | 保序 Map、LRU 缓存 |
| **TreeMap** | **红黑树** | 按 key 排序 | key 不允许 | O(log n) | 排序、范围查询 |

## 💻 怎么用（常用 API 速览）

```java
// ===== LinkedHashMap：保插入序 =====
Map<String, Integer> map = new LinkedHashMap<>();
map.put("b", 2); map.put("a", 1); map.put("c", 3);
// 遍历顺序 = 插入顺序：[b, a, c]（HashMap 不保证任何顺序）

// ===== TreeMap：按 key 排序 =====
TreeMap<String, Integer> tm = new TreeMap<>();
tm.put("b", 2); tm.put("a", 1); tm.put("c", 3);
tm.firstKey(); tm.lastKey();              // a / c
// 遍历顺序 = key 升序：[a, b, c]
// 自定义排序：new TreeMap<>(Comparator.reverseOrder())

// ===== TreeMap 范围查询（高频场景）=====
tm.floorKey("b"); tm.ceilingKey("b");     // <=b 的最大 / >=b 的最小
tm.lowerKey("b"); tm.higherKey("b");      // 严格小于 / 严格大于
tm.subMap("a", true, "c", false);         // [a, c) 子视图（视图！原 map 改会同步）
tm.tailMap("b");                          // key >= b 的部分
tm.headMap("b");                          // key < b 的部分

// ===== LinkedHashMap 做 LRU 缓存（完整代码，背下来）=====
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    LRUCache(int capacity) {
        super(capacity, 0.75f, true);   // ③ accessOrder=true：开启访问序
        this.capacity = capacity;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;       // ④ 超过容量就淘汰最久未访问的
    }
}
LRUCache<String, String> cache = new LRUCache<>(2);
cache.put("a", "1"); cache.put("b", "2");
cache.get("a");                          // 访问 a → a 变"最新"
cache.put("c", "3");                     // 淘汰最久未访问的 b
System.out.println(cache.containsKey("b"));  // false
```

## 🔍 关键源码（贴出逐段讲）

### LinkedHashMap：HashMap + 双向链表 = 顺序

```java
// LinkedHashMap 的节点比 HashMap.Node 多了 before/after 两个指针：
// static class Entry<K,V> extends HashMap.Node<K,V> {
//     Entry<K,V> before, after;   // 串成双向链表
// }
// transient LinkedHashMap.Entry<K,V> head, tail;   // 链表头尾

// HashMap 的三个"回调钩子"（HashMap 里是空方法，LinkedHashMap 覆写）：
// putVal 插入成功后会调用 afterNodeAccess(e) 和 afterNodeInsertion(evict)
// get 命中后会调用 afterNodeAccess(e)

// ① 插入序：每次插入把节点移到链表尾
void afterNodeInsertion(boolean evict) {
    LinkedHashMap.Entry<K,V> first = head;
    if (evict && first != null && removeEldestEntry(first)) {
        // ④ 若 removeEldestEntry 返回 true → 删除链表头的"最老"节点
        K key = first.key;
        removeNode(hash(key), key, null, false, true);
    }
}

// ② 访问序（accessOrder=true 时）：每次 get 把节点移到链表尾
void afterNodeAccess(Node<K,V> e) {
    LinkedHashMap.Entry<K,V> last;
    if (accessOrder && (last = tail) != e) {
        // 把 e 从链表中间摘下来，接到 tail（"最近使用"）
    }
}
```

**逐点看懂**：
- **为什么能保插入序**：每次 `put` 后 `afterNodeInsertion` 把新节点挂到链表尾——遍历走链表 = 插入顺序。
- **为什么能做 LRU**：`accessOrder=true` 时每次 `get` 把命中节点移到链表尾 → **链表头永远是最久未访问的**；`removeEldestEntry` 返回 true 时 `afterNodeInsertion` 删链表头 → 天然 LRU 淘汰。只需要覆写一个方法，**JDK 官方实现的 LRU 就是这段代码**。
- **为什么复杂度仍是 O(1)**：链表操作是 O(1)，哈希查找是 O(1)，增量不改变复杂度。
- 注意：`removeEldestEntry` 默认返回 false（不淘汰），覆写时才启用——这是模板方法模式。

### TreeMap：红黑树，有序 + O(log n) 的来源

```java
// TreeMap 核心结构：
// private transient Entry<K,V> root;   // 红黑树根节点
// static final class Entry<K,V> {
//     K key; V value;
//     Entry<K,V> left, right, parent;
//     boolean color = BLACK;           // 红黑树节点带颜色
// }
// private final Comparator<? super K> comparator;  // 为 null 则用 key 的自然序

// put 的主流程（简化为三步）：
// ① 从 root 开始按 comparator.compare 比较，往左/往右走，找到插入位置（O(log n)）
// ② 挂上新节点（默认红色）
// ③ fixAfterInsertion：旋转 + 变色，恢复红黑树性质（O(log n)）
```

**逐点看懂**：
- **为什么有序**：红黑树是二叉搜索树——左子树 < 根 < 右子树；遍历走**中序遍历**（左→根→右）就是 key 升序。
- **为什么 O(log n)**：红黑树保证树高 ≤ 2·log₂(n+1)（通过旋转/染色维持平衡），插入、查找、删除都从根走树高步。
- **为什么不允许 null key**：插入第一步就要 `compare(key, existingKey)`，`null` 无法参与比较 → 抛 NPE。
- **为什么常量比 HashMap 大**：树节点有 left/right/parent/color 4 个额外字段 + 每次操作走 log n 层比较；数据量小时 HashMap 快得多，**只有明确要排序/范围查询才用 TreeMap**。

## 🎯 面试问题

> [!question] 高频问题
> - Q: LinkedHashMap 怎么实现有序的？
>   - A: 在 HashMap 基础上给每个节点加 before/after 指针串成双向链表，put 后 afterNodeInsertion 把节点挂到链表尾，遍历走链表即插入序。
> - Q: 怎么用 LinkedHashMap 实现 LRU？
>   - A: 构造传 accessOrder=true（访问序），覆写 removeEldestEntry 返回 size()>capacity。get 命中节点被移到链表尾，链表头永远最久未访问，超容量即淘汰。
> - Q: HashMap 和 TreeMap 的区别？
>   - A: HashMap 哈希表：无序、O(1)、允许 null；TreeMap 红黑树：按 key 排序、O(log n)、不允许 null key、支持范围查询（floor/subMap）。要排序才用 TreeMap。
> - Q: TreeMap 为什么是 O(log n)？怎么保证平衡？
>   - A: 底层红黑树，插入后 fixAfterInsertion 旋转+变色维持平衡，树高 ≤ 2·log₂(n+1)，操作都从根走树高步。旋转细节在算法章节。
> - Q: TreeMap 为什么不允许 null key？（HashMap 却允许）
>   - A: TreeMap 插入时要调用 compareTo/compare 比较 key，null 无法比较会 NPE；HashMap 用 hash 定位桶，null 有固定位置（table[0]）所以允许。
> - Q: subMap 返回的是什么？
>   - A: 视图（不是拷贝）：原 map 的结构修改会反映到视图，遍历视图时可能抛 ConcurrentModificationException。

## 🛠️ 排查 / 实践要点

- 遍历顺序敏感的业务（报表、展示顺序）用 LinkedHashMap，别依赖 HashMap 的"恰好顺序"。
- 需要"最近最少使用"淘汰的本地缓存：LinkedHashMap 覆写 removeEldestEntry（注意：本地缓存要做容量上限与并发保护，见系统设计章节）。
- 需要区间统计/排序输出：TreeMap 的 subMap/tailMap/floorKey 一套 API 直接给答案。
- 数据量大且只需要"有序"但不需要范围查询：考虑排序后放 ArrayList（一次排序 O(n log n) 比持续维护树便宜）。

## ⚠️ 踩坑与注意事项

- 把 HashMap 迭代顺序当稳定行为 → 数据换了哈希分布就变序，要保序显式用 LinkedHashMap。
- `accessOrder` 构造参数忘了传 true → 只是保插入序，不是 LRU。
- TreeMap 的 key 不可比（无 Comparable 且没传 Comparator）→ ClassCastException。
- 往 TreeMap 放 null key → NPE。
- subMap 视图修改原 map 后遍历 → ConcurrentModificationException。
- LRU 缓存不设容量上限（removeEldestEntry 永远 false）→ 无限增长 OOM。

## 🧪 最小验证

```java
// 1. 插入序 vs 访问序
Map<String, Integer> ins = new LinkedHashMap<>();
ins.put("a", 1); ins.put("b", 2); ins.put("c", 3);
System.out.println(ins.keySet());          // [a, b, c]（插入序）

Map<String, Integer> acc = new LinkedHashMap<>(16, 0.75f, true);
acc.put("a", 1); acc.put("b", 2); acc.put("c", 3);
acc.get("a");                              // 访问 a
System.out.println(acc.keySet());          // [b, c, a]（a 被移到尾部）

// 2. LRU 淘汰
LRUCache<String, String> cache = new LRUCache<>(2);   // 见上文完整代码
cache.put("a", "1"); cache.put("b", "2");
cache.get("a");
cache.put("c", "3");
System.out.println(cache.keySet());        // [a, c]（b 被淘汰）

// 3. TreeMap 范围查询
TreeMap<Integer, String> tm = new TreeMap<>();
tm.put(1, "a"); tm.put(5, "e"); tm.put(9, "i");
System.out.println(tm.subMap(2, true, 8, true));   // {5=e}
System.out.println(tm.floorKey(6));                // 5
```

生产排障：Map 遍历顺序不对 → 换 LinkedHashMap；需要排序/区间 → TreeMap；需要淘汰策略 → LinkedHashMap 覆写 removeEldestEntry 并注意并发与容量。

## ✅ 精通检查清单

- 能讲清 LinkedHashMap 保序与 LRU 的源码机制（afterNodeInsertion/afterNodeAccess/removeEldestEntry）。
- 能默写一个 LRU 缓存的最小实现。
- 能讲清 TreeMap 有序/O(log n)/不允许 null 的因果。
- 能答出 HashMap vs TreeMap、subMap 视图等面试题。

## 🔗 关联笔记

- 本课内：总纲 [[04-集合框架-核心机制与选型]]；哈希机制 [[07-HashMap原理与扩容]]；Set 镜像 [[06-Set实现-HashSet与TreeSet]]
- 延伸：[[30-算法与数据结构/01-数据结构/07-平衡树-AVL-红黑树|红黑树原理]]（算法章节）

## 📚 参考资料

- [JDK 源码 LinkedHashMap.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/LinkedHashMap.java)
- [JDK 源码 TreeMap.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/TreeMap.java)
