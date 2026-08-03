---
title: Set实现-HashSet与TreeSet
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

# Set实现-HashSet与TreeSet

> [!abstract] 一句话总结
> Set 的三个实现全都不自己存数据：HashSet 委托 HashMap、LinkedHashSet 委托 LinkedHashMap、TreeSet 委托 TreeMap——**Set 就是"只用 key 的 Map"**；去重靠 equals/hashCode，排序靠 Comparable/Comparator；本课把委托源码贴出来讲清"为什么能去重/为什么有序"。

## 🎯 本节定位

- **本章第 6 课（集合域第 3 篇），集合实现篇（二）**。前置：[[01-核心机制与选型]]（equals/hashCode 契约、选型表）、[[04-HashMap原理与扩容]]（建议先看 04-HashMap 篇再回来，能彻底理解 HashSet；也可并行读）。
- 本课是"去重与排序"两个需求的完整答案；Map 变体见 [[05-Map实现-LinkedHashMap与TreeMap]]。
- **延伸阅读**：红黑树旋转/染色细节在 [[30-算法与数据结构/01-数据结构/07-平衡树-AVL-红黑树|算法章节]]；Comparable/Comparator 与排序算法在算法章节。

## 📌 这是什么

| 实现 | 底层 | 顺序 | 允许 null | 复杂度 | 一句话使用场景 |
| ---- | ---- | ---- | ---- | ---- | ---- |
| **HashSet** | HashMap | 无序 | 允许 1 个 | contains O(1) 平均 | 默认去重、快速判断存在 |
| LinkedHashSet | LinkedHashMap | **插入序** | 允许 1 个 | O(1) 平均 | 去重 + 要保插入顺序 |
| TreeSet | TreeMap（红黑树） | **按比较器排序** | 不允许 | O(log n) | 去重 + 要排序/范围查询 |

## 💻 怎么用（常用 API 速览）

```java
// ===== 创建 =====
Set<String> set = new HashSet<>();                  // 无序去重（默认）
Set<String> set = new LinkedHashSet<>();            // 保插入顺序
Set<String> set = new TreeSet<>();                  // 按自然序（元素需 Comparable）
Set<String> set = new TreeSet<>(Comparator.reverseOrder());   // 自定义排序

// ===== 增删查 =====
set.add("a");          // 已存在时返回 false（不报错、不覆盖）
set.contains("a");     // 平均 O(1)（TreeSet 是 O(log n)）
set.remove("a");
set.size(); set.isEmpty();

// ===== 去重一行流 =====
List<String> dup = List.of("b", "a", "b", "c");
List<String> unique = new ArrayList<>(new LinkedHashSet<>(dup));  // 去重且保原顺序

// ===== 集合运算 =====
Set<String> a = Set.of("1", "2"), b = Set.of("2", "3");
Set<String> u = new HashSet<>(a);  u.addAll(b);     // 并集
Set<String> i = new HashSet<>(a);  i.retainAll(b);  // 交集
Set<String> d = new HashSet<>(a);  d.removeAll(b);  // 差集

// ===== TreeSet 专有：范围查询 =====
TreeSet<Integer> ts = new TreeSet<>(List.of(1, 3, 5, 7, 9));
ts.first(); ts.last();              // 最小/最大
ts.lower(5); ts.higher(5);          // 严格小于/大于 5 的最大/最小
ts.floor(5); ts.ceiling(5);         // <=5 / >=5
ts.subSet(3, true, 7, true);        // [3,7] 闭区间视图
```

**⚠️ 高频坑**：TreeSet 的元素必须实现 `Comparable` 或构造时传 `Comparator`，否则 `add` 抛 `ClassCastException`；自定义对象放 HashSet 必须正确实现 equals/hashCode（契约见 04 课）。

## 🔍 关键源码（贴出逐段讲）

### HashSet：一行委托，没有任何自己的存储逻辑

```java
// HashSet 的全部"存储"就两个字段：
// private final HashMap<E,Object> map;              // 内部就是一个 HashMap
// private static final Object PRESENT = new Object();  // 所有 value 共用的占位对象

public HashSet() {
    map = new HashMap<>();                     // 构造 = 构造一个 HashMap
}
public HashSet(Collection<? extends E> c) {
    map = new HashMap<>(Math.max((int) (c.size()/.75f) + 1, 16));  // 按负载因子预估容量
    addAll(c);
}

public boolean add(E e) {
    return map.put(e, PRESENT) == null;        // 新 key → put 返回 null → add true
}                                              // 已存在 → put 返回旧值(PRESENT) → add false
public boolean contains(Object o) {
    return map.containsKey(o);
}
public boolean remove(Object o) {
    return map.remove(o) == PRESENT;
}
```

**逐点看懂**：
- **为什么 HashSet 能去重**：完全委托 HashMap——元素当 key，value 是固定占位对象。key 重复（equals 相等）时 `put` 只覆盖 value（还是同一个 PRESENT），Set 元素不变，`add` 返回 false。
- **为什么允许一个 null**：HashMap 允许 null key（放 table[0]），所以 HashSet 允许一个 null。
- **为什么无序**：顺序取决于哈希分布（桶的遍历顺序），不保证任何稳定顺序。
- 构造时按 `size/0.75 + 1` 预估容量——这就是 04 课"大批量插入前预估容量"的标准公式来源。

### LinkedHashSet：在 HashSet 之上加"保序"

```java
// LinkedHashSet 自己没有代码，只复写构造器：
public class LinkedHashSet<E> extends HashSet<E> {
    public LinkedHashSet() {
        super(16, .75f, true);     // 调用 HashSet 的"包私有"构造器：底层用 LinkedHashMap
    }
    // HashSet 里对应构造器：
    // HashSet(int initialCapacity, float loadFactor, boolean dummy) {
    //     map = new LinkedHashMap<>(initialCapacity, loadFactor);   // ← 就这一行不同
    // }
}
```

**逐点看懂**：LinkedHashSet = HashSet + LinkedHashMap 底层 → 多了"插入顺序链表"（原理见 [[05-Map实现-LinkedHashMap与TreeMap]]）。**去重逻辑不变，只是遍历顺序变成插入序**。

### TreeSet：委托 TreeMap，有序的来源

```java
// TreeSet 内部：
// private final NavigableMap<E,Object> m;   // 实际是 TreeMap

public TreeSet() {
    this(new TreeMap<>());              // 构造 = 构造一个 TreeMap
}
public TreeSet(Comparator<? super E> comparator) {
    this(new TreeMap<>(comparator));    // 自定义比较器
}

public boolean add(E e) {
    return m.put(e, PRESENT) == null;   // 同样委托，key 是元素
}
// 遍历用的是 TreeMap 的红黑树中序遍历 → 天然按 key 升序
// first()/last()/lower()/subSet() 全部委托 TreeMap 的对应方法
```

**逐点看懂**：
- TreeSet 的"有序"来自 TreeMap 的红黑树中序遍历（见 08 课）。
- **为什么 O(log n)**：红黑树插入/查找都要从根走 log₂n 层。
- **为什么不允许 null**：红黑树比较时 `null` 无法参与比较（`compareTo(null)` 抛 NPE），所以 TreeSet/TreeMap 都不允许 null key。

### Comparable vs Comparator（面试必考，放这里）

```java
// 方式一：元素自己实现 Comparable（"自然序"）
class User implements Comparable<User> {
    int id; String name;
    @Override public int compareTo(User o) {   // 返回 负数/0/正数
        return Integer.compare(this.id, o.id); // 按 id 升序
    }
}
Set<User> s1 = new TreeSet<>();               // 直接用自然序

// 方式二：外部传 Comparator（不改元素类，灵活）
Set<User> byName = new TreeSet<>(Comparator.comparing(u -> u.name));
Set<User> byNameDesc = new TreeSet<>(Comparator.comparing((User u) -> u.name).reversed());
// Comparator 链式组合：
Comparator<User> cmp = Comparator.comparing(User::name).thenComparing(User::id);
```

**对比结论（面试答法）**：
- `Comparable`：**"我是可比较的"**——类内部实现，只有一个 compareTo，定义自然序；改排序规则要改类。
- `Comparator`：**"我是比较器"**——独立的外部策略，可以多个、可以 Lambda/方法引用，不改元素类；用于 TreeSet/TreeMap 构造、`List.sort`、`Stream.sorted`。
- 与 equals 的一致性约定：`compareTo == 0` 应等价于 `equals`，否则 TreeSet 的"去重"（比较器认为相同就覆盖）和 HashSet 的"去重"（equals 判断）结果会不一致——经典坑。

## 🎯 面试问题

> [!question] 高频问题
> - Q: HashSet 为什么能去重？底层是什么？
>   - A: 底层是 HashMap，元素作为 key、value 是固定占位对象 PRESENT。add 时 map.put(e, PRESENT)，重复（equals 相等）时 put 只覆盖占位对象并返回旧值 → add 返回 false。去重完全依赖 key 的 equals/hashCode。
> - Q: HashSet、LinkedHashSet、TreeSet 的区别？
>   - A: 三者都委托 Map：HashSet 用 HashMap（无序、O(1)）；LinkedHashSet 用 LinkedHashMap（保插入序、O(1)）；TreeSet 用 TreeMap（按比较器排序、O(log n)）。要保序/排序才用后两者，默认 HashSet。
> - Q: hashCode 相同但 equals 不同会怎样？
>   - A: 两个对象散到同一桶（哈希冲突），contains 时 equals 逐个比较后返回 false——两个都保留在 Set 里，只是桶变长、性能下降（HashMap 会树化，见 07 课）。
> - Q: Comparable 和 Comparator 的区别？
>   - A: Comparable 是元素自身实现（自然序，一个 compareTo）；Comparator 是外部策略（可多个、可 Lambda、不改类）。TreeSet/TreeMap 两者都支持，List.sort/Stream.sorted 用 Comparator。
> - Q: TreeSet 为什么不允许 null？
>   - A: 底层红黑树插入时要调用 compareTo/compare 比较，null 无法参与比较会抛 NPE，所以 TreeSet/TreeMap 拒绝 null key。
> - Q: 去重保序怎么做？
>   - A: `new LinkedHashSet<>(list)` 或 `new ArrayList<>(new LinkedHashSet<>(list))`——去重且保持原顺序；TreeSet 是排序序不是插入序，别混。

## 🛠️ 排查 / 实践要点

- "contains 慢"：`List.contains` O(n) → `HashSet` O(1)（本课的核心价值场景）。
- 去重失败：查 equals/hashCode 是否同时重写、字段是否可变（04 课契约）。
- TreeSet 抛 ClassCastException：元素没实现 Comparable 且没传 Comparator。
- 去重结果和预期不符：compareTo 与 equals 不一致时，TreeSet 和 HashSet 的去重判定不同。

## ⚠️ 踩坑与注意事项

- 自定义对象放 HashSet 只重写 equals 不重写 hashCode（或反之）。
- 放进 Set 后修改对象的 hashCode 字段 → 对象"丢失"。
- TreeSet 的元素不可比 → ClassCastException。
- 混淆 LinkedHashSet（插入序）和 TreeSet（排序序）。
- 单线程用并发 Set（CopyOnWriteArraySet）——性能浪费；并发场景该用并发容器（并发章节）。

## 🧪 最小验证

```java
// 1. 委托验证：HashSet 的 add 返回值
Set<String> s = new HashSet<>();
System.out.println(s.add("a"));   // true（新元素）
System.out.println(s.add("a"));   // false（重复）

// 2. hashCode 相等但 equals 不同 → 两个都保留
class K { int id; K(int id){this.id=id;} 
  public int hashCode(){return 1;}             // 全部同桶
  public boolean equals(Object o){return o instanceof K k && k.id==id;} }
Set<K> ks = new HashSet<>();
ks.add(new K(1)); ks.add(new K(2));
System.out.println(ks.size());    // 2（冲突但都保留）

// 3. 去重保序
List<String> dup = List.of("b", "a", "b", "c");
System.out.println(new ArrayList<>(new LinkedHashSet<>(dup)));  // [b, a, c]

// 4. compareTo 与 equals 不一致的坑（面试演示）：
//    TreeSet 用 compareTo 判重、HashSet 用 equals 判重 → 同一批数据结果不同
```

生产排障：去重与存在性判断用 HashSet；要保序去重用 LinkedHashSet；要范围查询用 TreeSet（first/floor/subSet）。自定义对象一律先确认 equals/hashCode（或 compareTo）正确。

## ✅ 精通检查清单

- 能说出 Set 三实现的底层委托关系与复杂度。
- 能默写 HashSet.add 的一行委托源码并解释返回语义。
- 能答出 Comparable vs Comparator、TreeSet 为什么不能放 null、hashCode 相同 equals 不同会怎样。
- 遇到去重/排序需求能直接写出对应代码。

## 🔗 关联笔记

- 本课内：总纲 [[01-核心机制与选型]]；前置 [[04-HashMap原理与扩容]]（HashSet 的底层）；Map 变体 [[05-Map实现-LinkedHashMap与TreeMap]]
- 延伸：[[30-算法与数据结构/01-数据结构/07-平衡树-AVL-红黑树|红黑树原理]]

## 📚 参考资料

- [JDK 源码 HashSet.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/HashSet.java)
- [JDK 源码 TreeSet.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/util/TreeSet.java)
