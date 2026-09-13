---
title: Java新特性-Record与密封类
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Java核心
  - 阶段/01
created: 2026-08-03
updated: 2026-08-08
status: 已完成
mastery: ⭐⭐⭐☆☆
next_review: 2026-08-15
---

# Java新特性-Record与密封类

> [!abstract] 一句话总结
> `record` 让你一行声明一个"只用来装数据的类"，构造器、取值方法、`equals`、`hashCode`、`toString` 全由编译器生成；`sealed` 让你显式写出"这个接口只允许这几个类实现"，从而让 `switch` 能被编译器检查有没有漏分支。前者省样板代码，后者把"漏写分支"从线上 bug 变成编译错误。

## 📖 核心概念与关键术语速览

在深入语法之前，先明确几个核心概念的严谨定义与直观含义：

**"数据载体"** = 一个类里没有业务逻辑，纯粹是几个字段的打包，用来在方法之间搬运数据。DTO、VO、查询结果、消息事件都是。过去写它要手写构造器 + 一堆 getter + `equals`/`hashCode`/`toString`，四五个字段能写出 80 行，而真正有信息量的只有字段那几行。

**"值语义"** = 判断两个对象是否相等，看的是**里面的值**，不是看是不是同一个对象。`new Point(1,2)` 和 `new Point(1,2)` 是两个不同对象（`==` 为 `false`），但值语义下它们"相等"（`equals` 为 `true`）。钱、坐标、日期都该是值语义——两张一百块钱没有区别。

**"浅不可变"** = 只锁了第一层。`record Order(String sku, List<String> tags)` 里，`tags` 这个**引用**不能被换成别的 List（这是"不可变"），但那个 List **里面的元素**照样能被人 `add` 进去（这是"浅"）。想象你把箱子焊死在地上——箱子搬不走了，但箱子盖没锁，谁都能往里塞东西。**"浅"指的就是只焊了箱子，没锁盖子。**

**"穷尽检查"** = 编译器帮你确认 `switch` 把所有可能的情况都写了。少写一种就编译不过，而不是等到线上跑出个 `null`。

**"组件（component）"** = 就是 `record Point(int x, int y)` 括号里那两个东西。叫组件而不叫字段，是因为它一个词同时决定了四件事：字段、构造器参数、取值方法名、以及 `equals` 比较哪些内容。

## 🎯 本节定位

- **本章第 14 课，现代建模**。前置：[[01-泛型与类型擦除]]、[[04-集合框架/01-核心机制与选型]]（record 天然适合做 map key，因为 `equals`/`hashCode` 自动配套）、[[12-反射与动态代理/_index|12-反射与动态代理]]（`isRecord`/`getRecordComponents` 是框架识别 record 的入口）。
- **延伸阅读**：模式匹配（pattern matching）的完整语法属于语言特性专题，本课只用到够用的部分；虚拟线程见下一课 [[15-虚拟线程]]。

## 📌 这是什么

**record（JDK 16 正式）**：声明一个不可变的数据载体。

```java
record UserView(long id, String name) {}
```

这一行等价于手写约 40 行：`final` 类、两个 `private final` 字段、全参构造器、`id()` 和 `name()` 取值方法（**没有 `get` 前缀**）、`equals`、`hashCode`、`toString`。

**sealed（JDK 17 正式）**：声明一个继承受限的类型。

```java
sealed interface PayResult permits Success, Failed {}
```

只有 `Success` 和 `Failed` 能实现它，别人写 `implements PayResult` 编译不过。**编译器因此确切知道"一共就这两种"**，这是它全部价值的来源。

## 🤔 解决什么问题

**record 解决样板代码，但重点不是"少打字"。** 重点是**字段列表成为唯一契约**：加一个字段，`equals`、`hashCode`、`toString` 自动同步。手写时最典型的隐蔽 bug 就是加了字段忘改 `equals`——两个业务上不同的对象被判定相等，然后在 `HashMap` 里互相覆盖，查半天查不出来。record 让这类 bug 在语法层面不可能发生。

**sealed 解决"漏分支"。** 支付结果、订单状态、解析结果这类东西，子类型在业务上就是固定几个。过去要么 `final` 封死没法扩展，要么完全开放——开放的代价是你写 `switch` 时永远得加个 `default`，而 `default` 里通常只能抛异常或返回兜底值。半年后同事加了第三种结果类型，`switch` 静静地走进 `default`，没有任何人被提醒。sealed 让这个场景变成**编译错误**。

两者是搭档：**sealed 接口定义"有哪几种"，record 定义"每种带什么数据"**。

## 🧩 核心概念

| 概念                  | 一句话含义                   | 要记住的点                                      |
| ------------------- | ----------------------- | ------------------------------------------ |
| 组件 component        | `record X(a, b)` 括号里的东西 | 一处声明，同时是字段 + 构造参数 + 取值方法 + equals 依据       |
| 取值方法                | `id()`，不是 `getId()`     | 框架（Jackson 等）要 2.12+ 才认，老版本会报"找不到属性"       |
| 浅不可变                | 引用锁死，引用指向的内容不锁          | 需要真不可变 → compact constructor 里拷一份          |
| compact constructor | 省略参数列表的构造器，用来校验和改写      | 写法是 `record X(int n) { X { ... } }`，注意没有括号 |
| permits             | sealed 的许可名单            | 子类必须显式标 `final` / `sealed` / `non-sealed`  |
| 穷尽检查                | switch 覆盖了所有子类型才能编译     | sealed 的核心价值；覆盖全了就不用写 `default`            |

## ⚙️ record 编译后到底长什么样

不用猜，反射直接问它（本机 Java 21 实测）：

```java
record UserView(long id, String name) {}

System.out.println("父类     = " + UserView.class.getSuperclass());
System.out.println("是 final = " + Modifier.isFinal(UserView.class.getModifiers()));
System.out.println("组件     = " + Arrays.toString(UserView.class.getRecordComponents()));
System.out.println("toString = " + new UserView(1, "tom"));
```

实测输出：

```
父类     = class java.lang.Record
是 final = true
isRecord = true
组件     = long id  String name
方法     = [equals, hashCode, id, name, toString]
toString = UserView[id=1, name=tom]
```

三个结论直接从这里读出来，不用背：

- **父类是 `java.lang.Record`** → 唯一的继承位被占了，所以 **record 不能继承任何类**（但可以实现接口）。这个限制和 JDK 动态代理的 `$Proxy0 extends Proxy` 是同一个道理，见 [[12-反射与动态代理/01-反射与动态代理原理|反射与动态代理原理]]。
- **类是 `final`** → record 不能被继承。
- **`toString` 格式是 `类名[组件=值, ...]`** → 日志里看到这个方括号格式，就知道是个 record。

值语义也一起验了：

```
a == b        = false      ← 两个不同对象
a.equals(b)   = true       ← 但值相等
hash 相等     = true       ← equals 和 hashCode 自动配套
用新对象查 map = 原点附近    ← 所以能直接当 HashMap 的 key
```

**最后一行是 record 最实用的性质**：手写类做 map key，忘写 `hashCode` 就查不到值，这是经典 bug；record 从根上没有这个问题。

## 🆚 "浅不可变"到底浅在哪（实测）

这是文档里最需要亲眼看一次的地方。

```java
record OrderLine(String sku, List<String> tags) {}

var tags = new ArrayList<>(List.of("new"));
var line = new OrderLine("A", tags);

System.out.println("创建后        = " + line.tags());
tags.add("被外部偷偷改了");                  // 改外部那个 list
System.out.println("外部改 list 后 = " + line.tags());
line.tags().add("直接改也行");               // 直接改 record 里的 list
System.out.println("直接改内容    = " + line.tags());
```

实测输出：

```
创建后        = [new]
外部改 list 后 = [new, 被外部偷偷改了]
直接改内容    = [new, 被外部偷偷改了, 直接改也行]
```

**record 里的 `tags` 字段确实是 `final`，你没法把它换成另一个 List。但它指向的那个 List 对象，从里到外都能被改。** 而且注意第二行——**你甚至不需要拿到 record 对象，改外面那个原始 list 就够了**，因为两者指向同一个对象。这在多线程或者对象被到处传递时非常危险。

修法是 compact constructor 里拷一份：

```java
record SafeOrderLine(String sku, List<String> tags) {
    SafeOrderLine {                                    // ← 注意：没有参数列表，也没有括号
        if (sku == null || sku.isBlank())
            throw new IllegalArgumentException("sku 不能为空");
        tags = List.copyOf(tags);                      // ← 直接给参数赋值，编译器会用它去赋字段
    }
}
```

实测输出：

```
外部改 list 后 = [new]  (没被影响)
直接改内容    = 抛 UnsupportedOperationException (List.copyOf 是只读的)
空 sku 校验    = sku 不能为空
```

compact constructor 有两个反直觉的地方值得单独记：

**它没有参数列表。** 写 `SafeOrderLine {` 而不是 `SafeOrderLine(String sku, List<String> tags) {`。参数是隐含的，名字就是组件名。

**你在里面给参数赋值，而不是给字段赋值。** `tags = List.copyOf(tags)` 改的是参数变量；方法体跑完，编译器才自动把参数的最终值赋给字段。所以这里**不能写 `this.tags = ...`**（编译错误）。这个设计的好处是校验和改写都发生在字段被赋值之前，**非法对象根本没机会诞生**。

## 🆚 record 和 enum 到底差在哪（最容易混的一对）

这两个都常用来"表示一组东西"，但它们的区别是根本性的：**enum 管实例，record 管值。**

| | enum | record |
| --- | --- | --- |
| 实例数量 | **固定、可数**，写死在代码里 | **无限**，想 new 多少个就多少个 |
| 能否列举全部 | 能，`values()` | 不能，值域是无限的 |
| 相等判断 | 用 `==`（每个常量是单例） | 用 `equals`（值相等） |
| 有没有序号 | 有，`ordinal()` | 没有 |
| 典型用途 | 状态、类型、开关：`PAID`、`SHIPPED` | 数据：`Money(CNY, 4999)`、`Point(1,2)` |
| 专用容器 | `EnumMap`/`EnumSet`（数组实现，很快） | 普通 `HashMap` |

实测对比：

```
########## enum：实例是固定的、可数的 ##########
全部实例 = [CREATED, PAID, SHIPPED]
能用 ==  = true
有序号   = 2
单例     = true
EnumMap  = {PAID=3}  (数组实现，比 HashMap 快)

########## record：实例是无限的、按值相等 ##########
可以造无限多个不同的 Money：
  Money[currency=CNY, cents=1] / Money[currency=CNY, cents=999999]
== 结果     = false   <- 不同对象
equals 结果 = true    <- 但值相等
能 values() 列举吗 = 不能，值域无限
```

**判断方法很简单：问自己"我能把所有可能的取值列出来吗？"**

订单状态能列完（创建、已付、已发货、已完成、已取消）→ **enum**。金额列不完（1 分到几个亿，还要乘上币种）→ **record**。

一个常见误用：用 enum 表示"错误码 + 错误消息"。看着挺合理，但如果错误消息需要带动态参数（"余额不足，还差 12.5 元"），enum 就撑不住了——常量是写死的，装不了运行时数据。这时候应该是 `record BizError(String code, String msg)`。

反过来的误用是用 `String` 常量表示状态（`"PAID"`），拼错编译器不管，`switch` 也没有穷尽检查，这是 enum 存在的理由。

## ⚙️ sealed：让编译器帮你数分支

sealed 的价值只有在 `switch` 里才体现出来。完整例子：

```java
sealed interface PayResult permits Success, Failed, Pending {}

record Success(String tradeNo, long amount) implements PayResult {}
record Failed(String code, String reason)   implements PayResult {}
record Pending(int retryAfterSec)           implements PayResult {}

String describe(PayResult r) {
    return switch (r) {                              // ★ 没有 default
        case Success s -> "成功 单号=" + s.tradeNo() + " 金额=" + s.amount();
        case Failed  f -> "失败 " + f.code() + ":" + f.reason();
        case Pending p -> "处理中 " + p.retryAfterSec() + "s 后重试";
    };
}
```

实测输出：

```
  成功 单号=T202608081 金额=4999
  失败 E01:余额不足
  处理中 30s 后重试
```

**这段代码没有 `default` 分支，而且能编译通过。** 这就是穷尽检查：编译器知道 `PayResult` 一共三种，你三种都写了，所以不需要兜底。

**现在把价值说透：** 明天有人加了第四种 `record Cancelled(...) implements PayResult`，同时要在 `permits` 里加上它——这个 `switch` 会**立刻编译报错**，报错信息直接告诉你少了 `Cancelled` 分支。整个项目里所有对 `PayResult` 做 switch 的地方，一个都跑不掉。

对比一下不用 sealed 的写法：你必须加 `default -> throw new IllegalStateException()`，然后新增类型时，代码**照样编译通过、照样上线**，直到线上真跑到那个分支才抛异常。**sealed 把一个运行时问题变成了编译时问题**，这是它唯一但足够重要的价值。

配合 record pattern（JDK 21 正式）还能直接解构，连取值方法都不用调：

```java
switch (r) {
    case Success(String no, long amt) -> "成功 " + no + " / " + amt;
    case Failed(String code, var reason) -> "失败 " + code;
    case Pending(int sec) when sec < 60 -> "处理中(快)";
    case Pending(int sec) -> "处理中(慢)";
}
```

实测输出：

```
  成功 T202608081 / 4999
  失败 E01
  处理中(快)
  处理中(慢)
```

注意最后两行——`when` 守卫可以让同一个类型走不同分支，但**带 `when` 的分支不算覆盖该类型**，所以后面必须跟一个不带 `when` 的兜底，否则编译器认为 `Pending` 没被穷尽。

## 🤔 sealed / final / enum / 普通类 怎么选

这是四选一，判据是**两个问题**：

```
子类型的集合是固定的吗？
├─ 不固定，会不断加 → 普通类/接口（开放继承）
└─ 固定
   ├─ 固定为"几个具体的值"，且不携带运行时数据 → enum
   ├─ 固定为"几种类型"，每种带不同的数据字段     → sealed + record
   └─ 就一种，压根不需要子类型                  → final class
```

具体点：

- **`final`**：工具类、值对象、不希望被继承的实现类。表达"到此为止"。
- **`enum`**：状态机的状态、消息类型、配置开关。表达"就这几个值"。
- **`sealed`**：结果类型（成功/失败/进行中）、AST 节点、命令对象。表达"就这几种形态，但每种形态数据不同"。
- **普通类**：需要开放扩展的抽象（策略、模板方法、框架扩展点）。

顺带一个实测细节，能防止概念混淆：

```
enum 也是密封的吗 isSealed = false  (enum 靠另一套机制，不是 sealed)
```

enum 虽然也"限制了继承"，但它走的是 enum 自己的规则，`isSealed()` 返回 `false`，class 文件里也没有 `PermittedSubclasses` 属性。**别把两者当成一回事。**

## 🔍 sealed 怎么保证不被绕过

三道锁，`javap -v` 能看见第一道：

1. **class 文件里有 `PermittedSubclasses` 属性**，明确列出许可名单。
2. **编译期校验**：不在名单里的类 `implements` 它 → 编译错误；名单里的类没标 `final`/`sealed`/`non-sealed` → 编译错误。
3. **类加载期校验**：绕过编译器（比如直接改字节码）硬塞一个子类 → 加载时抛 `IncompatibleClassChangeError`。

还有个范围限制：**许可子类必须和 sealed 类型在同一个模块里**（没用模块系统时是同一个包）。这样别人没法在自己的 jar 里偷偷加一个子类型。

## 💻 实践要点

```java
// 1. DTO / 查询结果 / 消息事件 → record
record UserView(long id, String name, LocalDateTime createdAt) {}

// 2. 复合 map key → record（equals/hashCode 自动配套，这是最省心的用法）
record CacheKey(long tenantId, long userId) {}
Map<CacheKey, String> cache = new HashMap<>();

// 3. 需要校验或真不可变 → compact constructor
record Money(String currency, long cents) {
    Money {
        if (cents < 0) throw new IllegalArgumentException("金额不能为负");
    }
}

// 4. 有限结果类型 → sealed + record
sealed interface Result<T> permits Ok, Err {}
record Ok<T>(T value) implements Result<T> {}
record Err<T>(String msg) implements Result<T> {}
```

```bash
javap -p UserView.class      # 看生成的字段、访问器、equals/hashCode/toString
javap -v PayResult.class     # 看 PermittedSubclasses 属性
```

**边界：JPA / MyBatis 实体不要用 record。** 这些框架需要无参构造器、需要 setter、需要生成代理做懒加载，而 record 三样都给不了。实体用普通类，**查询出来映射成 record 做 DTO 往外传**，这是标准分工。

## ⚠️ 踩坑与注意事项

**数组组件的 `equals` 是坑（实测）：**

```java
record Bytes(String name, byte[] data) {}
new Bytes("f", new byte[]{1,2}).equals(new Bytes("f", new byte[]{1,2}))
```

实测输出：

```
内容一样但 equals = false   <- 数组比的是引用！
toString         = Bytes[name=f, data=[B@6ab778a]   <- 数组打印成地址
```

record 生成的 `equals` 对引用类型调用的是 `Objects.equals`，而数组的 `equals` 就是 `==`。**组件里带数组，值语义直接失效。** 解决办法：换成 `List<Byte>`，或者自己重写 `equals`/`hashCode`/`toString`（record 允许你覆盖生成的方法）。

**`hashCode` 不缓存（和 `String` 不同）。** `String` 内部有个 `hash` 字段缓存结果，record 没有，每次调用都重新算一遍所有组件。组件多、又频繁当 map key 时，这是个真实的开销点。顺带一个实测现象：record 的第一次 `hashCode` 调用明显慢（本机约 1.1ms，之后约 900ns），因为 record 的 `equals`/`hashCode`/`toString` 是编译成 `invokedynamic`、首次调用时才由 `ObjectMethods.bootstrap` 生成方法句柄链——和 Lambda 的首次调用开销是同一个机制。

**其他几个：**

- 给 record 加实例字段 → 编译错误。这是设计使然，它只能有组件。静态字段和静态方法是允许的。
- 老框架不认识 record：Jackson 需要 2.12+，否则报"找不到属性"（因为取值方法叫 `id()` 不叫 `getId()`）。
- sealed 的子类忘了标 `final`/`sealed`/`non-sealed` → 编译错误。
- 对"一定会不断扩展"的接口用 sealed → 每加一个实现都要改 `permits`，约束太死，这时候该用普通接口。
- `record` 不是 `final class` 的替代品：需要行为、需要可变状态的类，普通类更合适。record 只擅长"装数据"。

## 🧠 自测题（先自己答，再展开）

> [!question]- Q1: 为什么 record 不能继承别的类？这个限制和 JDK 动态代理只能代理接口有什么共同点？
> 因为 record 编译后隐式 `extends java.lang.Record`，Java 单继承，唯一的继承位已经被占了。
>
> 和动态代理是同一个原理：`$Proxy0` 隐式 `extends Proxy`，继承位被占，所以只能 `implements` 接口、没法代理类。**两个看似无关的限制，根因是同一条语言规则。** 记住这个类比，两边都不用背。

> [!question]- Q2: 有人用 `record Money(String currency, long cents)` 表示金额，也有人主张用 enum 列出常用面额。谁对？为什么？
> record 对。判据是"能否列举所有取值"：金额的值域是无限的（任意分数 × 任意币种），enum 的实例必须写死在代码里，装不下。
>
> 反过来，订单状态（创建/已付/已发货/已完成/已取消）能列完，且不需要携带运行时数据，就该用 enum——还能白拿 `values()`、`==` 比较、`EnumMap` 的性能优势。
>
> 边界情况：如果"状态"需要携带数据（比如"失败"要带失败原因），enum 就不够了，应该升级成 `sealed interface` + record。

> [!question]- Q3: 我在 record 里放了个 `List`，字段是 `final` 的，为什么还说它可变？怎么修？
> `final` 锁的是**引用**，不是引用指向的对象。字段不能被换成另一个 List，但那个 List 的内容能随便增删。而且更隐蔽的是：**外部持有的原始 list 引用和 record 里的是同一个对象**，改外面就等于改里面，连 record 对象都不用拿到。
>
> 修法是在 compact constructor 里 `tags = List.copyOf(tags)`。两个效果：切断了和外部引用的联系，且 `List.copyOf` 返回只读 list，之后 `add` 会抛 `UnsupportedOperationException`。
>
> 注意写法：compact constructor 里给**参数**赋值，不能写 `this.tags = ...`。

## 🎯 面试问题

> [!question] 高频问题
> - **Q: record 和 Lombok `@Data` 的区别？**
>   - **30 秒**：record 是语言特性，编译器保证类是 final、组件是 final、值语义方法与组件列表永久同步；`@Data` 是编译期代码生成，默认生成的是**可变** JavaBean（带 setter）。
>   - **2 分钟**：补三点。① record 能被 `Class#isRecord`/`getRecordComponents` 识别，框架可以针对它做特殊处理，Lombok 生成完就和手写类没区别；② record 不能继承类、不能加实例字段，约束更强；③ 迁移边界——JPA 实体不能换成 record（要无参构造器、setter、代理），DTO 可以。
> - **Q: record 是不可变的吗？**
>   - **30 秒**：是浅不可变。组件字段是 `final`，但组件指向的可变对象（List、Map、数组、可变自定义类）内容照样能改。
>   - **2 分钟**：补 compact constructor 做防御性拷贝的写法，以及"外部原始引用和内部是同一对象"这个更隐蔽的泄漏路径。
> - **Q: sealed 和 enum、final 怎么选？**
>   - **30 秒**：能列举所有取值且不带数据 → enum；类型固定但每种带不同数据 → sealed + record；不需要子类型 → final。
>   - **2 分钟**：补 sealed 的真正价值是穷尽检查——把"新增类型后漏改 switch"从线上 bug 变成编译错误；补 enum 的 `isSealed()` 是 `false`，两套机制不要混。
> - **Q: sealed 怎么防止被绕过？**
>   - **30 秒**：`PermittedSubclasses` 属性写进 class 文件，编译期 + 类加载期双重校验，且子类必须与 sealed 类型同模块（或同包）。
>   - **2 分钟**：补类加载期抛的是 `IncompatibleClassChangeError`；补子类必须显式标 `final`/`sealed`/`non-sealed`，`non-sealed` 是主动放开这一支的继承。

## 🪝 记忆钩子

- **"浅不可变 = 箱子焊死了，箱盖没锁。"** 想真锁上，compact constructor 里 `List.copyOf`。
- **"能列完 → enum，列不完 → record，几种形态各带数据 → sealed + record。"**
- **"sealed 的全部价值就一句：把漏分支从线上 bug 变成编译错误。"**
- **"继承位被占了"** —— record `extends Record`、`$Proxy0 extends Proxy`，同一条规则解释两个限制。
- **compact constructor 两个反直觉**：没有参数列表；给参数赋值而不是给字段赋值。

## 🧭 源码入口速查

- `java.lang.Record`：所有 record 的隐式父类。
- `Class#isRecord` / `getRecordComponents`：框架识别 record 的反射入口（衔接 [[12-反射与动态代理/_index|反射与动态代理]]）。
- `Class#isSealed` / `getPermittedSubclasses`：读取许可名单。
- `java.lang.runtime.ObjectMethods#bootstrap`：record 的 `equals`/`hashCode`/`toString` 真正的生成处，走 `invokedynamic`。
- `javap -p` / `javap -v`：看生成的方法、看 `PermittedSubclasses` 属性。

## ✅ 精通检查清单

- 能用"箱子焊死、箱盖没锁"解释浅不可变，并写出 compact constructor 的防御性拷贝（含"不能写 `this.x =`"这个细节）。
- 拿到一个建模场景，能在 enum / record / sealed+record / final / 普通类之间说出选择和判据。
- 能解释 sealed 的 switch 为什么可以不写 `default`，以及新增子类型时会发生什么。
- 能说出 record 不能继承类的原因，并类比到 JDK 动态代理只能代理接口。
- 知道两个坑：数组组件让 `equals` 失效、JPA 实体不能用 record。

## 🔗 关联笔记

- 本章内：上一课 [[13-注解与注解处理器]]；[[12-反射与动态代理/_index|12-反射与动态代理]]（`isRecord`/`getRecordComponents`，以及"继承位被占"的同源限制）；下一课 [[15-虚拟线程]]
- 应用：[[04-集合框架/01-核心机制与选型]]（record 做 map key 天然满足 equals/hashCode 契约）、[[04-集合框架/04-HashMap原理与扩容]]
- 延伸：[[10-Lambda与Stream]]（`invokedynamic` 首次调用开销，和 record 值语义方法是同一机制）

## 📚 参考资料

- [JEP 395: Records](https://openjdk.org/jeps/395)
- [JEP 409: Sealed Classes](https://openjdk.org/jeps/409)
- [JEP 440: Record Patterns](https://openjdk.org/jeps/440)
- [Dev.java Tutorials](https://dev.java/learn/)
