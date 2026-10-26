---
title: 11 · 技巧-LeetCode高频速写套路与避坑秘籍
tags:
  - 领域/算法
  - 类型/技巧
  - 刷题/避坑
created: 2026-10-26
updated: 2026-10-26
status: 已完成
mastery: ⭐⭐⭐⭐⭐
---

# 11 · 技巧：LeetCode 高频速写套路与避坑秘籍

> [!tip] 核心诉求
> 为什么高手刷题又快又稳，几乎没有边界 Bug？因为他们掌握了一套**把复杂边界“降维抹平”的通用速写套路**。
> 本篇整理了刷题实战中最值钱的 6 大避坑与速写技巧，让你在写代码时行云流水。

---

## 一、 链表救命稻草：Dummy 虚拟头节点

> **痛点**：做链表题时，如果头节点 `head` 本身可能被删除、被交换，或者合并后产生新头节点，往往需要写一大堆繁琐的 `if (head == null)` 特判。

### 核心解法：无脑挂载 Dummy 节点
在原头节点前面创造一个虚拟哨兵节点 `dummy`，所有操作均从 `dummy` 开始：

```java
public ListNode deleteNode(ListNode head, int val) {
    // 1. 创建 dummy 指向真正的 head
    ListNode dummy = new ListNode(0, head);
    ListNode cur = dummy;

    // 2. 遍历检查 cur.next
    while (cur.next != null) {
        if (cur.next.val == val) {
            cur.next = cur.next.next; // 删除节点
            break;
        }
        cur = cur.next;
    }

    // 3. 返回真实的头节点，永远不会出现 head 空指针异常
    return dummy.next;
}
```

> [!note] 适用场景
> - 合并两个有序链表（LC 21）
> - 删除链表的倒数第 N 个结点（LC 19）
> - 两两交换链表中的节点（LC 24）
> - 反转链表 II（LC 92）

---

## 二、 二维网格遍历：方向向量与一行流越界检查

> **痛点**：在岛屿问题（LC 200）、走迷宫等二维网格矩阵题中，上下左右要写 4 次独立的下标增减和 4 次越界检查，代码又长又臭还容易写错变量。

### 核心解法：静态方向数组 `DIRS` + 一行流守卫
```java
public class GridSearchExample {
    // 上、下、左、右四个方向的行列偏移量
    private static final int[][] DIRS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

    public void dfs(int[][] grid, int r, int c) {
        int m = grid.length, n = grid[0].length;
        
        // 标记已访问（沉岛思想）
        grid[r][c] = 0;

        // 统一循环探索 4 个邻居
        for (int[] d : DIRS) {
            int nr = r + d[0];
            int nc = c + d[1];

            // 一行流优雅越界与条件检查！
            if (nr < 0 || nr >= m || nc < 0 || nc >= n || grid[nr][nc] == 0) {
                continue;
            }

            dfs(grid, nr, nc);
        }
    }
}
```

---

## 三、 前缀和下标偏移 1 技巧（彻底免去 `if (l == 0)`）

> **痛点**：计算区间和时，如果直接用 `pre[i]` 表示前 $i$ 项和，当求 `[0, r]` 区间时必须写 `if (l == 0) return pre[r]; else return pre[r] - pre[l - 1];`，极易发生 `l - 1 = -1` 数组越界！

### 核心解法：长度定义为 $n + 1$，令 `pre[0] = 0`

- `pre[i]` 的物理定义：**前 $i$ 个元素的和**（即原数组下标 `0` 到 `i - 1` 的和）；
- **原数组区间 `[l, r]` 的和，直接等于**：
  $$\text{sum}(l, r) = \text{pre}[r + 1] - \text{pre}[l]$$

```java
// 1. 构建前缀和数组（长度为 n + 1）
int n = nums.length;
int[] pre = new int[n + 1];
for (int i = 0; i < n; i++) {
    pre[i + 1] = pre[i] + nums[i];
}

// 2. 任意区间 [left, right] 求和，永远无需任何 if 判断！
public int querySum(int left, int right) {
    return pre[right + 1] - pre[left];
}
```

---

## 四、 位运算三大高频杀器

| 运算表达式 | 核心作用 | 经典应用力扣题 |
| :--- | :--- | :--- |
| **`n & (n - 1)`** | **抹去二进制中最右边的那个 1** | LC 191 位1的个数、LC 231 2的幂 |
| **`n & (-n)`** | **提取二进制中最右边的那个 1（lowbit）** | 树状数组、LC 52 N皇后位运算优化 |
| **`a ^ a = 0` / `a ^ 0 = a`** | **相同抵消，0 保留原值** | LC 136 只出现一次的数字 |

### 实战代码示例
```java
// 1. 快速统计整数 n 的二进制中 1 的个数（比逐位右移快数倍）
int count = 0;
while (n != 0) {
    n &= (n - 1); // 每次抹掉最右边的 1
    count++;
}

// 2. 判断一个数是否是 2 的幂次方（正数且只有一个 1）
boolean isPowerOfTwo = (n > 0) && ((n & (n - 1)) == 0);
```

---

## 五、 防溢出与大数取模法则（防暴负数大坑）

### 1. 乘法计算必须先强转 `long`
```java
int a = 1_000_000;
int b = 1_000_000;
int MOD = 1_000_000_007;

// 错误示范：a * b 会先发生 int 溢出截断，再被模，结果完全错误！
// int ans = (a * b) % MOD; 

// 正确姿势：先将其中一个因子强转为 long
int ans = (int) (((long) a * b) % MOD);
```

### 2. 减法取模防止出现负数
在模运算中，如果两个数相减，在 Java 中 `%` 会保留负号（例如 `-3 % 7 = -3`）：
```java
// 减法取模防负数标准公式：(a - b + MOD) % MOD
int diff = (a - b) % MOD;
if (diff < 0) diff += MOD;

// 或者极简一行流：
int diffSafe = ((a - b) % MOD + MOD) % MOD;
```

---

## 六、 快慢指针找链表中点（奇偶统一标准式）

归并排序链表（LC 148）和回文链表（LC 234）必须熟练找到链表中点：

```java
// 奇数个节点停在正中点，偶数个节点停在靠左的中点
ListNode slow = head, fast = head;
while (fast.next != null && fast.next.next != null) {
    slow = slow.next;
    fast = fast.next.next;
}
// slow 即为中点！若需断开链表：
// ListNode secondHalf = slow.next;
// slow.next = null;
```
