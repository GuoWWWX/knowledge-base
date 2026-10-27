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

### 2. 二维数组排序（升序 / 降序 / 双关键字全覆盖，含结果对照）
做贪心、区间合并题时，经常需要对 `int[][] intervals` 排序。以下以示例数组展示排序前后变化：

```java
// 原始输入矩阵：
// int[][] intervals = {{3, 5}, {1, 8}, {1, 2}, {2, 4}};

// ① 按第 0 列升序（最常见：合并区间 LC 56）
Arrays.sort(intervals, (a, b) -> Integer.compare(a[0], b[0]));
// 输出结果: [[1, 8], [1, 2], [2, 4], [3, 5]]

// ② 按第 0 列降序
Arrays.sort(intervals, (a, b) -> Integer.compare(b[0], a[0]));
// 输出结果: [[3, 5], [2, 4], [1, 8], [1, 2]]

// ③ 按第 1 列升序（用最少数量的箭引爆气球 LC 452、无重叠区间 LC 435）
Arrays.sort(intervals, (a, b) -> Integer.compare(a[1], b[1]));
// 输出结果: [[1, 2], [2, 4], [3, 5], [1, 8]]

// ④ 按第 1 列降序
Arrays.sort(intervals, (a, b) -> Integer.compare(b[1], a[1]));
// 输出结果: [[1, 8], [3, 5], [2, 4], [1, 2]]

// ⑤ 双关键字排序：第 0 列升序；第 0 列相同时按第 1 列降序（俄罗斯套娃信封 LC 354）
Arrays.sort(intervals, (a, b) -> {
    if (a[0] != b[0]) {
        return Integer.compare(a[0], b[0]); // 第 0 列升序
    } else {
        return Integer.compare(b[1], a[1]); // 第 1 列降序
    }
});
// 输出结果: [[1, 8], [1, 2], [2, 4], [3, 5]]  <-- 观察：同样是 1 开头，8 排在 2 前面！
```

> [!danger] 致命红线：严禁用 `a - b` 做减法！
> 很多题解喜欢偷懒写 `(a, b) -> a[0] - b[0]`，**这是极度危险的习惯**！
> - 一旦数组中包含负数与正数（例如 $a = -2147483648, b = 1$），$a - b$ 会向下发生**整型下溢（Integer Overflow）**，负数减正数变成了正数，直接导致排序规则颠倒甚至报 `IllegalArgumentException: Comparison method violates its general contract`！
> - **标准解法**：始终使用 `Integer.compare(a[0], b[0])`！

---

## 二、 优先队列（PriorityQueue / 堆）极速上手

Java 中的 `PriorityQueue` **默认是小顶堆**（堆顶是最小值）。

### 1. 基础大/小顶堆声明与出队顺序对比
```java
// 1. 小顶堆（默认：堆顶为最小值）
PriorityQueue<Integer> minHeap = new PriorityQueue<>();
minHeap.addAll(List.of(5, 1, 3, 9));
// 依次 poll() 弹出顺序: 1 -> 3 -> 5 -> 9（从小到大依次出堆）

// 2. 大顶堆（两种标准写法：堆顶为最大值）
PriorityQueue<Integer> maxHeap1 = new PriorityQueue<>(Collections.reverseOrder());
PriorityQueue<Integer> maxHeap2 = new PriorityQueue<>((a, b) -> Integer.compare(b, a));
maxHeap1.addAll(List.of(5, 1, 3, 9));
// 依次 poll() 弹出顺序: 9 -> 5 -> 3 -> 1（从大到小依次出堆）
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

刷题时利用现代 Java API 可以极大精简代码，配合输出演进极易理解：

### 1. 频次统计（`getOrDefault` 免去繁琐判断）
```java
Map<String, Integer> countMap = new HashMap<>();

// 场景演练：依次计入 "apple", "banana", "apple"
// ① 第一次遇到 "apple"：map 中没有，getOrDefault 返回默认值 0，计算 0 + 1 = 1 并存入
countMap.put("apple", countMap.getOrDefault("apple", 0) + 1);
// 此时 countMap: {"apple": 1}

// ② 第一次遇到 "banana"：
countMap.put("banana", countMap.getOrDefault("banana", 0) + 1);
// 此时 countMap: {"apple": 1, "banana": 1}

// ③ 第二次遇到 "apple"：map 中已有，getOrDefault 返回已有值 1，计算 1 + 1 = 2 并更新
countMap.put("apple", countMap.getOrDefault("apple", 0) + 1);
// 最终 countMap: {"apple": 2, "banana": 1}
```

### 2. 邻接表建图（`computeIfAbsent` 自动初始化列表）
在图论建图或字符分组时，以前需要先判断 `map.containsKey`，现在只需一行：
```java
// 目标结构：点 u -> 相邻的所有邻居列表 [v1, v2, v3...]
Map<Integer, List<Integer>> graph = new HashMap<>();

// 场景演练：添加两条边 1 -> 2 和 1 -> 3
// ① 第一次为节点 1 添加邻居 2：
// 发现 key=1 不存在，自动执行后面的 Lambda 创建一个空的 new ArrayList<>() 放入 Map，并立即将 2 添加进去
graph.computeIfAbsent(1, k -> new ArrayList<>()).add(2);
// 此时 graph: {1=[2]}

// ② 第二次为节点 1 添加邻居 3：
// 发现 key=1 已经存在，直接返回已有的 ArrayList，并追加 3
graph.computeIfAbsent(1, k -> new ArrayList<>()).add(3);
// 最终 graph: {1=[2, 3]} （全自动无空指针隐患！）
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

### 3. `StringBuilder` 在回溯/路径拼接中的核心用法（含单步输出）
```java
StringBuilder sb = new StringBuilder();

sb.append("abc");                 // 当前内容: "abc"
sb.append(123);                   // 当前内容: "abc123"

// 回溯算法中撤销最后一步选择（删除最后一个字符 '3'）
sb.deleteCharAt(sb.length() - 1); // 变换后内容: "abc12"

// 高阶技巧：一键截断恢复现场（若追加了多个字符，直接一秒回滚到指定长度）
sb.setLength(3);                  // 截断后内容: "abc"

// 翻转字符串（常用于高精度加法、回文判断）
sb.reverse();                     // 翻转后内容: "cba"

String res = sb.toString();       // 最终字符串: "cba"
```

---

## 六、 数组与 List 复制、填充与互转全指南

### 1. 数组与 List 的克隆与区间截取
```java
// =============== 数组篇 ===============
int[] nums = {10, 20, 30, 40, 50};

// ① Arrays.copyOf：克隆全新数组（指定长度，多截少补 0）
int[] copyAll = Arrays.copyOf(nums, nums.length);
// 输出: [10, 20, 30, 40, 50]
int[] copyPart = Arrays.copyOf(nums, 3);
// 输出: [10, 20, 30]

// ② Arrays.copyOfRange：截取子区间（左闭右开 [from, to)）
int[] subArr = Arrays.copyOfRange(nums, 1, 4); // 截取下标 1, 2, 3
// 输出: [20, 30, 40]（长度等于 4 - 1 = 3，不含下标 4）

// ③ System.arraycopy：原生底层拷贝（性能之王！归并排序、原地覆盖必用）
// 语法：(源数组, 源起始, 目标数组, 目标起始, 拷贝长度)
int[] dest = new int[5];
System.arraycopy(nums, 0, dest, 0, 3);
// 输出 dest: [10, 20, 30, 0, 0]

// =============== List 篇 ===============
List<Integer> list = new ArrayList<>(List.of(10, 20, 30, 40, 50));

// ① 全新独立克隆：new ArrayList<>(list)（回溯收集解必备）
List<Integer> copyList = new ArrayList<>(list);
// 输出: [10, 20, 30, 40, 50]

// ② 截取子列表：list.subList(fromIndex, toIndex)（左闭右开 [from, to)）
List<Integer> subList = list.subList(1, 4);
// 输出: [20, 30, 40]
// ⚠️ 避坑：subList 是原列表的“视图”，修改 subList 会直接影响原 list！
// 若要完全独立，必须套一层：List<Integer> independent = new ArrayList<>(list.subList(1, 4));
```

---

### 2. 批量填充与二维 DP 防坑指南
```java
// =============== 数组填充 ===============
// ① 一维数组填充
int[] dp = new int[4];
Arrays.fill(dp, -1);
// 输出: [-1, -1, -1, -1]

// ② ⚠️ 致命避坑：二维数组不能直接 Arrays.fill(matrix, -1)！
// 正确姿势：遍历每一行进行填充！
int[][] memo = new int[3][4];
for (int[] row : memo) {
    Arrays.fill(row, -1);
}
// 输出: 3 行 4 列全部填充为 -1

// =============== List 填充 ===============
// ⚠️ 避坑警告：千万不要对空 List 调 Collections.fill！
// Collections.fill 只会替换“已有元素”，对 size=0 的空 List 毫无作用！

// ✅ 刷题神器：初始化包含 N 个相同默认值的可变 List
List<Integer> initList = new ArrayList<>(Collections.nCopies(5, 0));
// 输出: [0, 0, 0, 0, 0] （可任意修改、添加）

// 对已填充的 List 进行批量覆盖：
Collections.fill(initList, -1);
// 覆盖后输出: [-1, -1, -1, -1, -1]
```

---

### 3. 数组与 List 互相极速转换（力扣返回答案必备）

> [!tip] 灵魂吐槽：为什么基本类型转换这么麻烦？
> 因为 Java 历史包袱：泛型 `List<T>` 只能装对象（`Integer`），装不了底层基本数据类型（`int`）！
> ⚠️ **新手死穴**：千万别写 `Arrays.asList(new int[]{1, 2})`，它会把整个 `int[]` 当成一个对象，变成长度为 1 的 `List<int[]>`！
> 
> **力扣刷题真相**：Stream 不仅代码长、难记，而且运行时反复自动装箱拆箱，**性能很慢（动辄增加 10ms）**。
> **真正的高手在力扣里更推荐朴素的 2 行 for 循环（0ms 击败 100%，永远不会拼错 API）！**

#### ① 力扣最推荐：返璞归真 2 行 for 循环（0ms 满分，极度推荐）
```java
// 1. List<Integer> 转 int[]（力扣返回答案最最最常用姿势！）
List<Integer> list = List.of(1, 2, 3);
int[] ans = new int[list.size()];
for (int i = 0; i < ans.length; i++) ans[i] = list.get(i);
// 输出 ans: [1, 2, 3]（代码仅 2 行，耗时 0ms 击败 100%，绝不报错）

// 2. int[] 转 List<Integer>
int[] nums = {1, 2, 3};
List<Integer> res = new ArrayList<>();
for (int x : nums) res.add(x);
// 输出 res: [1, 2, 3]
```

#### ② 现代语法糖：Stream 一行流（如果记不住就果断用上面的 for 循环）
```java
// List<Integer> 转 int[]
int[] primitiveArr = list.stream().mapToInt(i -> i).toArray();

// int[] 转 List<Integer>
List<Integer> boxedList = Arrays.stream(nums).boxed().toList();
```

#### ③ 对象与二维类型：原生就支持一行流（无需 Stream！）
由于对象类型（`String`、`int[]`）本身就是标准的 `Object`，完全没有泛型阻碍，**可以直接优雅转换**：
```java
// 1. 二维 List<int[]> 转二维数组 int[][]（合并区间 LC 56 终极神技！）
List<int[]> resList = new ArrayList<>();
resList.add(new int[]{1, 3});
resList.add(new int[]{2, 6});
int[][] resultMatrix = resList.toArray(new int[0][]);
// 输出: [[1, 3], [2, 6]]

// 2. String[] 与 List<String> 互转
String[] sArr = {"a", "b", "c"};
List<String> sList = new ArrayList<>(Arrays.asList(sArr)); // 数组转 List
String[] backToArr = sList.toArray(new String[0]);          // List 转数组
```

---

## 七、 刷题高频工具函数与防溢出必备

```java
// 1. 字符串区间截取 String.substring（左闭右开 [begin, end)）
String str = "leetcode";
String subStr = str.substring(0, 4); // 截取下标 0, 1, 2, 3
// 输出: "leet"（不包含下标 4 的 'c'）

// 2. 字符串分割与去除首尾空白
String raw = "  apple, banana, orange  ";
String[] fruits = raw.trim().split(",\\s*");
// 输出 fruits: ["apple", "banana", "orange"]

// 3. 二分查找工具 Arrays.binarySearch（必须在有序数组上使用）
int[] sorted = {2, 5, 8, 12, 19};
int idx1 = Arrays.binarySearch(sorted, 8);
// 输出: 2 （找到了，返回元素下标 2）

int idx2 = Arrays.binarySearch(sorted, 10);
// 输出: -4 （没找到！返回负数 -(插入点 + 1)）
// 技巧推导：10 应该插入在 12 的位置（下标 3），所以返回值 = -(3 + 1) = -4
// 还原插入点：int insertPos = -idx2 - 1 = 3！刷题查插槽神技！

// 4. Character 字符判断神仙 API（验证回文串 LC 125 极速解）
char ch = 'A';
boolean isAlphanumeric = Character.isLetterOrDigit(ch); // true（是否为字母或数字）
char lower = Character.toLowerCase(ch);                 // 'a'
boolean isNum = Character.isDigit('9');                 // true

// 5. 内置位运算神技（比手写 while 快数倍）
int bitCount = Integer.bitCount(13); // 13 的二进制是 1101，输出: 3（1 的个数）
String binStr = Integer.toBinaryString(13); // 输出: "1101"
int fromBin = Integer.parseInt("1101", 2);  // 输出: 13

// 6. 图论/DP 安全无穷大（防加法溢出变负数）
// ⚠️ 严禁写 int INF = Integer.MAX_VALUE;（加权值会直接溢出成负数！）
// 正确标准：
int INF = 0x3f3f3f3f; // 约 1.06 * 10^9，两倍 INF 相加不会溢出，足够代表无穷大
```
