---
title: JIT编译与优化
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/JVM
  - 阶段/02
created: 2026-08-09
updated: 2026-08-09
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# JIT 编译与优化

> [!abstract] 一句话总结
> Java 号称"解释型语言"却能接近 C++ 性能，靠的就是 **JIT（Just-In-Time）即时编译**：解释器先跑，JVM 统计出"热点代码"后交给 C1/C2 编译成本地机器码，再叠加**方法内联、逃逸分析、标量替换、锁消除**等优化。理解 JIT 才能解释"为什么代码跑一会儿突然变快""为什么 benchmark 要预热"。

## 🎯 本节定位

- **02 章第 9 课**。前置：[[02-对象创建与内存布局]]（逃逸分析会改变对象的分配位置）、[[01-JVM内存区域]]（Code Cache 也占内存）。
- **为什么要学**：JIT 解释了三件日常困惑——①代码越跑越快 ②benchmark 必须预热 ③线上重启后前几分钟性能差。
- **面试重点**：解释执行 vs 编译执行、C1/C2 分层编译、热点探测机制、方法内联、逃逸分析三大优化。

---

## 一、为什么需要 JIT（先建立直觉）

### 三种执行方式的取舍

```
纯解释执行（早期 JVM）：
  字节码 → 解释器逐条翻译成机器指令 → 执行
  优点：启动快，无编译开销
  缺点：慢（每次执行都要重新翻译）

纯编译执行（AOT，如 C++）：
  源码 → 编译期全部编译成机器码 → 执行
  优点：运行快
  缺点：编译慢、无法根据运行时信息优化、失去跨平台

JIT 混合模式（现代 JVM 默认）：
  先解释执行 → 统计哪段代码执行频繁（热点）→ 把热点编译成机器码 → 之后直接跑机器码
  优点：启动快 + 热点代码快 + 能用运行时信息做激进优化
```

> [!important] JIT 的核心优势：运行时信息
> AOT 编译器只能看到源码，JIT 能看到**真实运行数据**——哪个分支走得多、哪个方法实际是什么类型、循环真实迭代多少次。所以 JIT 能做 AOT 做不到的优化（如根据实际类型做单态内联）。

---

## 二、分层编译（Tiered Compilation）

JVM 有两个 JIT 编译器，JDK 8+ 默认**同时使用**（分层编译）：

| 编译器 | 别名 | 特点 | 适用 |
| ---- | ---- | ---- | ---- |
| **C1** | Client Compiler | 编译快、优化少 | 快速获得中等性能 |
| **C2** | Server Compiler | 编译慢、优化激进 | 长期运行的热点代码 |

### 五个编译层次

```
Level 0：解释执行（Interpreter）
   ↓ 方法有点热
Level 1：C1 编译，不带 profiling（已确定不会更热的简单方法）
Level 2：C1 编译，带轻量 profiling
Level 3：C1 编译，带完整 profiling ← 大部分方法先到这里
   ↓ 收集够了 profiling 数据，确认是真热点
Level 4：C2 编译，用 profiling 数据做激进优化 ← 最终形态
```

**典型路径**：`Level 0（解释）→ Level 3（C1 + profiling）→ Level 4（C2 激进优化）`

```bash
# 观察 JIT 编译行为
-XX:+PrintCompilation           # 打印每次编译（方法名 + 层次）
# 输出示例：
#   1234  456 %  4  com.example.Service::process @ 12 (89 bytes)
#         ↑    ↑  ↑
#      编译ID  OSR 层次(4=C2)

-XX:-TieredCompilation          # 关闭分层，只用 C2（不推荐，启动慢）
-XX:TieredStopAtLevel=1         # 只到 C1（启动快、峰值性能低，适合短命进程/Serverless）
```

---

## 三、热点探测：JVM 怎么知道哪里"热"

HotSpot 用**基于计数器的热点探测**，每个方法有两个计数器：

| 计数器 | 统计什么 | 默认阈值（分层编译下动态调整） |
| ---- | ---- | ---- |
| **方法调用计数器** | 方法被调用多少次 | C1: ~2000，C2: ~15000 |
| **回边计数器** | 循环体执行多少次（"回边"= 循环跳回） | 用于触发 OSR |

```java
void process() {              // 方法调用计数器 +1
    for (int i = 0; i < 10000; i++) {   // 每次循环回跳，回边计数器 +1
        doWork();
    }
}
```

### OSR（On-Stack Replacement，栈上替换）

```
问题：一个方法只调用了 1 次，但内部循环 1000 万次——方法调用计数器才 1，
     按方法计数不会被编译，但这个循环明显是热点！

解法：OSR —— 回边计数器超阈值时，JVM 编译"循环体"，
     并在方法执行中途把栈帧从"解释执行版"替换成"编译执行版"。
     这就是"栈上替换"：方法还在栈上跑着，就换了实现。
```

> [!note] 面试点
> `-XX:+PrintCompilation` 输出里带 `%` 号的就是 OSR 编译。看到大量 OSR 说明有长循环。

---

## 四、三大核心优化（面试必考）

### 4.1 方法内联（Method Inlining）—— 最重要的优化

```java
// 优化前
int add(int a, int b) { return a + b; }
int calc() { return add(1, 2) * 3; }

// JIT 内联后（把方法体直接嵌进调用处）
int calc() { return (1 + 2) * 3; }
// 进一步常量折叠 → return 9;
```

**为什么内联是"优化之母"**：
- 消除方法调用开销（压栈、跳转、返回）；
- **更重要**：内联后代码在一起，才能触发其他优化（常量折叠、逃逸分析、死代码消除）。

**内联的限制**：

| 参数 | 默认值 | 含义 |
| ---- | ---- | ---- |
| `-XX:MaxInlineSize` | 35 字节码 | 普通方法超过这个大小不内联 |
| `-XX:FreqInlineSize` | 325 字节码 | 热点方法可以内联到这么大 |
| `-XX:MaxInlineLevel` | 15 | 内联嵌套深度上限 |

```bash
-XX:+PrintInlining      # 打印内联决策（需配合 -XX:+UnlockDiagnosticVMOptions）
# 输出会说明为什么没内联：too big / not inlineable / hot method too big
```

> [!warning] 写代码的启示
> **小方法是好事**——不用担心方法调用开销，JIT 会内联掉。但**巨型方法（>325 字节码）永远无法被内联**，反而阻碍优化。所以"为了性能把代码堆成一个大方法"是错的。

**虚方法能内联吗？**

```java
interface Animal { void sound(); }
// 如果运行时只加载了一个实现类 Dog → 单态（monomorphic）→ JIT 可以内联
// 如果有 Dog/Cat 两个实现 → 双态（bimorphic）→ 可以内联两个分支 + 类型检查
// 如果有 3+ 个实现 → 多态（megamorphic）→ 无法内联，走虚方法表
```

这也是"接口默认只有一个实现时性能最好"的原因。

### 4.2 逃逸分析（Escape Analysis）

判断一个对象的作用域**是否逃出当前方法**：

```java
// 不逃逸：对象只在方法内使用
void noEscape() {
    StringBuilder sb = new StringBuilder();   // 不逃逸
    sb.append("a").append("b");
    String s = sb.toString();                 // sb 本身没有传出去
}

// 方法逃逸：对象被传给其他方法/返回
StringBuilder methodEscape() {
    StringBuilder sb = new StringBuilder();
    return sb;                                // 逃出方法了
}

// 线程逃逸：对象被其他线程访问
static StringBuilder shared;
void threadEscape() {
    shared = new StringBuilder();             // 逃到堆共享区
}
```

基于逃逸分析结果，JIT 可以做三种优化：

### 4.3 逃逸分析带来的三大优化

**① 标量替换（Scalar Replacement）—— 对象不进堆！**

```java
class Point { int x, y; }

void calc() {
    Point p = new Point();   // 不逃逸
    p.x = 1;
    p.y = 2;
    System.out.println(p.x + p.y);
}

// JIT 优化后（对象被"拆散"成局部变量，根本不创建对象）：
void calc() {
    int x = 1;       // 直接在栈帧的局部变量表里
    int y = 2;
    System.out.println(x + y);
}
// → 零堆分配，零 GC 压力！
```

> [!important] 这解释了"对象一定在堆上分配吗"
> **不一定**。标量替换后对象被拆成基本类型放进栈帧局部变量表，根本没有对象产生。这是"栈上分配"说法的真实机制（HotSpot 没有真的实现"把完整对象放栈上"，而是拆散成标量）。

**② 锁消除（Lock Elision）**

```java
// 优化前
void appendStrings() {
    StringBuffer sb = new StringBuffer();   // StringBuffer 方法都是 synchronized
    sb.append("a");                          // 但 sb 不逃逸，不可能有竞争
    sb.append("b");
}

// JIT 发现 sb 不会线程逃逸 → 消除所有 synchronized
// 结果：StringBuffer 在这里和 StringBuilder 一样快
```

**③ 栈上分配的相关参数**

```bash
-XX:+DoEscapeAnalysis      # 开启逃逸分析（JDK 7+ 默认开启）
-XX:+EliminateAllocations  # 开启标量替换（默认开启）
-XX:+EliminateLocks        # 开启锁消除（默认开启）
-XX:-DoEscapeAnalysis      # 关闭后可对比性能差异（实验用）
```

### 4.4 其他重要优化（了解即可）

| 优化 | 说明 |
| ---- | ---- |
| **常量折叠** | `3 * 4` 编译期算成 `12` |
| **死代码消除** | 永远不执行的分支直接删掉 |
| **循环展开** | 把循环体复制多份减少跳转 |
| **锁粗化** | 相邻的多个同步块合并成一个大同步块（减少反复加解锁） |
| **数组边界检查消除** | 能证明索引不越界时跳过检查 |
| **公共子表达式消除** | 重复计算只算一次 |

---

## 五、去优化（Deoptimization）—— JIT 也会"猜错"

JIT 的激进优化基于**假设**，假设被打破时必须回退：

```java
interface Handler { void handle(); }

// 运行前期只有 FastHandler 一个实现 → JIT 内联了 FastHandler.handle()
// 运行到一半，动态加载了 SlowHandler → 之前的内联假设失效！
// → JVM 触发去优化：丢弃编译代码，回退到解释执行，重新收集 profiling 后再编译
```

**常见去优化触发原因**：
- 新类加载破坏了单态假设（如上例）
- 分支预测错误（之前从不走的分支开始走了）
- 出现未预期的异常路径
- 类型检查失败（假设是 String 结果来了 Integer）

```bash
-XX:+PrintCompilation      # 输出里带 "made not entrant" / "made zombie" 就是去优化
```

> [!note] 生产启示
> 大量去优化会让性能忽高忽低。如果线上性能抖动，可以看 `PrintCompilation` 里是否有反复的编译-去优化循环（通常是动态代理/反射大量生成类导致）。

---

## 六、Code Cache（编译后的机器码存哪）

JIT 编译出的本地代码存在 **Code Cache**（属于本地内存，不在堆里）：

```bash
-XX:ReservedCodeCacheSize=256m   # Code Cache 上限（默认 240m）
-XX:InitialCodeCacheSize=16m     # 初始大小
```

**Code Cache 满了会怎样**：
```
CodeCache is full. Compiler has been disabled.
→ JIT 停止工作，所有代码退回解释执行 → 性能断崖式下跌！
```

排查：
```bash
jcmd <pid> Compiler.codecache        # 看 Code Cache 使用情况
jcmd <pid> VM.native_memory summary  # Code Cache 也计入 RSS（见 01 篇内存账本）
```

常见原因：大量动态生成类（Groovy 脚本、频繁热部署、大量 CGLIB 代理）。

---

## 💻 实践：观察 JIT 行为

```bash
# 1. 看哪些方法被编译到哪一层
java -XX:+PrintCompilation -jar app.jar 2>&1 | head -50

# 2. 看内联决策（为什么某方法没被内联）
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining -jar app.jar

# 3. 对比逃逸分析开关的性能差异（验证标量替换效果）
java -XX:+DoEscapeAnalysis  -jar bench.jar   # 默认
java -XX:-DoEscapeAnalysis  -jar bench.jar   # 关闭对比

# 4. Serverless / 短命进程优化（只用 C1，启动快）
java -XX:TieredStopAtLevel=1 -jar app.jar

# 5. Code Cache 监控
jcmd <pid> Compiler.codecache
```

### 为什么 Benchmark 必须预热

```java
// ❌ 错误的性能测试
long start = System.nanoTime();
for (int i = 0; i < 1000; i++) doWork();   // 前几百次还在解释执行！
long cost = System.nanoTime() - start;      // 测出来的是"解释执行 + 编译中"的混合速度

// ✅ 正确做法：先预热让 JIT 编译完成，再测量
for (int i = 0; i < 100_000; i++) doWork();  // 预热
long start = System.nanoTime();
for (int i = 0; i < 1000; i++) doWork();     // 此时已是 C2 编译后的机器码
```

这就是为什么要用 **JMH（Java Microbenchmark Harness）** 做基准测试——它自动处理预热、死代码消除、常量折叠等 JIT 干扰因素。

---

## ⚠️ 踩坑与注意事项

- **拿未预热的数据下性能结论**：JVM 前几千次调用是解释执行，比稳态慢一个数量级。
- **为了"减少方法调用"写巨型方法**：超过 325 字节码永远无法内联，反而阻碍所有后续优化。
- **忽略 Code Cache 满**：大量动态类生成会打满 Code Cache，JIT 停摆导致性能断崖。
- **以为 `new` 一定分配堆内存**：不逃逸的对象会被标量替换，零分配。
- **线上重启后立刻打满流量**：JIT 还没编译完，此时性能最差。生产应该配合**预热流量 + 慢启动**（如 Nacos/K8s 的 readiness 延迟）。

## 🎯 面试问题

> [!question] 高频问题
> - **Q: Java 是解释型还是编译型语言？**
>   - A: 两者结合。源码先编译成字节码（javac），运行时字节码由解释器解释执行；JVM 通过热点探测把频繁执行的代码用 JIT（C1/C2）编译成本地机器码。所以既有解释执行的启动优势，又有编译执行的峰值性能。
> - **Q: C1 和 C2 有什么区别？分层编译是什么？**
>   - A: C1 编译快优化少，C2 编译慢优化激进。分层编译让代码从 Level 0（解释）→ Level 3（C1 + profiling）→ Level 4（C2 用 profiling 激进优化）逐级提升，兼顾启动速度和峰值性能。
> - **Q: JVM 怎么判断热点代码？**
>   - A: 基于计数器——方法调用计数器（方法被调用次数）和回边计数器（循环回跳次数）。超过阈值就触发编译；回边计数器超阈值时用 OSR（栈上替换）在方法执行中途替换成编译版本。
> - **Q: 什么是方法内联？为什么它最重要？**
>   - A: 把被调用方法的方法体直接嵌入调用处。除了省掉调用开销，更重要的是内联后代码在一起，才能进一步触发常量折叠、逃逸分析、死代码消除等优化——所以它是"优化之母"。方法超过 `FreqInlineSize`（325字节码）无法内联。
> - **Q: 逃逸分析能带来哪些优化？**
>   - A: 三个——①标量替换：不逃逸对象被拆成局部变量，不进堆、零 GC 压力；②锁消除：不会线程逃逸的对象上的 synchronized 被删除（StringBuffer 在局部使用时和 StringBuilder 一样快）；③栈上分配（HotSpot 通过标量替换实现）。
> - **Q: 对象一定在堆上分配吗？**
>   - A: 不一定。逃逸分析确认对象不逃出方法后，JIT 会做标量替换——把对象字段直接拆成栈帧局部变量表里的基本类型，根本不创建对象。
> - **Q: 什么是去优化（Deoptimization）？**
>   - A: JIT 的激进优化基于运行时假设（如"这个接口只有一个实现"）。假设被打破时（新类加载、分支预测失败、类型检查失败），JVM 必须丢弃编译代码回退到解释执行，重新 profiling 再编译。反复去优化会造成性能抖动。
> - **Q: 为什么性能测试要预热？**
>   - A: JVM 前几千次调用还在解释执行或 C1 阶段，比 C2 编译后的稳态性能慢一个数量级。不预热测出来的是"解释执行 + 编译中"的混合数据，没有参考价值。生产应用同理——重启后前几分钟性能最差，需要预热流量。

## ✅ 精通检查清单

- 能解释为什么 Java 既是解释型又是编译型，JIT 相对 AOT 的独特优势（运行时信息）。
- 能说出分层编译的 5 个层次和典型路径（0 → 3 → 4）。
- 能说清热点探测的两个计数器和 OSR 的作用。
- 能解释方法内联为什么是"优化之母"，以及巨型方法为什么伤性能。
- 能说出逃逸分析的三大优化（标量替换/锁消除/栈上分配）。
- 能解释去优化的触发原因和对性能抖动的影响。
- 能说出 Code Cache 满会导致什么后果和怎么排查。

## 🔗 关联笔记

- [[02-对象创建与内存布局]] —— 标量替换让对象不进堆，改变了对象分配位置
- [[01-JVM内存区域]] —— Code Cache 属于本地内存，计入 RSS 账本
- `10-字节码与执行引擎（待建设）` —— JIT 编译的输入就是字节码，栈帧结构决定了 OSR 怎么替换
- [[06-JVM调优实战]] —— JIT 参数调优（TieredStopAtLevel / Code Cache）
- [[../01-Java核心/10-Lambda与Stream|Lambda 与 Stream]] —— invokedynamic 与 Lambda 的 JIT 内联
- [[../03-并发编程/03-volatile与synchronized|volatile 与 synchronized]] —— 锁消除、锁粗化是 JIT 层面的锁优化
- [[_index]]

## 📚 参考资料

- 深入理解 Java 虚拟机（周志明）第 11 章：后端编译与优化
- [JMH - Java Microbenchmark Harness](https://github.com/openjdk/jmh)
- [HotSpot JIT 编译器文档](https://docs.oracle.com/en/java/javase/21/vm/java-hotspot-virtual-machine-performance-enhancements.html)
- `java -XX:+PrintFlagsFinal -version | grep -i inline`：查看内联相关默认参数
