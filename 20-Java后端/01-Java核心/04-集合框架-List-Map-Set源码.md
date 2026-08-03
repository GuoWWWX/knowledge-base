---
title: 集合框架-List-Map-Set源码
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Java核心
  - 阶段/01
created: 2026-07-04
updated: 2026-08-03
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# 集合框架-List-Map-Set源码

> [!abstract] 一句话总结
> 集合框架 = 两棵家族树（Collection 与 Map）+ 一张选型表。学完本课你应该能：**看到需求直接说出用哪个类、怎么写代码**（List/Set/Map/Queue 四族各 3 个常用实现），并且能讲出 ArrayList 扩容、HashSet 去重、fail-fast 这几个关键机制**到底长什么样**（源码已贴在本课，不用自己去翻）。

## 🎯 本节定位

- **本章第 4 课，数据组织**。前置：[[01-泛型与类型擦除]]（集合 API 的类型安全来自泛型）、[[02-异常处理最佳实践]]（`ConcurrentModificationException` 属于运行期异常）、[[03-String与字符串常量池]]（String 是最常见的元素/key）。
- 下一课 [[05-HashMap原理与扩容]] 深挖 HashMap；本课负责"全景 + 使用"，把 `HashSet 基于 HashMap`、`TreeMap 是红黑树` 这些关系讲清，下一课直接接续。
- **延伸阅读**：红黑树旋转/染色原理在 [[30-算法与数据结构/01-数据结构/07-平衡树-AVL-红黑树|算法章节]]；并发容器在 [[20-Java后端/03-并发编程/08-并发容器-ConcurrentHashMap|03-并发编程章节]]，本课只讲单线程语义。

## 📌 这是什么：集合家族全景

两棵家族树，记住一句话：**Set 就是"只用 key 的 Map"，Queue/Deque 是"有队首队尾语义的 Collection"**。

```
Collection（单元素容器）
├── List（有序、可重复、按下标访问）
│   ├── ArrayList      —— 数组实现（默认首选）
│   └── LinkedList     —— 双向链表
├── Set（不重复）
│   ├── HashSet        —— 底层 HashMap（默认首选）
│   ├── LinkedHashSet  —— 底层 LinkedHashMap（保插入序）
│   └── TreeSet        —— 底层 TreeMap（按排序）
└── Queue/Deque（队列/双端队列）
    ├── ArrayDeque     —— 循环数组（默认首选）
    └── PriorityQueue  —— 二叉堆（按优先级出队）

Map（键值映射，不继承 Collection）
├── HashMap           —— 哈希表（默认首选）
├── LinkedHashMap     —— HashMap + 双向链表（保序/LRU）
└── TreeMap           —— 红黑树（按 key 排序）

遗留类（面试认识即可，生产别用）：Vector、Stack、Hashtable —— 方法全 synchronized，性能差
```

## 🤔 解决什么问题 / 为什么需要

- **选型错误 = 线上慢接口**：`List.contains` 是 O(n)，`HashSet.contains` 平均 O(1)——循环里调用 10 万次就是 10 万倍差距。**本课第一目标是让你会选**。
- **正确性依赖契约**：去重失败、找不到 key、迭代报错，根源大多是 equals/hashCode 契约被破坏（见下文"契约"节）。
- **读懂关键源码**：ArrayList 为什么默认首选、HashSet 为什么能去重、迭代时为什么报 ConcurrentModificationException——**源码关键段已贴在本课**，不用自己翻。

## 🧩 一图选型：先看语义，再看复杂度

| 接口 | 实现 | 底层结构 | 顺序 | 允许 null | 查/增 复杂度 | 什么时候用 |
| ---- | ---- | ---- | ---- | ---- | ---- | ---- |
| List | **ArrayList** | `Object[]` | 保序(按下标) | 允许 | 按下标查 O(1)；尾增摊还 O(1)；中间插删 O(n) | **默认列表**：读多、尾增、要下标 |
| List | LinkedList | 双向链表 | 保序 | 允许 | 按下标查 O(n)；头尾增删 O(1) | 频繁头尾操作（但一般用 ArrayDeque 更好） |
| Set | **HashSet** | HashMap | 无序 | 允许 1 个 | contains O(1) 平均 | **默认去重**、快速 contains |
| Set | LinkedHashSet | LinkedHashMap | 插入序 | 允许 1 个 | O(1) 平均 | 去重 + 要保插入顺序 |
| Set | TreeSet | TreeMap(红黑树) | 按比较器排序 | 不允许 | O(log n) | 去重 + 要排序/范围查询 |
| Map | **HashMap** | 数组+链表/红黑树 | 无序 | key/value 均允许 | get/put O(1) 平均 | **默认键值**、缓存、计数 |
| Map | LinkedHashMap | HashMap+双向链表 | 插入序/访问序 | 允许 | O(1) 平均 | 保序 Map；accessOrder=true 做 LRU |
| Map | TreeMap | 红黑树 | 按 key 排序 | key 不允许 | O(log n) | 排序、范围查询(floorEntry 等) |
| Queue | **ArrayDeque** | 循环数组 | FIFO | 不允许 | 头尾操作 O(1) | **默认队列/栈** |
| Queue | PriorityQueue | 二叉堆 | 按优先级 | 不允许 | 入/出 O(log n) | 任务按优先级处理、TopK |

**选型三步**（背下来）：
1. 是键值对 → Map；单元素 → Collection。
2. 要不要去重 → Set；要不要保序/下标 → List；FIFO/栈 → Queue。
3. 具体实现看"顺序"和"复杂度"两列：默认就选加粗的四个（ArrayList/HashSet/HashMap/ArrayDeque），有特殊顺序需求才换 Linked/Tree 变体。

## 💻 每种集合怎么用（本课核心）

### 1. List：ArrayList（默认）与 LinkedList

```java
// ===== 创建 =====
List<String> list = new ArrayList<>();              // 默认容量 10（懒创建，首次 add 才建数组）
List<String> list = new ArrayList<>(1000);          // 预估容量：大批量 add 前必做，减少扩容
List<String> list = new ArrayList<>(otherSet);      // 从任意 Collection 复制

// ===== 增删改查 =====
list.add("a");              // 尾部追加，摊还 O(1)
list.add(0, "x");           // 指定位置插入，O(n)（后面的元素整体后移）
list.get(0);                // 按下标查，O(1) —— ArrayList 的招牌
list.set(0, "b");           // 修改
list.remove(0);             // 按下标删，O(n)
list.remove("a");           // 按值删（equals 匹配第一个），O(n)
list.contains("a");         // O(n)！循环里调用要警惕（→ 换 HashSet）
list.indexOf("a");          // O(n)
list.size(); list.isEmpty(); list.clear();

// ===== 遍历 =====
for (String s : list) { }                       // 增强 for（底层迭代器）
list.forEach(System.out::println);              // Lambda 风格（06 课）
list.stream().filter(...).toList();             // Stream（06 课）

// ===== 排序 =====
list.sort(Comparator.naturalOrder());           // JDK 8+，原地排序（Timsort）
Collections.sort(list);                         // 老写法

// ===== 什么时候用 LinkedList？ =====
LinkedList<String> ll = new LinkedList<>();
ll.addFirst("head");   ll.addLast("tail");      // 头尾操作 O(1)
ll.removeFirst();      ll.removeLast();
// 注意：ll.get(500) 是 O(n)（要遍历）；"插入快"是误解——定位就要 O(n)
// 结论：需要"双端操作"优先 ArrayDeque；LinkedList 实际场景很少
```

**⚠️ 高频坑**：`Arrays.asList("a","b")` 返回的是**定长视图**，`add/remove` 会抛 `UnsupportedOperationException`；要可变列表就 `new ArrayList<>(Arrays.asList(...))`。JDK 9+ 用 `List.of("a","b")`（不可变）。

### 2. Set：HashSet（默认）/ LinkedHashSet / TreeSet

```java
// ===== 创建 =====
Set<String> set = new HashSet<>();          // 无序去重（默认）
Set<String> set = new LinkedHashSet<>();    // 保插入顺序去重
Set<String> set = new TreeSet<>();          // 按自然序/比较器排序
Set<String> set = new TreeSet<>(Comparator.reverseOrder());  // 自定义排序

// ===== 增删查 =====
set.add("a");          // 已存在时返回 false，不报错也不覆盖
set.contains("a");     // 平均 O(1) —— 去重和快速判断的核心价值
set.remove("a");       // 按值删
set.size(); set.isEmpty();

// ===== 遍历（顺序取决于实现）=====
for (String s : set) { }
// HashSet 遍历顺序无保证；LinkedHashSet 是插入序；TreeSet 是排序序

// ===== 去重一行流 =====
List<String> dup = List.of("a", "b", "a");
List<String> unique = new ArrayList<>(new LinkedHashSet<>(dup));  // 去重且保原顺序

// ===== 集合运算（Set 接口默认方法）=====
Set<String> a = Set.of("1", "2"), b = Set.of("2", "3");
Set<String> u = new HashSet<>(a);  u.addAll(b);                 // 并集
Set<String> i = new HashSet<>(a);  i.retainAll(b);              // 交集
Set<String> d = new HashSet<>(a);  d.removeAll(b);              // 差集

// ===== TreeSet 专有：范围查询 =====
TreeSet<Integer> ts = new TreeSet<>(List.of(1, 3, 5, 7, 9));
ts.first(); ts.last();                    // 最小/最大
ts.lower(5); ts.higher(5);                // 严格小于/大于 5 的最大/最小
ts.floor(5); ts.ceiling(5);               // <=5 / >=5
ts.subSet(3, true, 7, true);              // [3,7] 范围
```

**注意**：Set 存的是**自定义对象**时，去重/查找依赖 equals/hashCode（见下文"契约"节）；TreeSet 要求元素实现 `Comparable` 或传入 `Comparator`，否则运行时抛 `ClassCastException`。

### 3. Map：HashMap（默认）/ LinkedHashMap / TreeMap

```java
// ===== 创建 =====
Map<String, Integer> map = new HashMap<>();                 // 无序（默认）
Map<String, Integer> map = new HashMap<>(capacity);         // 预估容量 = expected/0.75 + 1
Map<String, Integer> map = new LinkedHashMap<>();           // 保插入序
Map<String, Integer> map = new TreeMap<>();                 // 按 key 排序

// ===== 增删改查 =====
map.put("a", 1);              // 插入/更新，返回旧值（没有则 null）
map.get("a");                 // 查，没有返回 null
map.getOrDefault("x", 0);     // 查，没有返回默认值（比"判 null"优雅）
map.containsKey("x");         // 判断 key 是否存在（比 get==null 更语义化）
map.remove("a");              // 删
map.size(); map.isEmpty(); map.clear();

// ===== 遍历 3 种方式 =====
for (Map.Entry<String, Integer> e : map.entrySet()) {
    e.getKey(); e.getValue(); e.setValue(2);        // 推荐：能改值
}
for (String k : map.keySet()) { }
for (Integer v : map.values()) { }

// ===== 计数/分组神器（JDK 8+，强烈推荐）=====
map.merge("a", 1, Integer::sum);        // "a" 存在则 +1，不存在则放 1（计数）
map.computeIfAbsent("a", k -> new ArrayList<>()).add("x");  // 不存在才建（分组）
map.computeIfPresent("a", (k, v) -> v + 1);   // 存在才更新

// ===== TreeMap 专有：范围查询 =====
TreeMap<String, Integer> tm = new TreeMap<>(map);
tm.firstKey(); tm.lastKey();                    // 最小/最大 key
tm.floorEntry("m"); tm.ceilingEntry("m");       // <=m / >=m
tm.subMap("a", true, "m", false);               // 范围子视图
tm.tailMap("n");                                // key >= n 的部分

// ===== LinkedHashMap 做 LRU 缓存（了解，实战常用）=====
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    LRUCache(int capacity) { super(capacity, 0.75f, true); this.capacity = capacity; }  // accessOrder=true
    @Override protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;   // 超过容量淘汰最久未访问的
    }
}
```

**⚠️ 高频坑**：`HashMap` 的 key 用**可变对象**且 put 后修改了参与 hashCode 的字段 → get/remove 找不到（详情见 [[05-HashMap原理与扩容]]）；key 为 null 时 `get` 返回 null 无法区分"没有"和"值为 null"，用 `containsKey` 判断。

### 4. Queue/Deque：ArrayDeque（默认）与 PriorityQueue

```java
// ===== 当队列用（FIFO）=====
Deque<String> queue = new ArrayDeque<>();
queue.offer("a");               // 入队（尾）
queue.offer("b");
String head = queue.peek();     // 看队首，不弹出
String x = queue.poll();        // 出队（头），空返回 null

// ===== 当栈用（LIFO）=====
Deque<String> stack = new ArrayDeque<>();
stack.push("a");                // 入栈
stack.push("b");
stack.peek();                   // 看栈顶
stack.pop();                    // 出栈

// ===== 优先队列（按优先级出队）=====
Queue<Integer> pq = new PriorityQueue<>();                        // 小顶堆：最小的先出
Queue<Integer> pq2 = new PriorityQueue<>(Comparator.reverseOrder());  // 大顶堆：最大的先出
pq.offer(5); pq.offer(1); pq.offer(3);
pq.poll();                      // 1（小顶堆先出最小）

// 自定义优先级：传 Comparator（如任务按 deadline 排序）
Queue<Task> tasks = new PriorityQueue<>(Comparator.comparing(Task::deadline));
```

**⚠️ 高频坑**：`ArrayDeque`/`PriorityQueue` **不允许 null**（`offer(null)` 抛 NPE）；`PriorityQueue` 的元素需可比（Comparable/Comparator）；`LinkedList` 也能当队列，但节点开销大，**双端操作用 ArrayDeque 更快**；不要用遗留的 `Stack`/`Vector`/`Hashtable`。

### 5. 遗留类（面试认识，生产别用）

- `Vector`/`Hashtable`：方法全 `synchronized`，JDK 1.0 遗留，性能差；单线程用 ArrayList/HashMap，多线程用并发容器（03-并发编程章节）。
- `Stack`：继承 Vector，同样全同步；用 `ArrayDeque` 代替。
- 面试可能问"Vector 和 ArrayList 区别"——答：Vector 方法同步 + 扩容翻倍（ArrayList 1.5 倍），其余没优势。

## 🔍 关键源码（已贴出，逐段讲）

> 下面的代码是 JDK 源码的**关键片段**（JDK 17 略作精简），完整上下文想看时按文末"源码去哪看"操作。**本课不需要你自己翻源码**，看懂这些片段就够面试和排障。

### 1. ArrayList.add 与扩容（为什么默认首选它）

```java
// ArrayList#add —— 两步：可能扩容 + 数组写入
public boolean add(E e) {
    modCount++;                                  // ① 结构修改计数 +1（fail-fast 用）
    add(e, elementData, size);
    return true;
}
private void add(E e, Object[] elementData, int s) {
    if (s == elementData.length)
        elementData = grow();                    // ② 容量不够 → 扩容
    elementData[s] = e;                          // ③ 直接写数组，O(1)
    size = s + 1;
}
// grow() 的核心一行（扩容 1.5 倍）：
// int newCapacity = oldCapacity + (oldCapacity >> 1);   // 右移1位 = /2
// elementData = Arrays.copyOf(elementData, newCapacity); // 整体搬到新数组
```

**逐点看懂**：
- **为什么尾部 add 是"摊还 O(1)"**：大多数 add 直接写数组（O(1)），只有容量满的那一次要 `copyOf` 搬整个数组（O(n)），但 1.5 倍增长让"搬移总成本"均摊到每次 add 上仍是常数级。
- **为什么 1.5 倍不是 2 倍**：增长越快搬移越少但浪费的空间越多；1.5 倍是"搬移次数与空间浪费"的折中（扩容后一半空间可能闲置 → 1.5 倍只闲置 1/3）。
- **为什么 modCount++**：迭代器靠它检测"迭代过程中被修改"（见下方 fail-fast）。
- **为什么预估容量有用**：`new ArrayList<>(1000)` 直接建 1000 容量数组，省掉扩容搬移；大批量数据是明显性能提升。

### 2. HashSet.add（去重为什么可靠）

```java
// HashSet 内部就一个 HashMap：
// private final HashMap<E,Object> map;
// private static final Object PRESENT = new Object();   // 所有 value 共用的占位对象

public boolean add(E e) {
    return map.put(e, PRESENT) == null;   // 新 key → put 返回 null → add 返回 true
}                                         // 已存在 → put 返回旧值(PRESENT) → add 返回 false
```

**逐点看懂**：HashSet 没有任何自己的存储逻辑，**完全委托 HashMap**：元素当 key，value 是固定占位对象。所以"能不能去重"取决于 key 的 `equals/hashCode`（见"契约"节），这也解释了为什么 HashSet 允许一个 null（HashMap 允许 null key）。

### 3. fail-fast 迭代器（为什么增强 for 里 remove 会报错）

```java
// ArrayList.Itr（迭代器内部类）的核心检查：
private class Itr implements Iterator<E> {
    int expectedModCount = modCount;             // 创建迭代器时"快照"修改计数
    public E next() {
        checkForComodification();                // 每次 next 都检查
        ...
    }
    final void checkForComodification() {
        if (modCount != expectedModCount)
            throw new ConcurrentModificationException();   // 发现被改过就抛异常
    }
}
```

**逐点看懂**：迭代器创建时记下 `expectedModCount`；任何结构修改（add/remove/clear）都会 `modCount++`。下次 `next()` 发现两边不一致 → 抛异常。**增强 for 本质就是迭代器**，所以循环内 `list.remove(x)` 必然触发；正确删除用 `iterator.remove()`（它同步维护 expectedModCount）或收集后批量删。fail-fast 是"尽早暴露错误"，**不是并发安全机制**。

### 4. TreeMap 与 LinkedHashMap（一句话源码级理解）

- **TreeMap.put** 走红黑树插入：先按比较器找插入位置（O(log n)），插入后 `fixAfterInsertion` 做旋转+染色保持平衡。红黑树的旋转细节在算法章节，这里只需知道：**它保证"有序 + O(log n)"，代价是常数比 HashMap 大**。
- **LinkedHashMap** 是 HashMap + 双向链表：每次 put 把节点挂到链表尾（保插入序）；`accessOrder=true` 时每次 get 也把节点移到尾 → **链表头永远是最久未访问的**，配合 `removeEldestEntry` 就是 LRU 淘汰（见上方代码）。

## ⚖️ equals/hashCode 契约（集合正确性的地基）

所有基于哈希的容器（HashSet/HashMap/HashTable 系列）都依赖两条规则：

1. **相等对象 hashCode 必须相等**：`a.equals(b)` 为 true ⇒ `a.hashCode() == b.hashCode()`。违反它 → 相等的对象散到不同桶 → contains/get 找不到。
2. **hashCode 相等不代表相等**：不同对象可能同桶（哈希冲突），由 equals 最终判断。

**正确写法**（手写规则）：
```java
@Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User u)) return false;
    return id == u.id && Objects.equals(name, u.name);
}
@Override public int hashCode() { return Objects.hash(id, name); }   // 必须与 equals 用同一组字段
```
- 参与计算的字段**必须不可变**：放进 Set/Map 后若修改了这些字段 → 桶位错乱，对象"丢失"。
- 现代写法：直接 `record User(long id, String name) {}`，编译器生成正确配套的 equals/hashCode（见 [[10-Java新特性-Record与密封类]]）。

## 🛠️ 排查 / 实践要点

- 慢接口先看**容器 + 复杂度**：`List.contains` O(n) vs `HashSet.contains` O(1)；循环内调用要乘外层次数（`for users -> roles.contains(x)` 是 O(n×m)）。
- `ConcurrentModificationException`：查增强 for 中是否直接 remove → 改 `iterator.remove()` 或收集后批量删除。
- 去重/查找失效：查 equals/hashCode 是否同时重写、字段是否可变。
- 大批量插入前预估容量：`new ArrayList<>(n)`、`new HashMap<>(n/0.75f + 1)`。

## ⚠️ 踩坑与注意事项

- 用 LinkedList 期待"插入快"：定位 O(n) + 节点对象开销，业务默认 ArrayList。
- 把 HashMap 迭代顺序当稳定行为（它不保证顺序，要保序用 LinkedHashMap）。
- 修改放进集合后的对象字段（破坏 hashCode）。
- `Arrays.asList` 结果直接 add/remove → `UnsupportedOperationException`。
- 并发场景共享 HashMap/ArrayList（应使用并发容器，见并发章节）。
- 循环内 `list.contains` / `map.get` 不预判——复杂度失控。

## 🎯 面试问题

> [!question] 高频问题
> - Q: ArrayList 和 LinkedList 怎么选？
>   - A: ArrayList 连续数组：随机访问 O(1)、缓存友好、尾部追加摊还 O(1)；LinkedList 按下标定位 O(n)、节点对象开销大，只有已持节点引用时的插删才 O(1)。业务默认 ArrayList。
> - Q: HashSet 为什么能去重？和 TreeSet 区别？
>   - A: HashSet 底层 HashMap（元素作 key），去重靠 equals/hashCode，无序、O(1)；TreeSet 底层 TreeMap（红黑树），按比较器排序、O(log n)，元素必须可比较。
> - Q: HashMap、LinkedHashMap、TreeMap 区别？
>   - A: HashMap 无序 O(1)；LinkedHashMap 保插入序（accessOrder=true 可做 LRU）；TreeMap 按 key 排序 O(log n)，支持范围查询。
> - Q: fail-fast 是什么？是并发安全机制吗？
>   - A: 迭代器通过 modCount/expectedModCount 检测迭代期间的结构修改，不一致就抛 ConcurrentModificationException。它只是尽早暴露错误，不是并发保护。
> - Q: 为什么 equals 和 hashCode 必须一起重写？
>   - A: 哈希容器先 hashCode 定位桶、再 equals 判相等；只重写一个会导致相等对象找不到或去重失效。

## 🧭 想看完整源码，去哪看？（3 种方式）

本课已把关键片段贴出来，**理解机制够用了**。想追完整上下文时：

1. **本地（最快）**：JDK 安装目录自带源码包——`C:\Program Files\Java\jdk-21\lib\src.zip`，解压即全部 JDK 源码；IDEA 中把光标放类名上按 `Ctrl+B`（Mac: `Cmd+B`）跳转定义，首次会提示下载 Sources，下载完就能看。
2. **在线（不用装东西）**：
   - grep.app：`https://grep.app/search?q=ArrayList` 全仓库搜任意类；
   - GitHub openjdk/jdk：`https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/ArrayList.java`（把文件名换成 HashMap/LinkedList 等即可）。
3. **推荐路径**：先看本课片段 → 再在 IDEA 里对同一个方法 `Ctrl+B` 看上下文 → 看不懂的局部（如红黑树）跳到对应章节补。

**本课源码入口清单**（想看时直奔）：
- `ArrayList#add` → `grow()`：扩容 1.5 倍 + `Arrays.copyOf`。
- `ArrayList.Itr#checkForComodification`：fail-fast 检查点。
- `HashSet#add`：一行委托 `HashMap#put(e, PRESENT)`。
- `TreeMap#put` → `fixAfterInsertion`：红黑树旋转染色。
- `LinkedHashMap#afterNodeAccess`、`removeEldestEntry`：顺序/LRU 维护。

## 🧪 最小验证

```java
// 1. 扩容观察：断点进 ArrayList.grow
List<Integer> list = new ArrayList<>(1);
list.add(1);
list.add(2);   // 断点：容量 1 → 2

// 2. fail-fast
Iterator<Integer> it = list.iterator();
list.add(3);
it.next();     // ConcurrentModificationException

// 3. 去重保序
List<String> dup = List.of("b", "a", "b", "c");
System.out.println(new ArrayList<>(new LinkedHashSet<>(dup)));  // [b, a, c]

// 4. TreeMap 范围查询
TreeMap<Integer, String> tm = new TreeMap<>();
tm.put(1, "a"); tm.put(5, "e"); tm.put(9, "i");
System.out.println(tm.subMap(2, true, 8, true));   // {5=e}

// 5. PriorityQueue 按优先级出队
Queue<Integer> pq = new PriorityQueue<>(List.of(5, 1, 3));
System.out.println(pq.poll());   // 1
```

生产排障：集合慢接口先列"容器/数据量/操作/复杂度"四列；去重与查找问题先查 equals/hashCode；迭代异常先查循环内 remove。

## ✅ 精通检查清单

- 能不看表格说出 List/Set/Map/Queue 四族各 2-3 个实现及其复杂度。
- 能直接写出每种集合的创建 + 增删改查 + 遍历代码。
- 能讲清 ArrayList 扩容、HashSet 去重、fail-fast 三个机制的源码片段。
- 能回答本页高频面试题，并补充项目中的真实取舍。

## 🔗 关联笔记

- 本课内：上一课 [[03-String与字符串常量池]]；深挖见 [[05-HashMap原理与扩容]]；处理见 [[06-Lambda与Stream]]；record 做 key 见 [[10-Java新特性-Record与密封类]]
- 延伸：[[30-算法与数据结构/01-数据结构/07-平衡树-AVL-红黑树|红黑树原理]]；[[20-Java后端/03-并发编程/08-并发容器-ConcurrentHashMap|并发容器]]

## 📚 参考资料

- [Dev.java Collections Tutorials](https://dev.java/learn/api/collections-framework/)
- [Oracle Java Collections Overview](https://docs.oracle.com/javase/8/docs/technotes/guides/collections/overview.html)
- [JDK 源码（GitHub）](https://github.com/openjdk/jdk/tree/master/src/java.base/share/classes/java/util)
