---
title: Lambda与Stream
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Java核心
  - 阶段/01
created: 2026-07-04
updated: 2026-08-06
status: 已完成
mastery: ⭐⭐⭐☆☆
---
# Lambda与Stream

> [!abstract] 一句话总结
> Lambda 的底层是 **invokedynamic + LambdaMetafactory 运行期生成**（不是匿名内部类）；Stream 的底层是 **AbstractPipeline 流水线 + Sink 链反向构建**——中间操作只是往链表上挂 stage（惰性），终端操作才反向组装 Sink 链并遍历数据源，元素逐个走完整条链（融合）。理解"字节码怎么变"和"数据流怎么走"是本课的灵魂。

## 🎯 本节定位

- **本章第 10 课，数据处理**。前置：[[01-泛型与类型擦除]]（函数式接口的泛型签名、PECS）、[[20-Java后端/01-Java核心/04-集合框架/01-核心机制与选型]]（Stream 的源头是集合）。
- **延伸阅读（本课不展开）**：并行流底层的 ForkJoinPool 与线程模型在 [[20-Java后端/03-并发编程/09-CompletableFuture异步编程|03-并发编程章节]]；本课只讲边界，不深入线程调度。

## 📌 这是什么

- **Lambda**：函数式接口（只有一个抽象方法的接口）的匿名实现，如 `x -> x * 2`。**编译产物不是匿名内部类，而是 `invokedynamic` 调用点**（下文"工作原理"深挖）。
- **Stream**：不是集合、不存储数据，它是**数据处理流水线的描述**：数据源 → 中间操作链 → 终端操作触发执行。底层是一条 `AbstractPipeline` 链表。
- 四类函数式接口记牢：`Predicate<T>`（判断，返回 boolean）、`Function<T,R>`（转换）、`Consumer<T>`（消费，无返回）、`Supplier<T>`（生产，无入参）。

## 🤔 解决什么问题 / 为什么需要

- 让集合处理代码更短、更接近业务语义：`filter/map/reduce/groupingBy` 直接表达"过滤、转换、归约、分组"。
- 通过组合操作减少临时变量和外部可变状态（更不容易出并发 bug）。
- 惰性 + 短路让流水线只算必要部分：`findFirst` 找到即停，不遍历全量。
- 理解边界才能避免性能误判和副作用事故（并行流的坑）。

## 🧩 核心概念

| 概念          | 含义                                                | 精通要点                                            |
| ----------- | ------------------------------------------------- | ----------------------------------------------- |
| 函数式接口       | `@FunctionalInterface` 标注的单抽象方法接口。                | Predicate/Function/Consumer/Supplier 四大天王。      |
| 变量捕获        | Lambda 能读取外层局部变量，但必须是 effectively final。          | 为什么？Lambda 可能在其他线程执行，变量是拷贝，改外层变量无意义且不安全。        |
| 中间操作        | filter/map/sorted/distinct 等，返回新 Stream。          | **惰性**：只是往 Pipeline 链表上挂一个新 stage。              |
| 终端操作        | collect/reduce/forEach/count 等，触发执行。              | 触发时反向构建 Sink 链；Stream 执行后不可复用。                  |
| 短路操作        | findFirst/findAny/anyMatch/limit。                 | 短路 Sink 返回 false 让上游停止，可大幅减少计算。                 |
| Spliterator | 数据源遍历器 + 拆分器。                                     | 并行流靠 `trySplit()` 把数据拆给多个线程。                    |
| 收集器         | `Collectors.toList/groupingBy/partitioningBy...`。 | collect 的底层是 `Collector` 接口的 accumulate/finish。 |

## ⚙️ 工作原理（深度）

### 一、Lambda 底层：invokedynamic，不是匿名内部类

#### 1.1 匿名内部类是怎么做的（对比基准）

```java
// 老写法：匿名内部类
list.forEach(new Consumer<String>() {
    @Override 
    public void accept(String x) { System.out.println(x); }
});
```

javac 会生成一个**独立的 class 文件**（如 `Main$1.class`），且**每次执行到 new 都创建一个新实例**。缺点：

1. **多一个类文件**——类加载、元数据占用；
2. **每次 new 一个新对象**——无法复用；
3. **接口方法调用是虚调用**——性能略差（JIT 能优化，但没到"直接内联目标方法"的程度）。

#### 1.2 现代 javac：invokedynamic 调用点

```java
list.forEach(x -> System.out.println(x));
```

javac 把它编译成一条 `invokedynamic` 指令，并附带一个引导方法（bootstrap method）引用：

```text
// 字节码层面（javap -c 可见）
invokedynamic #Lambda$0 : accept(LambdaMetafactory$metafactory)
```

这条指令的意思是：**"运行期第一次执行到这里时，调用 `LambdaMetafactory.metafactory` 把 Lambda 变成真正的函数对象（CallSite），之后每次执行直接复用这个 CallSite"**。

#### 1.3 LambdaMetafactory 做了什么

`metafactory` 在**运行期**用字节码生成技术（ASM）当场生成一个实现类（不是编译期 class 文件），核心产物：

| 产物 | 说明 |
| ---- | ---- |
| `CallSite` | 惰性缓存：**第一次引导后，之后每次执行直接取用，不再走 metafactory** |
| 生成的实现类 | 实现 `Consumer`，`accept()` 内部直接调用目标方法（`System.out.println`），**类似内联，不是虚调用** |
| 捕获字段 | 若 Lambda 捕获了外层变量，生成的实例持有这些字段（拷贝） |

```java
// 无捕获的 Lambda：生成的实例是单例，可无限复用
// x -> System.out.println(x)  不引用任何外层变量
// → metafactory 生成的类实例被 JVM 缓存，等价于一个静态单例

// 有捕获的 Lambda：每次执行 new 一个实例，但类只生成一次
// threshold -> x -> x > threshold  捕获了 threshold
// → 类生成一次，实例按调用次数创建（携带不同的捕获值）
```

#### 1.4 为什么"性能接近手写"

- **无独立类文件**：类在运行期生成，不占编译产物；
- **调用点一次引导**：`invokedynamic` 首次引导后，JVM 直接把调用点绑定到生成的实现，后续是**直接调用**（甚至内联），不是接口虚调用；
- **无捕获时零分配**：单例复用，不 new 对象。

> [!note] 面试一句话
> Lambda 编译成 `invokedynamic` + 引导方法 `LambdaMetafactory`，运行期生成函数对象并缓存 CallSite：无独立类文件、无捕获时单例复用、引导后直接内联调用。匿名内部类是编译期 class 文件 + 每次 new，两者不是一回事。

#### 1.5 方法引用

`x -> System.out.println(x)` ≡ `System.out::println`，是同一机制的语法糖。四种形态：

```java
类::静态方法       // Math::max       ≡ (a,b) -> Math.max(a,b)
对象::实例方法     // obj::method     ≡ x -> obj.method(x)
类::实例方法       // String::length  ≡ s -> s.length()   （第一个参数当 receiver）
类::new            // ArrayList::new  ≡ () -> new ArrayList<>()   （构造器引用）
```

#### 1.6 为什么只能捕获 effectively final 变量？

Lambda 编译后可能被**延迟到其他线程执行**（并行流、异步任务）。JVM 的实现是**拷贝**捕获变量到 Lambda 对象里：若允许修改外层变量，修改只发生在拷贝的内层变量上，外层看不见，语义混乱。所以编译器强制：捕获的局部变量要么 `final`，要么从未被重新赋值（effectively final）。`int[] arr` 这类"包装"技巧能改内容，但那是绕过规则的下策。

---

### 二、Stream 底层：Pipeline 链表 + Sink 链

#### 2.1 三个环节：建源 → 挂 stage → 终端触发

```java
List<String> list = List.of("apple", "banana", "cherry");

list.stream()                                   // ① 建源：集合的 StreamSupport 生成 Head stage
    .filter(s -> s.length() > 5)                // ② 挂 stage 1（中间操作，不执行）
    .map(String::toUpperCase)                   // ② 挂 stage 2（中间操作，不执行）
    .sorted()                                   // ② 挂 stage 3（stateful，不执行）
    .toList();                                  // ③ 终端操作：这才真正触发执行
```

**关键**：`filter/map/sorted` 每调用一次，只是在 `AbstractPipeline` 链表上**追加一个新 stage**，把引用记下来，**完全没碰数据**。数据源到终端操作之间是一条"只记录了操作顺序"的链表。

```text
Head → filterStage → mapStage → sortedStage
（链表：每个 stage 有 previousStage / nextStage / sourceStage 引用）
```

#### 2.2 中间操作为什么是惰性的：源码看 filter

```java
// ReferencePipeline#filter（简化）
@Override
public final Stream<P_OUT> filter(Predicate<? super P_OUT> predicate) {
    Objects.requireNonNull(predicate);
    // 返回一个新的 StatelessOp，并把 this 设为它的 previousStage
    return new StatelessOp<>(this, StreamShape.REFERENCE, StreamOpFlag.NOT_SIZED) {
        @Override
        Sink<P_OUT> opWrapSink(int flags, Sink<P_OUT> sink) {
            // 每个 stage 都要实现：把自己的逻辑包装成一个 Sink
            return new Sink.ChainedReference<>(sink) {
                @Override
                public void accept(P_OUT u) {
                    if (predicate.test(u)) downstream.accept(u);  // 通过才传给下游
                }
            };
        }
    };
}
```

注意：`new StatelessOp<>()` 只是创建了一个**新对象挂在链上**，`opWrapSink` 这个方法**此刻根本没有被调用**——它要等终端操作才被调用。这就是"惰性"的源码本质。

#### 2.3 终端操作触发：反向构建 Sink 链

```java
// AbstractPipeline#evaluate（终端操作入口，简化）
final <R> R evaluate(TerminalOp<E_OUT, R> terminalOp) {
    // 关键：从最后一个 stage 开始，反向调用每个 stage 的 opWrapSink
    // 把"终端 sink"从尾到头逐层包裹，形成一条 Sink 链
    return isParallel()
        ? terminalOp.evaluateParallel(this, sourceSpliterator(terminalOp.getOpFlags()))
        : terminalOp.evaluateSequential(this, sourceSpliterator(terminalOp.getOpFlags()));
}
```

**反向包装的过程**（顺序流）：

```text
终端操作：toList() → 创建 terminalSink（收集元素到 List）
  ↑ sortedStage.opWrapSink(flags, terminalSink)      → 包一层：缓冲排序后再交给 terminalSink
  ↑ mapStage.opWrapSink(flags, sortedSink)           → 包一层：map 后交给 sortedSink
  ↑ filterStage.opWrapSink(flags, mapSink)           → 包一层：过滤后交给 mapSink
最终：filterSink → mapSink → sortedSink → terminalSink  ← 一条完整的 Sink 链
```

**执行时**：遍历数据源，**每个元素**从链头（filterSink）进，依次走完整条链，最后进 terminalSink：

```java
for (元素 : 数据源) {
    filterSink.accept(元素);   // 每个元素一次循环走完整条链 —— 这就是"融合"
}
```

> [!important] 融合（fusion）的本质
> **相邻的中间操作不会"分轮"执行**——不存在"先 filter 完所有元素、再 map 所有元素"。元素是**逐个**走完 `filter → map → sorted → collect` 的整条链。这正是 `limit(5)` 能"只处理前 5 个就停"的原因，也是惰性的价值所在。

#### 2.4 短路为什么能停：Sink 的返回值

`findFirst` 的 Sink 是这样设计的：

```java
// findFirst 的 terminalSink（简化）
boolean found = false;
Object value;
public boolean cancellationRequested() { return found; }   // 关键：找到就"请求取消"
public void accept(Object e) { found = true; value = e; }
```

遍历循环每一轮都检查：

```java
// AbstractPipeline 遍历循环（简化）
do {
    if (sink.cancellationRequested()) break;   // 短路 Sink 返回 true → 立即停
    sink.accept(下一个元素);
} while (还有元素);
```

`findFirst/anyMatch/limit` 这类短路操作的 Sink 在满足条件后返回 `cancellationRequested() == true`，**上游遍历立即终止**——这就是"找到第一个就停"的机制。

#### 2.5 stateful 操作为什么打断融合

`sorted` / `distinct` 的 Sink 必须**看到全部元素**才能工作（排序要全量、去重要见全集）：

```java
// sorted 的 opWrapSink（简化）
return new Sink.ChainedReference<>(downstream) {
    List<Object> buffer = new ArrayList<>();
    @Override public void accept(Object e) { buffer.add(e); }   // 先全部收进缓冲区
    @Override public void end() {                               // 全部接收完后
        buffer.sort(comparator);
        buffer.forEach(downstream::accept);                     // 再一次性交给下游
    }
};
```

所以 `filter → map → sorted` 的融合被 sorted **打断**：filter/map 融合为一轮，sorted 缓冲全量，再统一往下游发。这也解释了"有 stateful 操作的流无法真正流式"。

#### 2.6 并行流的真相

- `parallelStream()` 默认使用 `ForkJoinPool.commonPool`（线程数 = CPU 核数 - 1），数据通过 `Spliterator.trySplit()` 拆成多段，**每段各建一条独立的 Sink 链**并行执行，最后合并结果（归约/收集器有合并协议）。
- **并行 ≠ 更快**：拆分、合并、线程切换都有成本。小数据量、简单操作、有状态操作（sorted）往往串行更快。
- **大忌**：并行流里做阻塞 IO、访问共享可变状态、依赖线程局部变量、包事务——commonPool 是全局共享的，一个任务卡住会拖累同 JVM 所有并行流。

## 💻 代码实践 / 排查命令

```java
// 分组计数：最常用的 collect
Map<String, Long> byDept = users.stream()
    .filter(User::active)                 // Predicate 方法引用
    .collect(Collectors.groupingBy(       // 按部门分组
        User::department,
        Collectors.counting()));          // 每组的数量

// 短路：找到第一个管理员就停
Optional<User> firstAdmin = users.stream()
    .filter(u -> u.roles().contains("admin"))
    .findFirst();

// 归约：求和、最大、拼接
int total = orders.stream().mapToInt(Order::amount).sum();
String names = users.stream().map(User::name).collect(Collectors.joining(", "));

// 注意：复杂收集器拆成命名方法，别写一行谜题
Map<String, Map<String, Long>> stats = users.stream()
    .collect(Collectors.groupingBy(User::department,
        Collectors.groupingBy(User::city, Collectors.counting())));
// ↑ 超过一层分组建议提取为独立方法
```

## 🛠️ 排查 / 实践要点

- **性能对比要实测**：for 循环、stream、parallelStream 三选一，小数据量下 for 和 stream 差别很小，parallel 常更慢；拿数据量说话，不凭感觉。
- **看线程名**：并行流压测时若 `ForkJoinPool.commonPool-worker-*` 卡着 RPC/锁/DB，其他用 commonPool 的模块全被拖慢。
- **副作用排查**：Stream 里修改外部 List/Map（`forEach` 里 put）在多线程下必出问题；用 `collect` 收敛，不用 `forEach` 堆积。
- **`toList()` 不可变**：JDK 16+ 的 `stream().toList()` 返回不可变列表（`add` 抛异常），需要可变集合用 `.collect(Collectors.toList())`。

## ⚠️ 踩坑与注意事项

- 在 stream 中修改外部集合（`forEach` 里 `list.add/remove` 或 map.put）。
- 复用同一个 Stream → `IllegalStateException: stream has already been operated upon or closed`。
- 把 `parallelStream` 用在 IO 阻塞或有事务上下文的业务链路上。
- 用 `stream().toList()` 后试图 `add`，抛 `UnsupportedOperationException`。
- 大集合无限流（`Stream.iterate(0, i -> i+1)` 不配 `limit` 直接终端操作）→ 永不终止。

## 🎯 面试问题

> [!question] 高频问题
> - Q: Lambda 底层是匿名内部类吗？
>   - A: 不是。现代 javac 用 `invokedynamic` + `LambdaMetafactory`：运行期生成函数对象并缓存 CallSite。无独立类文件、无捕获时单例复用、引导后直接内联调用（接近手写性能）。匿名内部类是编译期 class 文件 + 每次 new 实例。
> - Q: invokedynamic 机制具体是什么？
>   - A: javac 把 Lambda 编译成一条 `invokedynamic` 指令 + 引导方法引用；第一次执行时调用 `LambdaMetafactory.metafactory`（用 ASM 运行期生成实现类），之后复用 CallSite 直接调用。捕获变量作为字段拷入生成的实例。
> - Q: Stream 为什么是惰性的？中间操作到底做了什么？
>   - A: 中间操作只是在 `AbstractPipeline` 链表上追加一个新 stage（`filter` 就是 `new StatelessOp<>(this, ...)` 挂到 previousStage 上），完全不碰数据。终端操作才触发执行。
> - Q: 终端操作触发时发生了什么？
>   - A: 从最后一个 stage 反向调用 `opWrapSink`，把 terminalSink 从尾到头逐层包装成一条 Sink 链；然后遍历数据源，每个元素从链头进、依次走完整条链（融合）。
> - Q: findFirst 为什么能提前停？
>   - A: 短路 Sink 的 `cancellationRequested()` 满足条件后返回 true，遍历循环每轮检查该标志，true 就立即 break，不再处理剩余元素。
> - Q: sorted 为什么打断融合？
>   - A: sorted 的 Sink 必须先把全部元素收进缓冲区（排序要全量），`end()` 时才一次性交给下游——所以 filter/map 可以融合，遇到 sorted 会断成两段。
> - Q: 为什么 Lambda 捕获的局部变量必须是 effectively final？
>   - A: Lambda 可能延迟到其他线程执行，捕获变量是拷贝而非引用；允许修改会产生"改了但外层看不见"的语义混乱，编译器直接禁止。
> - Q: parallelStream 有什么风险？
>   - A: 默认用全局共享的 commonPool，阻塞任务会拖累其他并行流；共享可变状态会出数据竞争；事务/线程局部变量语义会丢失。小数据量并行往往更慢。

## 🧭 源码入口速查

- `java.lang.invoke.LambdaMetafactory#metafactory`：Lambda 动态调用点引导入口（运行期生成实现类）。
- `java.util.stream.AbstractPipeline`：保存 source、stage、flags、上下游链表。
- `ReferencePipeline#filter/map`：创建新 stage（`StatelessOp`），不立即遍历。
- `AbstractPipeline#evaluate`：终端操作入口，反向 `opWrapSink` 组装 Sink 链。
- `Sink.ChainedReference`：Sink 链节点，`accept` 处理后调 `downstream.accept`。
- `java.util.stream.TerminalOp` / `ReduceOps` / `ForEachOps`：终端操作触发执行。
- `java.util.Spliterator#trySplit`：并行拆分任务的关键接口。
- `java.util.stream.Collectors#groupingBy`：多级分组与下游收集器组合。

## 🧪 最小验证

```java
// 1. 惰性 + 短路：只处理必要元素
Stream.of(1, 2, 3, 4, 5, 6)
    .peek(i -> System.out.println("visit " + i))
    .filter(i -> i > 2)
    .findFirst();   // 只打印 visit 1~3，找到 3 就停

// 2. effectively final
int threshold = 10;         // 不重新赋值 → 可捕获
list.removeIf(x -> x > threshold);
// threshold = 20;          // 若解除注释 → 编译错误

// 3. 并行流线程观察
IntStream.range(0, 8).parallel()
    .forEach(i -> System.out.println(Thread.currentThread().getName() + " " + i));
// 输出会混入 ForkJoinPool.commonPool-worker-N

// 4. 用 javap -c 看 Lambda 字节码
// javap -c -p Main.class
// 能看到 invokedynamic 指令 + LambdaMetafactory 引导方法引用
```

## ✅ 精通检查清单

- 能说出 Lambda 的编译产物（invokedynamic + 运行期生成），并对比匿名内部类。
- 能画出 Pipeline 链表（Head → filter → map → sorted）和反向构建 Sink 链的过程。
- 能解释"融合"：每个元素一次循环走完整条 Sink 链，不是分轮执行。
- 能解释短路机制（cancellationRequested）和 stateful 操作打断融合的原因。
- 能回答本页列出的高频面试题，并补充项目中的真实取舍。

## 🔗 关联笔记

- 本课内：上一课 [[20-Java后端/01-Java核心/04-集合框架/07-排序详解|排序详解]]（Stream.sorted 与 Comparator）；下一课 [[11-IO与NIO]]
- 延伸：[[20-Java后端/03-并发编程/09-CompletableFuture异步编程|CompletableFuture 异步编程]]（并行任务模型，并发章节）

## 📚 参考资料

- [Dev.java Tutorials](https://dev.java/learn/)
- [Oracle Java Documentation](https://docs.oracle.com/en/java/)
- [JLS 15.27 - Lambda Expressions](https://docs.oracle.com/javase/specs/jls/se21/html/jls-15.html#jls-15.27)
- [JEP 335 / JEP 314：Stream 优化与 Spliterator](https://openjdk.org/jeps/335)
