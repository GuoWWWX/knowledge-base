---
title: List实现-ArrayList与LinkedList
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

# List实现-ArrayList与LinkedList

> [!abstract] 一句话总结
> List 的两个实现代表两种数据结构哲学：ArrayList 用连续数组换"按下标随机访问 O(1)、缓存友好"，LinkedList 用节点链换"已定位节点的插删 O(1)"——但定位本身要 O(n)，所以业务默认 ArrayList；本课把两者的关键源码贴出来逐段讲，面试直接能用。

## 🎯 本节定位

- **本章第 5 课，集合实现篇（一）**。前置：[[04-集合框架-核心机制与选型]]（接口层次、modCount/fail-fast、契约）。
- 后续：[[06-Set实现-HashSet与TreeSet]] · [[07-HashMap原理与扩容]] · [[08-Map实现-LinkedHashMap与TreeMap]] · [[09-Queue实现-ArrayDeque与PriorityQueue]]。
- **延伸阅读**：数组扩容与 `System.arraycopy` 的性能细节属于 JVM/系统层面，本课只讲集合视角。

## 📌 这是什么

- **ArrayList**：底层 `Object[] elementData`，按下标访问 O(1)，尾部追加摊还 O(1)，中间插入/删除 O(n)（搬移元素）。
- **LinkedList**：双向链表（`Node<E>` 持有 prev/next/item），按下标访问 O(n)（从头/尾遍历），头尾操作 O(1)。
- 两者都允许 null、允许重复、保序（按下标）、不是线程安全。

## 💻 怎么用（常用 API 速览）

```java
// ===== 创建 =====
List<String> list = new ArrayList<>();              // 默认容量 10（懒创建）
List<String> list = new ArrayList<>(1000);          // 预估容量：大批量 add 前必做
List<String> list = new ArrayList<>(otherSet);      // 从任意 Collection 复制
List<String> ll = new LinkedList<>(otherSet);

// ===== 增删改查 =====
list.add("a");              // 尾部追加，摊还 O(1)
list.add(0, "x");           // 指定位置插入，O(n)（后面元素整体后移）
list.get(0);                // 按下标查，O(1) —— ArrayList 的招牌
list.set(0, "b");           // 修改
list.remove(0);             // 按下标删，O(n)
list.remove("a");           // 按值删（equals 匹配第一个），O(n)
list.contains("a");         // O(n)！循环里调用要警惕 → 换 HashSet（见 06 课）
list.indexOf("a");          // O(n)
list.size(); list.isEmpty(); list.clear();

// ===== LinkedList 特有（头尾操作 O(1)）=====
ll.addFirst("head");  ll.addLast("tail");
ll.removeFirst();     ll.removeLast();
ll.getFirst();        ll.getLast();

// ===== 排序/遍历 =====
list.sort(Comparator.naturalOrder());   // 原地排序（Timsort）
for (String s : list) { }               // 迭代器（fail-fast，见 04 课）
```

**⚠️ 高频坑**：`Arrays.asList("a","b")` 返回**定长视图**，`add/remove` 抛 `UnsupportedOperationException`；要可变就 `new ArrayList<>(Arrays.asList(...))`。JDK 9+ 用 `List.of("a","b")`（不可变）。

## 🔍 关键源码（贴出逐段讲）

### ArrayList.add 与扩容（1.5 倍）

```java
// ArrayList 核心字段：
// transient Object[] elementData;   // 数据数组
// private int size;                 // 元素个数（≠数组长度）
// protected transient int modCount; // 结构修改计数（04 课讲过）

public boolean add(E e) {
    modCount++;                                  // ① 结构修改计数 +1（fail-fast 用）
    add(e, elementData, size);
    return true;
}
private void add(E e, Object[] elementData, int s) {
    if (s == elementData.length)
        elementData = grow();                    // ② 容量不够 → 扩容
    elementData[s] = e;                          // ③ 直接写数组 O(1)
    size = s + 1;
}
// grow() 的核心：
// int newCapacity = oldCapacity + (oldCapacity >> 1);   // 右移1位 = 除以2 → 1.5 倍
// elementData = Arrays.copyOf(elementData, newCapacity); // 整体搬到新数组
// get(int index) 就是 elementData[index] 一行（O(1) 的来源）
// remove(int index)：System.arraycopy(elementData, index+1, elementData, index, numMoved)
//                    把后面的元素整体前移一位（O(n) 的来源）
```

**逐点看懂**：
- **为什么尾部 add 是"摊还 O(1)"**：大多数 add 直接写数组 O(1)，只有容量满的那次 `copyOf` 搬整个数组 O(n)；但 1.5 倍增长让"搬移总成本"均摊到每次 add 仍是常数级（数学上：搬移次数 ≈ 1/(1.5-1) = 2 次/元素）。
- **为什么 1.5 倍不是 2 倍**：增长越快搬移越少，但浪费的空间越多。2 倍时闲置可达一半，1.5 倍闲置约 1/3——**搬移成本与空间浪费的折中**。
- **为什么预估容量有用**：`new ArrayList<>(1000)` 直接建 1000 容量，省掉若干次 `copyOf` 的整数组搬移。
- **为什么 remove(0) 比 remove(size-1) 慢**：前者要 `arraycopy` 前移 n-1 个元素，后者只把 size--（不搬移）。
- **为什么容量初始是 10 但不直接建**：JDK 8+ 懒创建，首次 add 才 `new Object[10]`，节省空列表内存。

### LinkedList.node：按下标访问为什么是 O(n)

```java
// LinkedList 核心字段：
// transient Node<E> first;  transient Node<E> last;  transient int size;
// private static class Node<E> { E item; Node<E> next; Node<E> prev; }

// 按下标取节点 —— 从离得近的一端开始遍历：
Node<E> node(int index) {
    if (index < (size >> 1)) {           // ① 下标在前半段
        Node<E> x = first;
        for (int i = 0; i < index; i++) x = x.next;   // 从头往后走
        return x;
    } else {                              // ② 下标在后半段
        Node<E> x = last;
        for (int i = size - 1; i > index; i--) x = x.prev;  // 从尾往前走
        return x;
    }
}
// linkBefore / unlink：改前后节点的 next/prev 指针即可（O(1)，但调用前先要 node() 定位）
```

**逐点看懂**：
- `get(500)` 要走 500 步（虽然做了"从近端走"的优化，仍是 O(n)）。
- 所以 `ll.add(500, x)` 是 **O(n) 定位 + O(1) 插链** = O(n)；所谓"LinkedList 插入快"只在**已持有节点引用**时成立（如迭代器内 `it.remove()`）。
- 节点是独立对象（Node），还有 prev/next 两个指针 → **内存开销约为 ArrayList 的 3 倍以上**（数据 + 2 指针 + 对象头），且节点散落堆中，**缓存局部性差**（遍历时 CPU 缓存命中率低）。

## 📊 ArrayList vs LinkedList（面试对比表）

| 维度 | ArrayList | LinkedList |
| ---- | ---- | ---- |
| 底层 | `Object[]` 连续数组 | 双向链表（节点散落） |
| 随机访问 get(i) | **O(1)** | O(n)（要遍历） |
| 尾部 add | **摊还 O(1)**（扩容搬移均摊） | O(1) |
| 头部 add/remove | O(n)（整体后移） | **O(1)** |
| 中间插删 | O(n)（arraycopy 搬移） | O(n)（定位 O(n) + 改指针 O(1)） |
| 内存 | 数组连续、有闲置容量 | 每元素 3 倍开销（数据+2指针） |
| 缓存 | 友好（连续内存） | 差（节点分散） |
| 适用 | 读多、尾增、按下标 | 头尾操作极多（但用 ArrayDeque 更好） |

**结论一句话**：**业务代码默认 ArrayList**。LinkedList 唯一的优势（头尾 O(1)）被 ArrayDeque 取代（见 09 课），"插入快"是经典误解——定位成本 O(n) 就抵消了。

## 🎯 面试问题

> [!question] 高频问题
> - Q: ArrayList 和 LinkedList 怎么选？
>   - A: ArrayList 连续数组：随机访问 O(1)、缓存友好、尾部追加摊还 O(1)；LinkedList 按下标定位 O(n)、节点开销大（约 3 倍内存）、缓存差，只有已持节点引用时的插删才 O(1)。业务默认 ArrayList。
> - Q: ArrayList 扩容机制？为什么 1.5 倍？
>   - A: 容量满时 grow：`oldCapacity + (oldCapacity >> 1)` 扩 1.5 倍，`Arrays.copyOf` 搬到新数组。1.5 倍是搬移次数与空间浪费的折中（2 倍闲置可达一半，1.5 倍约 1/3）；均摊后尾部 add 仍是 O(1)。
> - Q: ArrayList 和 Vector 的区别？
>   - A: Vector 是 JDK 1.0 遗留类，方法全 synchronized（线程安全但慢），扩容 2 倍；ArrayList 不同步、扩容 1.5 倍。单线程用 ArrayList，多线程用并发容器（CopyOnWriteArrayList，见并发章节）。
> - Q: ArrayList 的 remove(int) 为什么是 O(n)？
>   - A: 删除后要把后面的元素通过 System.arraycopy 整体前移一位；删尾部则无需搬移。
> - Q: 为什么说 LinkedList 插入不一定快？
>   - A: add(index, e) 要先 node(index) 从头部或尾部遍历定位（O(n)），定位成本 + 节点分配成本往往超过 ArrayList 的数组搬移；只有持有节点引用时（迭代器内）才 O(1)。

## 🛠️ 排查 / 实践要点

- 大批量数据：`new ArrayList<>(n)` 预容量，避免多次扩容搬移。
- 循环里 `list.contains(x)` → 换 `HashSet`（见 06 课）；循环里 `list.remove(0)` → 换 `ArrayDeque` 或倒序遍历。
- 遍历删除：`iterator.remove()` / `list.removeIf(...)`；直接 remove 抛 ConcurrentModificationException（见 04 课）。
- 看到 `LinkedList` 代码先问一句：真的需要头尾操作吗？多数场景 ArrayList + 倒序/双指针更优。

## ⚠️ 踩坑与注意事项

- 用 LinkedList 期待"插入快"，忽略定位成本和对象分配成本。
- `Arrays.asList` 的返回直接 add/remove → `UnsupportedOperationException`。
- `subList` 是视图：修改原 list 后遍历 subList 抛 ConcurrentModificationException。
- 频繁 `list.remove(0)`（O(n)）当队列用——用 ArrayDeque（09 课）。
- 并发场景共享 ArrayList（数据错乱）——用 CopyOnWriteArrayList（并发章节）。

## 🧪 最小验证

```java
// 1. 扩容观察：断点进 ArrayList.grow
List<Integer> list = new ArrayList<>(1);
list.add(1);
list.add(2);   // 断点：容量 1 → 2（1.5 倍取整）

// 2. 性能对比（可自行计时，10 万次）：
//    头插：ArrayList.add(0,x) 是 O(n²)；LinkedList.addFirst 是 O(n)
LinkedList<Integer> ll = new LinkedList<>();
for (int i = 0; i < 100_000; i++) ll.addFirst(i);   // 快
ArrayList<Integer> al = new ArrayList<>();
for (int i = 0; i < 100_000; i++) al.add(0, i);     // 明显慢（每次整体后移）

// 3. 视图陷阱
List<String> sub = al.subList(0, 2);
al.add("x");
sub.size();   // ConcurrentModificationException
```

生产排障：List 慢接口先看操作类型——get 多换 ArrayList、头尾操作多换 ArrayDeque、contains 多换 HashSet；内存上涨看 elementData 数组是否远超 size（容量闲置）。

## ✅ 精通检查清单

- 能不看表说出 ArrayList/LinkedList 各操作的复杂度及原因。
- 能默写 ArrayList.add/grow、LinkedList.node 的源码片段并解释。
- 能答出"为什么默认 ArrayList""为什么 1.5 倍""LinkedList 插入为什么不快"。
- 能在代码中识别并修复 List 的 O(n²) 隐患。

## 🔗 关联笔记

- 本课内：总纲 [[04-集合框架-核心机制与选型]]；后续 [[06-Set实现-HashSet与TreeSet]] · [[07-HashMap原理与扩容]] · [[08-Map实现-LinkedHashMap与TreeMap]] · [[09-Queue实现-ArrayDeque与PriorityQueue]]
- 延伸：[[20-Java后端/03-并发编程/08-并发容器-ConcurrentHashMap|并发容器]]（CopyOnWriteArrayList）

## 📚 参考资料

- [JDK 源码 ArrayList.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/ArrayList.java)
- [JDK 源码 LinkedList.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/LinkedList.java)
