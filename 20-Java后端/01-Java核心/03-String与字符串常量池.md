---
title: String与字符串常量池
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

# String与字符串常量池

> [!abstract] 一句话总结
> String 是 Java 使用最频繁的类：不可变设计 + 常量池缓存让它在安全、缓存、线程安全上天然可靠，但代价是每次修改都产生新对象——所以拼接要用 StringBuilder；理解"不可变 + 池化"这对因果，才能解释为什么 String 是完美的 HashMap key、以及大量字符串拼接为什么会拖慢接口。

## 🎯 本节定位

- **本章第 3 课，语言地基收尾**。前置：[[02-异常处理最佳实践]]（字符串转换异常属于运行期异常）。
- 本课为下一课 [[20-Java后端/01-Java核心/04-集合框架/01-核心机制与选型]] 铺垫：String 是集合最常见的元素与 key，它的 `equals/hashCode` 实现和不可变性是"为什么 String 适合作 HashMap key"的答案（[[20-Java后端/01-Java核心/04-集合框架/04-HashMap原理与扩容]] 会用到）。
- **延伸阅读（本课不展开）**：字符编码（Charset）在 [[11-IO与NIO]] 中结合读写讲；String 常量池的 JVM 存储细节（堆/元空间、JDK 7 前后变化）在 [[20-Java后端/02-JVM/01-JVM内存区域|02-JVM 章节]]。

## 📌 这是什么

- `String` 是**不可变对象**：创建后内容不可变（JDK 8 及以前内部是 `final char[]`，JDK 9+ 是 `final byte[]` + coder 标记）。
- **字符串常量池**（String Pool）：JVM 对字符串字面量（`"abc"`）和 `intern()` 的字符串做缓存，相同内容的字符串复用同一个实例。
- `StringBuilder`（可变、线程不安全）与 `StringBuffer`（可变、方法级同步）用于高效拼接。
- 字符串字面量**本身是对象**：`"abc"` 在编译期就进入常量池，`new String("abc")` 则在堆上额外创建一个新对象。

## 🤔 解决什么问题 / 为什么需要

- **为什么不可变？** 四个原因环环相扣：
  1. **安全**：String 常作文件名、类名、URL、密码等敏感数据，被外部改掉会出漏洞；且 String 常作为方法参数传入，可变的话调用方无法保证参数不被改。
  2. **缓存 hashCode**：不可变 ⇒ 哈希值只需算一次（String 内部缓存 `int hash` 字段），HashMap 频繁 get 时省掉重复计算。
  3. **常量池复用**：只有不可变，多个引用共享同一个实例才安全——内容不会悄悄变。
  4. **线程安全**：不可变对象天然线程安全，无需同步。
- **为什么需要常量池？** 程序里大量重复字符串（如状态名、日志模板、字段名），池化让它们共享内存，避免每个字面量都新建对象。
- **为什么需要 StringBuilder？** String 不可变 ⇒ `+` 拼接每次产生新对象；循环内拼接 N 次就是 O(N²) 的复制成本，必须用可变的 StringBuilder。

## 🧩 核心概念

| 概念 | 含义 | 精通要点 |
| ---- | ---- | -------- |
| 不可变性 | 创建后内容不可变，所有"修改"都返回新对象。 | 根因是 `final` 字段 + 不提供修改方法。 |
| 字符串常量池 | JVM 缓存字符串实例的区域。 | 字面量自动入池；`intern()` 手动入池。 |
| intern() | 返回常量池中内容相等的字符串实例。 | 池中没有则放入；重复内容返回同一个引用。 |
| StringBuilder | 可变字符串，append 高效。 | 线程不安全；单线程拼接首选。 |
| StringBuffer | 可变字符串 + 方法级 synchronized。 | 线程安全但慢；现在少用。 |
| 值语义 | `equals` 比较内容，`==` 比较引用。 | 判断字符串相等永远用 `equals`。 |

## ⚙️ 工作原理 / 流程

```mermaid
flowchart TD
    A["代码: String s = \"abc\""] --> B{常量池有 \"abc\"?}
    B -- 是 --> C[直接复用池中实例]
    B -- 否 --> D[池中创建 \"abc\"]
    C --> E["s == 池中引用"]
    D --> E
    F["代码: new String(\"abc\")"] --> G[堆上新建对象<br/>内容拷贝自池中字面量]
    G --> H["== 与池中引用不同<br/>equals 相同"]
    I["循环拼接 +"] --> J[每次 new StringBuilder + 复制]
    I --> K["正确: StringBuilder.append<br/>O(n) 一次完成"]
```

**关键区分（高频考点）**：
- `"a" + "b"`：编译期常量折叠，直接变成 `"ab"`（池中一份）。
- `s1 + s2`（变量）：javac 编译成 `new StringBuilder().append(s1).append(s2).toString()`。
- `new String("abc")`：创建**两个**对象——池中的 `"abc"` 和堆上的新对象（但若池中已有则只新建堆上那个）。

## 🔍 源码 / 原理深挖

### String 的内存结构（JDK 9+）

```java
// JDK 8: private final char value[];
// JDK 9+: private final byte[] value;  private final byte coder;  // LATIN1 / UTF16
```

JDK 9 引入 **Compact Strings**：纯 ASCII 字符串用 `byte[]`（Latin-1，1 字节/字符）存储，只有含中文等多字节字符才用 UTF-16（2 字节/字符），堆内存平均省一半。这也是为什么 `"abc".length()` 返回 3 而 `"中文".length()` 返回 2——length 按**字符**数算，不是字节数（字节数用 `getBytes().length`）。

### equals 为什么快？

`String.equals` 先比引用、再比 `byte[]` 长度、最后逐字节比（JDK 9+ 还先比 coder 是否一致）。`hashCode` 公式 `s[0]*31^(n-1) + ... + s[n-1]`：用质数 31 是为了让哈希分布更均匀（31 奇素数，`31*i` 可优化为 `(i<<5)-i`），且**结果缓存**在 `hash` 字段（不可变才有这个资格）。

### 为什么 String 是完美的 HashMap key

HashMap 的 key 要求（见 [[20-Java后端/01-Java核心/04-集合框架/04-HashMap原理与扩容]]）：**hashCode 稳定 + equals 语义正确 + 不可变**。String 三点全占：
1. 不可变 ⇒ put 之后内容不会变 ⇒ 桶位不会错乱；
2. hashCode 缓存 ⇒ 重复 get 不重复计算；
3. equals/hashCode 实现正确且配套 ⇒ 去重、查找可靠。

### StringBuilder 为什么比 + 快

`"a" + b + c` 在**循环内**每轮都会 new 一个 StringBuilder 再 toString（临时对象 + 复制）；直接 `StringBuilder.append` 在循环外建一次，扩容策略（初始 16、不够翻倍+2）类似 ArrayList，整体 O(n)。

### 什么时候用 StringBuffer？

方法级 synchronized，只有**多个线程共享同一个可变字符串**时才需要（这种场景极少，通常可用不可变 String 或干脆不用共享可变状态）；单线程一律 StringBuilder。

## 💻 代码实践 / 排查命令

```java
// 1. 循环拼接必须用 StringBuilder（10 万次差距巨大）
StringBuilder sb = new StringBuilder(1024);   // 预估容量减少扩容
for (int i = 0; i < 100_000; i++) sb.append(i).append(',');
String result = sb.toString();

// 2. 判断相等永远 equals
String a = "abc";
String b = new String("abc");
System.out.println(a == b);        // false（引用不同）
System.out.println(a.equals(b));   // true（内容相同）
System.out.println(a == "abc");    // true（字面量在池中复用）
System.out.println(a == b.intern()); // true（intern 返回池中实例）

// 3. 大量重复字符串去重：intern 谨慎使用（见踩坑）
```

```bash
# 排查字符串相关内存：大字符串数组、重复内容
jmap -histo:live <pid> | findstr "String"
# char[]/byte[] 数量异常多 → 检查是否循环拼接、是否大量重复字符串未去重
```

## 🛠️ 排查 / 实践要点

- **慢接口排查**：循环内 `s += x` 是典型的 O(N²) 隐患，`jstack` 看热点栈 + 代码审查定位；大循环拼接用 StringBuilder 并预估容量。
- **内存排查**：`jmap -histo` 看 `char[]/byte[]` 占比；日志、错误码等大量重复字符串可 `intern()` 去重，但**池是全局的、不可回收**，滥用会撑满堆（见踩坑）。
- **编码排查**：`getBytes()` 不带 Charset 会用平台默认编码（Windows GBK），跨环境不一致——统一 `StandardCharsets.UTF_8`（详见 [[11-IO与NIO]]）。
- **== 误用排查**：看到 `==` 比字符串多半是 bug；`equals` 也可能 NPE（用 `"常量".equals(var)` 或 `Objects.equals`）。

## ⚠️ 踩坑与注意事项

- 用 `==` 判断字符串相等：字面量 + 常量池会"碰巧"相等（`a == "abc"` 为 true），变量拼接或 `new String` 后就 false——**永远用 equals**。
- 循环内用 `+` 拼接字符串，复杂度退化为 O(N²)。
- 滥用 `intern()`：常量池里的字符串**不参与常规 GC 回收**，把用户输入、动态内容全 intern 会撑爆堆。
- `StringBuffer` 当默认选择：无并发共享时它只是白白加锁。
- 用 `new String("abc")` 制造无意义对象：除非确实需要独立引用，直接写字面量。
- 拼接 null：`"a" + null` 结果是 `"anull"`（字符串），而 `StringBuilder.append(null)` 会追加 `"null"` 文本——行为一致但要注意业务语义。

## 🎯 面试问题

> [!question] 高频问题
> - Q: 为什么 String 设计成不可变的？
>   - A: 安全（参数/敏感数据不可被改）、可缓存 hashCode（HashMap 高效）、常量池共享才安全、天然线程安全。根因是 final 字段 + 无修改方法。
> - Q: `"a" + b` 在底层怎么执行？循环拼接为什么慢？
>   - A: 变量拼接编译成 `new StringBuilder().append("a").append(b).toString()`；循环内每轮都 new StringBuilder 并复制，整体 O(N²)。应循环外建一个 StringBuilder。
> - Q: `new String("abc")` 创建了几个对象？`intern()` 是什么？
>   - A: 若池中无 "abc"，创建 2 个：池中字面量对象 + 堆上 new 的对象；池中有则只建 1 个。`intern()` 返回常量池中内容相等的实例（没有就放入），让相同内容的字符串共享引用。
> - Q: 为什么 String 适合作 HashMap 的 key？
>   - A: 不可变（入 map 后内容不会变，桶位稳定）+ hashCode 缓存（重复 get 快）+ equals/hashCode 正确配套（去重查找可靠）。

## 🧭 源码入口速查

- `String#equals`：先比引用 → 比长度 → 逐字节比。
- `String#hashCode`：31 质数哈希 + `hash` 字段缓存。
- `String#intern`：常量池查询/放入，本地方法（`StringTable`）。
- `StringBuilder#append`：`byte[]` 不够时 `ensureCapacityInternal` 扩容（翻倍+2）。
- `StringCoding`：Charset 编码/解码入口。

## 🧪 最小验证

```java
// 1. 不可变：所有"修改"都返回新对象
String s = "hello";
String t = s.toUpperCase();
System.out.println(s);   // hello（原对象没变）
System.out.println(t);   // HELLO

// 2. 常量池复用
String x = "java";
String y = "java";
System.out.println(x == y);          // true（池中同一实例）
String z = new String("java");
System.out.println(x == z);          // false（堆上新对象）
System.out.println(x == z.intern()); // true（intern 回到池中实例）

// 3. 拼接性能对比（可自行计时）
StringBuilder sb = new StringBuilder();
for (int i = 0; i < 100_000; i++) sb.append(i);
String bad = "";
for (int i = 0; i < 100_000; i++) bad += i;   // O(N²)，明显更慢
```

生产里看到字符串相关慢接口或内存上涨：先查循环内拼接（`+=`）、`==` 误用、`new String` 泛滥、`getBytes()` 无 Charset；大字符串集合用 `jmap -histo` 看 `byte[]/char[]` 分布。

## ✅ 精通检查清单

- 能用自己的话讲清本主题解决的问题和适用边界。
- 能指出至少 2 个源码入口或底层机制。
- 能给出一个最小代码示例或一组线上排查命令。
- 能回答本页列出的高频面试题，并补充项目中的真实取舍。

## 🔗 关联笔记

- 本课内：上一课 [[02-异常处理最佳实践]]；应用见 [[20-Java后端/01-Java核心/04-集合框架/01-核心机制与选型]]、[[20-Java后端/01-Java核心/04-集合框架/04-HashMap原理与扩容]]（String 作 key）
- 延伸：[[11-IO与NIO]]（Charset 编码）；[[20-Java后端/02-JVM/01-JVM内存区域|JVM 内存区域]]（常量池存放位置）

## 📚 参考资料

- [Dev.java Tutorials](https://dev.java/learn/)
- [Oracle Java Documentation](https://docs.oracle.com/en/java/)
- [JDK 源码 String.java](https://github.com/openjdk/jdk/blob/master/src/java.base/share/classes/java/lang/String.java)
