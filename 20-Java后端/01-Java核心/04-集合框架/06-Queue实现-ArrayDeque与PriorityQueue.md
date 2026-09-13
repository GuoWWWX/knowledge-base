---
title: Queue实现-ArrayDeque与PriorityQueue
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
# Queue实现-ArrayDeque与PriorityQueue

> [!abstract] 一句话总结
> 队列家族就两个常用实现：ArrayDeque 用**循环数组**实现双端队列（当队列/栈用，替代 Stack 和 LinkedList 的队列用法），PriorityQueue 用**二叉堆**实现"按优先级出队"（TopK、任务调度）——本课把循环数组的索引魔法和堆的上浮/下沉源码贴出来讲。

## 🎯 本节定位

- **本章第 9 课（集合域第 6 篇），集合实现篇（五，收尾）**。前置：[[01-核心机制与选型]]、[[02-List实现-ArrayList与LinkedList]]（ArrayDeque 与 ArrayList/LinkedList 的对比）。
- **延伸阅读**：堆排序与 TopK 算法的完整推导在 [[../../../30-算法与数据结构/01-数据结构/08-堆与优先队列|算法章节]]；阻塞队列（BlockingQueue）与线程池队列在 [[../../03-并发编程/07-线程池原理与参数|03-并发编程章节]]。

## 📌 这是什么

| 实现 | 底层 | 出队顺序 | 允许 null | 复杂度 | 使用场景 |
| ---- | ---- | ---- | ---- | ---- | ---- |
| **ArrayDeque** | 循环数组 | FIFO（当队列）/ LIFO（当栈） | **不允许** | 头尾操作 O(1) | 默认队列、默认栈 |
| **PriorityQueue** | 二叉堆 | 按优先级（默认最小先出） | **不允许** | 入/出 O(log n) | TopK、按优先级处理 |

**一句话记住**：需要"先进先出"或"先进后出"用 ArrayDeque；需要"谁最急谁先出"用 PriorityQueue。

## 💻 怎么用（常用 API 速览）

```java
// ===== 当队列用（FIFO）=====
Deque<String> queue = new ArrayDeque<>();
queue.offer("a");              // 入队（尾）——offer 失败返回 false，不抛异常
queue.offer("b");
queue.peek();                  // 看队首不弹出；空返回 null
queue.poll();                  // 出队（头）；空返回 null
// 严格版（失败抛异常）：add / element / remove

// ===== 当栈用（LIFO）=====
Deque<String> stack = new ArrayDeque<>();
stack.push("a");               // 入栈
stack.push("b");
stack.peek();                  // 看栈顶："b"
stack.pop();                   // 出栈："b"

// ===== PriorityQueue：按优先级出队 =====
Queue<Integer> pq = new PriorityQueue<>();                       // 小顶堆：最小先出
Queue<Integer> pq2 = new PriorityQueue<>(Comparator.reverseOrder());  // 大顶堆：最大先出
	pq.offer(5); pq.offer(1); pq.offer(3);
pq.poll();                     // 1（小顶堆）
pq.peek();                     // 看堆顶（当前最小）

// 自定义优先级：任务按 deadline 排序
Queue<Task> tasks = new PriorityQueue<>(Comparator.comparing(Task::deadline));

// ===== 经典场景：TopK（前 K 个最大/最小）=====
// 求最大的 K 个 → 维护大小为 K 的小顶堆，比堆顶大就替换
Queue<Integer> topK = new PriorityQueue<>(K);          // 小顶堆，堆顶是第 K 大
for (int x : data) {
    if (topK.size() < K) topK.offer(x);
    else if (x > topK.peek()) { topK.poll(); topK.offer(x); }
}
// 结果：topK 里就是最大的 K 个，peek() 是第 K 大
```

**⚠️ 高频坑**：ArrayDeque 和 PriorityQueue **都不允许 null**（offer(null) 抛 NPE）；PriorityQueue 元素需可比较（Comparable/Comparator）；不要用遗留 `Stack`（继承 Vector，全方法同步）；`LinkedList` 也能当队列但节点开销大，双端操作用 ArrayDeque。

## 🔍 关键源码（贴出逐段讲）

### ArrayDeque：循环数组，头尾都是 O(1)

```java
// ArrayDeque 核心字段：
// transient Object[] elements;   // 循环数组，长度恒为 2 的幂
// transient int head;            // 队首下标
// transient int tail;            // 队尾下标（下一个可写位置）

// 入队（尾）：tail 位置写入后"绕回"
public void addLast(E e) {
    if (e == null) throw new NullPointerException();   // ① 拒绝 null
    elements[tail] = e;                                // ② 写入尾位置
    tail = (tail + 1) & (elements.length - 1);         // ③ 尾指针+1，满则绕回 0（& 是因为长度是 2 的幂）
    if (tail == head) doubleCapacity();                // ④ 队满 → 扩容翻倍
}

// 出队（头）：取 head 位置后"绕回"
public E pollFirst() {
    int h = head;
    E result = (E) elements[h];
    if (result != null) {
        elements[h] = null;                            // 置空防内存泄漏
        head = (h + 1) & (elements.length - 1);        // 头指针+1，绕回
    }
    return result;
}
```

**逐点看懂**：
- **循环数组**：物理上是一个数组，逻辑上首尾相连——`head/tail` 递增到末尾就"绕"回 0（`& (len-1)` 等价于取模，因为长度是 2 的幂）。**数组不搬移、不浪费**，头尾操作都 O(1)。
- **为什么不允许 null**：源码用 `elements[h] == null` 判断"这个位置是否为空"，如果允许存 null 就无法区分"空位"和"null 元素"——这是**实现层面**的原因，面试加分点。
- **扩容**：队满（tail 追平 head）时 `doubleCapacity` 翻倍并把元素按顺序搬到新数组（O(n)，均摊后入队仍 O(1)）。
- **为什么替代 Stack/LinkedList**：Stack 全方法同步（慢）、LinkedList 有节点开销；ArrayDeque 数组连续、缓存友好、无同步——"默认栈/队列"就是它。

### PriorityQueue：二叉堆，上浮/下沉

```java
// PriorityQueue 核心字段：
// transient Object[] queue;          // 堆数组（下标从 0 开始）
// private final Comparator<? super E> comparator;   // null 则用自然序

// 入队：先放数组尾部，再"上浮"（siftUp）到合适位置
private void siftUpComparable(int k, E x) {       // k = 新元素下标（尾部）
    Comparable<? super E> key = (Comparable<? super E>) x;
    while (k > 0) {
        int parent = (k - 1) >>> 1;               // ① 父节点下标 = (k-1)/2
        Object e = queue[parent];
        if (key.compareTo((E) e) >= 0) break;     // ② 比父节点大/等 → 位置对了，停
        queue[k] = e;                             // ③ 父节点下沉
        k = parent;                               // ④ 继续向上比较
    }
    queue[k] = key;
}
// 出队：取堆顶（最小），把尾部元素放堆顶，再"下沉"（siftDown）恢复堆性质
```

**逐点看懂**：
- **二叉堆**：数组表示的完全二叉树，`queue[0]` 是堆顶（最小/最大）。父子关系纯靠下标：`parent = (k-1)/2`，`left = 2k+1`，`right = 2k+2`——不需要任何指针。
- **为什么入/出 O(log n)**：上浮/下沉每次走一层，树高 log₂n。
- **小顶堆 vs 大顶堆**：默认（自然序）是小顶堆——`compareTo` 返回负数的（更小）在上；传 `Comparator.reverseOrder()` 就是大顶堆。
- **为什么不允许 null**：插入第一件事就是 `compareTo`，null 无法比较 → NPE。
- **迭代顺序 ≠ 出队顺序**：`pq.iterator()` 是数组顺序（堆顺序），不是优先级顺序；要按序取只能 poll。

## 🎯 面试问题

> [!question] 高频问题
> - Q: ArrayDeque 和 LinkedList 都能当队列，选哪个？
>   - A: ArrayDeque：循环数组、连续内存缓存友好、头尾 O(1)、无节点开销；LinkedList：节点分散、内存 3 倍。双端队列/栈一律 ArrayDeque。
> - Q: 为什么 ArrayDeque 不允许 null？Stack 为什么不用？
>   - A: ArrayDeque 源码用"元素是否为 null"判断槽位空/满，存 null 会破坏该逻辑（实现层面）；Stack 是 JDK 1.0 遗留、全方法 synchronized 性能差，用 ArrayDeque 替代。
> - Q: PriorityQueue 底层是什么？复杂度？
>   - A: 二叉堆（数组实现），入队 siftUp 上浮、出队 siftDown 下沉，都是 O(log n)；默认小顶堆，传 Comparator.reverseOrder() 变大顶堆。
> - Q: 怎么用 PriorityQueue 求 TopK？
>   - A: 求最大 K 个用大小为 K 的小顶堆：比堆顶大就替换（poll+offer），堆顶就是第 K 大；时间复杂度 O(n log K)。
> - Q: PriorityQueue 能放 null 吗？迭代顺序是优先级顺序吗？
>   - A: 不能，插入要 compareTo，null 会 NPE。迭代顺序是数组（堆）顺序不是优先级顺序，要按序取只能 poll。

## 🛠️ 排查 / 实践要点

- "栈"不要用 Stack、队列不要用 LinkedList 当默认：`new ArrayDeque<>()` 一把梭。
- TopK/求中位数/任务按优先级调度：PriorityQueue；注意 O(log n) 的入出队成本。
- PriorityQueue 的 `contains` 是 O(n)（数组线性扫），别在堆上做频繁包含判断。
- 阻塞队列（生产者消费者）是并发章节的 BlockingQueue，别用 PriorityQueue 当线程间队列。

## ⚠️ 踩坑与注意事项

- `offer(null)` / `add(null)` → NPE（两个实现都不允许）。
- PriorityQueue 元素不可比 → ClassCastException。
- 用 `peek()` 取堆顶前不判空：空堆返回 null，`poll()` 同理。
- 以为 PriorityQueue 迭代有序 → 迭代顺序是堆数组顺序，不是优先级序。
- 用 LinkedList 当栈/队列（性能与内存都不如 ArrayDeque）。
- 单线程场景用线程安全队列（ArrayBlockingQueue 等）→ 白付同步开销。

## 🧪 最小验证

```java
// 1. 循环数组绕回（可断点观察 head/tail）
ArrayDeque<Integer> dq = new ArrayDeque<>();
for (int i = 0; i < 8; i++) dq.addLast(i);
System.out.println(dq.pollFirst());   // 0
dq.addLast(8);                        // tail 绕回

// 2. 小顶堆 vs 大顶堆
Queue<Integer> min = new PriorityQueue<>(List.of(5, 1, 3));
Queue<Integer> max = new PriorityQueue<>(Comparator.reverseOrder());
max.addAll(List.of(5, 1, 3));
System.out.println(min.poll());   // 1
System.out.println(max.poll());   // 5

// 3. 迭代顺序 ≠ 优先级顺序
System.out.println(min);   // 可能输出 [3, 5, ...]（堆数组序）——别依赖！

// 4. null 检查
// dq.offer(null);   // NullPointerException
```

生产排障：队列用 ArrayDeque；"最急先出"用 PriorityQueue；涉及多线程队列换 BlockingQueue（并发章节）；TopK 类需求先想堆。

## ✅ 精通检查清单

- 能讲清 ArrayDeque 循环数组的 head/tail 绕回机制与"为什么不允许 null"。
- 能讲清 PriorityQueue 的 siftUp/siftDown 与 O(log n) 来源。
- 能写出 TopK 的堆解法代码。
- 能答出"Stack 为什么不用""迭代顺序不是优先级顺序"等面试题。

## 🔗 关联笔记

- 本课内：总纲 [[01-核心机制与选型]]；对比 [[02-List实现-ArrayList与LinkedList]]
- 延伸：[[../../../30-算法与数据结构/01-数据结构/08-堆与优先队列|堆与优先队列]]（算法章节）；[[../../03-并发编程/07-线程池原理与参数|线程池与阻塞队列]]（并发章节）

## 📚 参考资料

- [JDK 源码 ArrayDeque.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/ArrayDeque.java)
- [JDK 源码 PriorityQueue.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/PriorityQueue.java)
