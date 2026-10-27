---
title: 10 · 技巧-Java刷题必备API与集合排序速查
tags:
  - 领域/算法
  - 类型/技巧
  - 语言/Java
created: 2026-10-26
updated: 2026-10-26
status: 已完成
mastery: ⭐⭐⭐⭐⭐
---

# 10 · 技巧：Java 刷题必备 API 与集合排序速查宝典

> [!tip] 核心诉求
> 刷算法题时，很多同学不是不会算法思路，而是**卡在 Java 语法和集合 API 上**：
> - 二维数组排序老是记不住语法？
> - 写 `(a, b) -> a - b` 结果遇到整型溢出报 Bug 找半天？
> - 小顶堆、大顶堆怎么一句话建好？
> - `Stack` 为什么被官方弃用？现在用什么替代？
> - `Map` 统计频次还在写四五行 `if-else`？
> 
> 本文收录刷题中**出现频次最高、最能提升编码速度、防止翻车的实用语法糖与标准模板**。

---

## 一、 排序与 Comparator（必背与防溢出红线）

### 1. 一维数组与 List 排序（升序 / 降序全对比）

#### ① `Arrays.sort`：针对一维原生数组
```java
// 1. 基本数据类型数组 int[]
int[] nums = {3, 1, 4, 1, 5};
Arrays.sort(nums); // 升序：[1, 1, 3, 4, 5]

// ⚠️ 避坑：int[] 不支持直接传 Collections.reverseOrder()（Java 泛型不支持基本数据类型）
// 若必须对 int[] 进行降序：
// 最优解：先升序排序，再用双指针首尾翻转（耗时 O(N)，零内存开销，大厂笔试首选）
for (int l = 0, r = nums.length - 1; l < r; l++, r--) {
    int t = nums[l]; nums[l] = nums[r]; nums[r] = t;
}

// 2. 包装类 / 对象数组 Integer[]
Integer[] arr = {3, 1, 4, 1, 5};
Arrays.sort(arr);                                      // 升序：[1, 1, 3, 4, 5]
Arrays.sort(arr, Collections.reverseOrder());          // 降序：[5, 4, 3, 1, 1]
Arrays.sort(arr, (a, b) -> Integer.compare(b, a));     // 降序（Lambda 标准写法）
```

#### ② `Collections.sort`：针对集合 List（经典工具类）
```java
List<Integer> list = new ArrayList<>(List.of(3, 1, 4, 1, 5));

// 升序（两种常用写法）：
Collections.sort(list);                                // 默认自然升序
Collections.sort(list, (a, b) -> Integer.compare(a, b));// Lambda 升序

// 降序（两种常用写法）：
Collections.sort(list, Collections.reverseOrder());    // 经典逆序工具
Collections.sort(list, (a, b) -> Integer.compare(b, a));// Lambda 降序
```

#### ③ `list.sort`：List 自身实例方法（Java 8+ 官方首推）
```java
List<Integer> list = new ArrayList<>(List.of(3, 1, 4, 1, 5));

// 升序（三种常用写法）：
list.sort(null);                                       // 传 null 即默认自然升序
list.sort(Comparator.naturalOrder());                  // 显式自然升序
list.sort((a, b) -> Integer.compare(a, b));            // Lambda 升序

// 降序（三种常用写法）：
list.sort(Collections.reverseOrder());                 // 常用降序
list.sort(Comparator.reverseOrder());                  // 现代逆序接口
list.sort((a, b) -> Integer.compare(b, a));            // Lambda 降序
```

> [!tip] 极简速查对比表格
> | 排序目标 | 推荐调用方式 | 升序写法 | 降序写法 |
> | :--- | :--- | :--- | :--- |
> | **`int[]` 基础数组** | `Arrays.sort` | `Arrays.sort(nums);` | 先升序后双指针原地翻转 |
> | **`Integer[]` 对象数组** | `Arrays.sort` | `Arrays.sort(arr);` | `Arrays.sort(arr, Collections.reverseOrder());` |
> | **`List<T>` 列表 (首推)** | `list.sort` | `list.sort(null);` 或 `list.sort(Comparator.naturalOrder());` | `list.sort(Collections.reverseOrder());` |
> | **`List<T>` 列表 (兼容)** | `Collections.sort` | `Collections.sort(list);` | `Collections.sort(list, Collections.reverseOrder());` |

---

### 2. 二维数组排序（升序 / 降序 / 双关键字全覆盖）
做贪心、区间合并题时，经常需要对 `int[][] intervals` 排序：

```java
// ① 按第 0 列升序（最常见：合并区间 LC 56）
Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));

// ② 按第 0 列降序
Arrays.sort(intervals, (a, b) -> Integer.compare(b[0], a[0]));

// ③ 按第 1 列升序（用最少数量的箭引爆气球 LC 452、无重叠区间 LC 435）
Arrays.sort(intervals, (a, b) -> Integer.compare(a[1], b[1]));

// ④ 按第 1 列降序
Arrays.sort(intervals, (a, b) -> Integer.compare(b[1], a[1]));

// ⑤ 双关键字排序：第一列升序，第一列相同时按第二列降序（俄罗斯套娃信封 LC 354）
Arrays.sort(envelopes, (a, b) -> {
    if (a[0] != b[0]) {
        return Integer.compare(a[0], b[0]); // 宽度升序
    } else {
        return Integer.compare(b[1], a[1]); // 高度降序
    }
});
```

> [!danger] 致命红线：严禁用 `a - b` 做减法！
> 很多题解喜欢偷懒写 `(a, b) -> a[0] - b[0]`，**这是极度危险的习惯**！
> - 一旦数组中包含负数与正数（例如 $a = -2147483648, b = 1$），$a - b$ 会向下发生**整型下溢（Integer Overflow）**，负数减正数变成了正数，直接导致排序规则颠倒甚至报 `IllegalArgumentException: Comparison method violates its general contract`！
> - **标准解法**：始终使用 `Integer.compare(a[0], b[0])`！

---

## 二、 优先队列（PriorityQueue / 堆）极速上手

Java 中的 `PriorityQueue` **默认是小顶堆**（堆顶是最小值）。

### 1. 基础大/小顶堆声明
```java
// 1. 小顶堆（默认）
PriorityQueue<Integer> minHeap = new PriorityQueue<>();

// 2. 大顶堆（两种标准写法）
PriorityQueue<Integer> maxHeap1 = new PriorityQueue<>(Collections.reverseOrder());
PriorityQueue<Integer> maxHeap2 = new PriorityQueue<>((a, b) -> Integer.compare(b, a));
```

### 2. 存复合对象 / 数组的堆（前 K 个高频元素 LC 347）
若堆中存储的是 `int[]`（例如 `[num, count]`），按频次从小到大建小顶堆：
```java
// 堆中存 [数值, 频次]，按频次升序排（维护容量为 K 的小顶堆）
PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[1], b[1]));

for (Map.Entry<Integer, Integer> entry : map.entrySet()) {
    pq.offer(new int[]{entry.getKey(), entry.getValue()});
    if (pq.size() > k) {
        pq.poll(); // 弹出频次最小的，最终剩下前 K 个频次最大的
    }
}
```

---

## 三、 栈与队列：正确选型与操作防坑

> [!warning] 为什么必须弃用 `java.util.Stack`？
> 1. `Stack` 继承自古老的 `Vector`，所有操作均带 `synchronized` 重量级锁，性能很差；
> 2. `Stack` 破坏了栈的封装性，允许根据下标任意插入元素。
> 
> **Java 官方推荐统统使用 `Deque`（双端队列）接口的实现类 `ArrayDeque`**。

### 1. 替代栈（Stack）的标准姿势
```java
Deque<Integer> stack = new ArrayDeque<>();

stack.push(1);       // 入栈
int top = stack.peek(); // 查看栈顶
int val = stack.pop();  // 弹出栈顶
boolean empty = stack.isEmpty(); // 判空
```

### 2. 替代单向队列（Queue / BFS 必备）
```java
Queue<TreeNode> queue = new ArrayDeque<>();

queue.offer(root);      // 入队
TreeNode cur = queue.poll(); // 出队
TreeNode peek = queue.peek(); // 查看队首
int size = queue.size();     // 当前层节点数（分层 BFS 必备）
```

> [!tip]- 深入底层：为什么栈顶和队首调用的都是 peek()？（源码映射真相）
> 很多初学者常困惑：**“栈顶不是数组后面吗？队首不是数组最前面吗？为什么调用的都是同一个 `peek()` 方法？”**
> 
> **核心真相**：`ArrayDeque` 是一个**两头都能进出的双端管道（左边是 Head 头部，右边是 Tail 尾部）**。
> 
> 直接看 **JDK 官方源码**的底层映射实现：
> 
> **① 当你把它当【栈（Stack）】用时（全部在 Head 头部进出）：**
> ```java
> stack.push(x);  // 源码底层调的是：addFirst(x)    --> 从头部塞进去！
> stack.pop();    // 源码底层调的是：removeFirst() --> 从头部掏出来！
> stack.peek();   // 源码底层调的是：peekFirst()   --> 探头看一眼头部的元素（栈顶就在头部）！
> ```
> 
> **② 当你把它当【单向队列（Queue）】用时（Tail 尾部进、Head 头部出）：**
> ```java
> queue.offer(x); // 源码底层调的是：addLast(x)     --> 从尾部排进来！
> queue.poll();   // 源码底层调的是：removeFirst() --> 队伍最前面的人出队！
> queue.peek();   // 源码底层调的是：peekFirst()   --> 探头看一眼排在最前面的那个人！
> ```
> 
> **双端管道全景图解：**
> ```text
>                      【Head 头部】                     【Tail 尾部】
>                            │                               │
>                            ▼                               ▼
>                  ┌───┬───┬───┬───┐
> 双端管道容器:      │ 3 │ 2 │ 1 │   │
>                  └───┴───┴───┴───┘
>                            ▲                               ▲
>                            │                               │
> 【当栈用】：       入栈(push)/出栈(pop)/peek()                (不用这一端)
>                   全部在头部操作！栈顶就是 3
> 
> 【当队列用】：     出队(poll)/查看队首 peek()               入队(offer)
>                   从头部出！队首就是 3                      新来的从尾部排入
> ```
> 
> **设计哲学**：在 Java 规范中，`peek()` 的含义就是**“偷看一眼下一个即将被弹出的元素”**。无论栈顶还是队首，下一个被弹出的元素物理上都在 **Head 头部**，所以它们底层调用的**全都是 `peekFirst()`**！

---

## 四、 Map 与 Set 一行流神仙 API

刷题时利用现代 Java API 可以极大精简代码：

### 1. 频次统计（不用写任何 `if-else`）
```java
Map<Integer, Integer> countMap = new HashMap<>();

// 极简一行流统计出现次数
countMap.put(num, countMap.getOrDefault(num, 0) + 1);
```

### 2. 邻接表建图（多对一、一对多分组）
在图论建图或字符分组时，以前需要先判断 `map.containsKey`，现在只需一行：
```java
// u -> [v1, v2, v3]
Map<Integer, List<Integer>> graph = new HashMap<>();

// 如果键不存在，则执行后面的 lambda 放入一个空 ArrayList，然后直接 .add()
graph.computeIfAbsent(u, k -> new ArrayList<>()).add(v);
```

### 3. 一行流初始化固定集合
```java
// 创建不可变元音字母哈希集
Set<Character> vowels = Set.of('a', 'e', 'i', 'o', 'u', 'A', 'E', 'I', 'O', 'U');
if (vowels.contains(ch)) { ... }
```

---

## 五、 字符与字符串高性能速查技巧

在 LeetCode 判题机中，字符串操作极其影响执行用时。

### 1. 字符统计优先用 `int[26]` 彻底替换 `HashMap`
若字符集确定为小写字母，用数组做哈希表速度提升 **10 倍以上**：
```java
int[] cnt = new int[26];
// 计数
for (char c : s.toCharArray()) {
    cnt[c - 'a']++;
}
// 判断两个字符串是否互为字母异位词
for (char c : t.toCharArray()) {
    if (--cnt[c - 'a'] < 0) return false;
}
```

### 2. 避免循环中反复调用 `s.charAt(i)`
```java
// 慢: 每次调用 charAt 都有边界检查开销
for (int i = 0; i < s.length(); i++) {
    char c = s.charAt(i);
}

// 推荐: 先转成底层字符数组再线性遍历
char[] cs = s.toCharArray();
for (char c : cs) {
    // 效率极高
}
```

### 3. `StringBuilder` 在回溯/路径拼接中的核心用法
```java
StringBuilder sb = new StringBuilder();

sb.append("abc");
sb.append(123);

// 回溯算法中撤销最后一步选择
sb.deleteCharAt(sb.length() - 1); 

// 翻转字符串
sb.reverse();

String res = sb.toString();
```

---

## 六、 常用数值与数组工具函数

```java
// 1. 数组一键填充默认值（初始化 DP 数组等）
int[] dp = new int[n];
Arrays.fill(dp, -1);

// 2. 数组拷贝与截取
int[] copy = Arrays.copyOf(nums, nums.length);
int[] sub = Arrays.copyOfRange(nums, 1, 4); // 左闭右开 [1, 4)

// 3. 最大值与最小值简写
int maxVal = Math.max(a, Math.max(b, c));
```
