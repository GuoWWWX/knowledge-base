---
title: Optional与空值处理
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Java核心
  - 阶段/01
created: 2026-11-01
updated: 2026-11-03
status: 整理中
mastery: ⭐☆☆☆☆
---

# Optional与空值处理

> [!abstract] 一句话总结
> `Optional<T>` 表示“这里可能有一个 T，也可能没有”。通常由查询方法返回，让调用方明确处理查不到的情况：用默认值、跳过操作，或者抛出异常。

## 🎯 本节定位与阅读顺序

- 先读第 1～6 节：从普通 null 写法走到 Optional，弄懂最常用的几种处理方式，然后运行完整示例。
- 第 7 节是进阶选读：orElseGet、filter、flatMap 和 Stream，入门时不必一次记住。
- 前置只需要会类、对象、方法返回值和简单泛型。`Optional<User>` 的尖括号表示“里面如果有值，它的类型是 User”，可对照 [[01-泛型与类型擦除]]。
- Optional 自 JDK 8 提供；本篇两个完整程序按 JDK 21 验证。后文 `stream()` 为 JDK 9+、`isEmpty()` 为 JDK 11+、Stream.toList() 为 JDK 16+。

## 1. 🤔 先看问题：查不到用户时怎么办

假设我们在写一个用户查询方法。下面的 User 是自己的业务类，提供 getName()；findUserOrNull 也是自己写的方法，并不是 JDK 自带的查询功能。完整定义在第 6 节。

```java
User user = findUserOrNull(2); // 假设只有编号 1 存在，查询 2 返回 null
String name = user.getName();  // user 为 null，会抛 NullPointerException
```

普通写法完全可以处理，关键是调用方要记得判断：

```java
User user = findUserOrNull(2);
String name;
if (user != null) {
    name = user.getName();
} else {
    name = "游客";
}
```

问题在于，方法签名只写 `User findUserOrNull(int id)`，调用方仍需要从命名、文档或实现得知它会返回 null。Optional 用返回类型直接提示：**这个查询允许没有结果，使用结果时请明确选择处理方式。**

原来的 if 写法仍然有效；业务分支复杂时，继续写 if 就很清楚。学习 Optional 的目的，是读懂并表达“可能没有结果”的返回值，而不是把所有判断都改成链式调用。

## 2. 📦 Optional 到底是什么

把它理解成一个最多装一个值的容器：

| 返回结果 | 意思 |
| --- | --- |
| `User` 对象 | 这是用户本人 |
| `Optional<User>`，有值 | 容器里有一个用户 |
| `Optional<User>`，为空 | 容器存在，但没有查到用户 |

下面用字符串看这两种状态，代码可放进 main，需导入 `java.util.Optional`：

```java
Optional<String> a = Optional.of("小明"); // 有值
Optional<String> b = Optional.empty();   // 没有值

System.out.println(a.isPresent()); // true
System.out.println(b.isPresent()); // false
```

`Optional<User>` 和 `User` 是不同类型，所以不能直接对查询结果调用用户的方法：

```java
Optional<User> result = findUser(1);
// result.getName(); // 编译不通过，getName 是 User 的方法，不是 Optional 的方法
```

此时要做的，是通过 Optional 提供的方法处理里面的用户。它不会帮你查询数据库，也不会凭空生成缺失的用户；它只表达和处理“有值 / 无值”。

## 3. 🏭 查询方法怎么返回 Optional

下面是方法声明片段，和第 6 节使用同一个 User、findUserOrNull：

```java
static Optional<User> findUser(int id) {
    User user = findUserOrNull(id);    // 先执行真正的查询，可能返回 null
    return Optional.ofNullable(user); // 再把查询结果转换成 Optional
}
```

`ofNullable` 做的事只有两种：

```text
查到 User 对象 → Optional 里有这个 User
查到 null      → 返回空 Optional
```

所以方法返回值有了明确约定：返回有值或为空的 Optional，**不再把 Optional 本身返回成 null**。

## 4. 🧰 调用方怎么用：先学三种处理方式

以下片段都使用第 6 节定义的 User 与 findUser；放在 main 中运行，编号 1 存在，编号 2 不存在。

### 方式一：没有就用默认值——orElse

先用第 2 节的字符串容器理解：

```java
Optional<String> name = Optional.empty();
String displayName = name.orElse("游客");
System.out.println(displayName); // 游客
```

`orElse("游客")` 的意思是：**里面有字符串，就返回那个字符串；里面没有，就返回“游客”。** 返回的是 String，不再是 Optional。

查用户时，我们想要的是“用户的姓名”，所以先用 map 取姓名：

```java
String name = findUser(1)
        .map(user -> user.getName())
        .orElse("游客");
System.out.println(name); // 小明
```

这一行按顺序拆开看：

| 执行部分 | 得到的类型 | 在本例做什么 |
| --- | --- | --- |
| `findUser(1)` | Optional<User> | 查询用户，可能有，也可能没有 |
| `.map(user -> user.getName())` | Optional<String> | 有用户时取姓名；没有用户就跳过 |
| `.orElse("游客")` | String | 有姓名就取出来，没有就用“游客” |

`user -> user.getName()` 可以读成“拿到 user 后，返回它的姓名”。它是一段交给 map 执行的操作；只有 Optional 有用户时才执行，user 不会因为容器为空而变成 null 传进来。

查询 2 时是另一条路径：

```java
String name = findUser(2)
        .map(user -> user.getName()) // 没有用户，这段不会执行
        .orElse("游客");
System.out.println(name);           // 游客
```

这与第 1 节的 if/else 达到相同的展示效果。本例用户姓名固定非 null；如果用户存在但 getName() 返回 null，map 也会得到空 Optional，最后使用“游客”。

等 Lambda 看熟后，下面两种写法表达同样的取姓名操作：

```java
.map(user -> user.getName())
.map(User::getName) // 方法引用，本节先用上面更直观的写法即可
```

这两行是链式调用中的片段，不是独立的 Java 语句；Lambda 和方法引用详见 [[10-Lambda与Stream]]。

### 方式二：有就做事，没有就跳过——ifPresent

```java
findUser(1).ifPresent(user -> System.out.println(user.getName())); // 打印 小明
findUser(2).ifPresent(user -> System.out.println(user.getName())); // 不执行打印
```

`ifPresent` 适合“查到才通知 / 才处理”这种操作，返回类型是 void，不是给你返回一个姓名。它相当于“有值才执行 if 内的代码”，没有值时不做事。

### 方式三：没有就认为业务失败——orElseThrow

```java
User user = findUser(1)
        .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
System.out.println(user.getName()); // 小明
```

这里 `orElseThrow` 返回的是 User，所以后面能直接调用 getName()。如果换成 findUser(2)，就会抛指定异常。

`() -> new IllegalArgumentException(...)` 表示“需要时创建异常”：括号里没有参数，箭头右边是创建异常的操作，只有缺值时才执行。

如何选取决于业务：展示昵称查不到可以显示“游客”；必须给指定用户转账时，查不到应停止并报错。Optional 不替你决定缺值的业务含义。

## 5. 🧩 创建方式只需分清三个

| 写法 | 适用情况 | 传入 null 的结果 |
| --- | --- | --- |
| `Optional.of(value)` | 你确信值不为 null | 立即抛 NullPointerException |
| `Optional.ofNullable(value)` | 值可能是 null | 返回空 Optional |
| `Optional.empty()` | 明确表示没有结果 | 不需要传参数 |

把旧查询返回的可空结果包装起来，通常用 ofNullable。写 `Optional.of(findUserOrNull(2))` 仍会抛异常，因为 of 不接受 null。

## 6. 💻 完整实验：查得到、查不到各跑一遍

保存为 `OptionalDemo.java`，运行 `java OptionalDemo.java`。这里只模拟查询，不连接数据库；User、查询方法、调用方都在同一个程序中。

```java
import java.util.Optional;

public class OptionalDemo {
    static class User {
        private final String name;
        User(String name) { this.name = name; }
        String getName() { return name; }
    }

    static User findUserOrNull(int id) {
        if (id == 1) return new User("小明");
        return null;
    }

    static Optional<User> findUser(int id) {
        return Optional.ofNullable(findUserOrNull(id));
    }

    public static void main(String[] args) {
        // 普通 null 写法。
        User user = findUserOrNull(2);
        String oldName;
        if (user != null) oldName = user.getName();
        else oldName = "游客";
        System.out.println(oldName); // 游客

        // 有值、无值时，分别获取展示姓名。
        String name1 = findUser(1).map(u -> u.getName()).orElse("游客");
        String name2 = findUser(2).map(u -> u.getName()).orElse("游客");
        System.out.println(name1); // 小明
        System.out.println(name2); // 游客

        // 有就执行；查不到的这一行不会打印。
        findUser(1).ifPresent(u -> System.out.println("查到：" + u.getName()));
        findUser(2).ifPresent(u -> System.out.println("不应出现这一行"));

        // 查不到必须报错的业务。
        try {
            User required = findUser(2).orElseThrow(
                    () -> new IllegalArgumentException("用户不存在"));
            System.out.println(required.getName());
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage()); // 用户不存在
        }
    }
}
```

应依次输出：

```text
游客
小明
游客
查到：小明
用户不存在
```

最小验证：把 main 中的 findUser(2) 改成 findUser(1)，观察默认值、ifPresent 和异常分支如何变化。能自己预测输出，再读下一节。

## 7. 🔍 进阶选读：基础用熟后再看

### orElseGet：默认值需要计算时，缺值才执行

`orElse("游客")` 的默认值是现成字符串，用起来简单。但如果默认值来自查询或计算，要注意普通 Java 方法调用会先求出实参：

```java
// 假设 found 是 Optional<String>，loadDefaultName() 会执行一次备用查询。
String a = found.orElse(loadDefaultName());          // 不管 found 有没有值，先执行查询
String b = found.orElseGet(() -> loadDefaultName()); // 只有 found 为空才执行查询
```

orElseGet 接收一段“生成备用值的操作”，而不是已经算出的值。两种方法的返回类型都是 String，区别是执行时间。

### filter：有值还要满足条件

下面是第 6 节 main 中可加的片段：

```java
String name = findUser(1)
        .map(user -> user.getName())
        .filter(value -> !value.isBlank()) // 空白姓名也当作没有可用姓名
        .orElse("游客");
```

filter 保留满足条件的值，不满足时变成 empty；它不负责改写姓名。

### flatMap：被调用的方法已经返回 Optional 时使用

假设我们已经拿到 Optional<String> 用户名，现在要调用一个“按姓名查邮箱”的方法；邮箱也可能没有，所以该方法返回 Optional<String>。

```text
用户名容器.map(查邮箱)     → Optional<Optional<String>>，又包一层
用户名容器.flatMap(查邮箱) → Optional<String>，直接接住查邮箱的容器
```

上面是类型示意，不是可执行代码。记住区分：取普通 String 姓名用 map；调用返回 Optional 的方法用 flatMap。下面完整程序把 flatMap 和备用值求值一起演示，不需要第 6 节的 User 类。

保存为 `OptionalDetailsDemo.java`，运行 `java OptionalDetailsDemo.java`：

```java
import java.util.Optional;

public class OptionalDetailsDemo {
    static int calls;
    static String loadDefaultName() {
        calls++;
        return "游客";
    }
    static Optional<String> findEmail(String name) {
        if (name.equals("小明")) return Optional.of("xiaoming@example.com");
        return Optional.empty();
    }
    public static void main(String[] args) {
        Optional<String> found = Optional.of("小明");
        System.out.println(found.orElse(loadDefaultName())); // 小明
        System.out.println(calls);                          // 1，已经执行过备用查询
        System.out.println(found.orElseGet(() -> loadDefaultName())); // 小明
        System.out.println(calls);                          // 1，没有新增查询
        Optional<String> missing = Optional.empty();
        System.out.println(missing.orElseGet(() -> loadDefaultName())); // 游客
        System.out.println(calls);                          // 2，这次缺值才执行

        Optional<Optional<String>> nested = found.map(name -> findEmail(name));
        Optional<String> email = found.flatMap(name -> findEmail(name));
        System.out.println(nested.get().orElse("没有邮箱")); // xiaoming@example.com
        System.out.println(email.orElse("没有邮箱"));        // xiaoming@example.com
        System.out.println(Optional.of("其他用户")
                .flatMap(name -> findEmail(name)).orElse("没有邮箱")); // 没有邮箱
    }
}
```

这里 nested.get() 只用于演示“多包了一层”，并且本例已知外层有值；业务代码不要照着无条件 get。

### 和 Stream 的关系

`stream.findFirst()` 等查询可能找不到元素，因此返回 Optional。下面是可放进 main 的片段，需导入 `java.util.List` 和 `java.util.Optional`：

```java
List<String> names = List.of("小明", "小红");
Optional<String> found = names.stream().filter(name -> name.equals("小李")).findFirst();
System.out.println(found.orElse("没有这个人")); // 没有这个人
```

Optional.stream() 则把有值变成 1 个元素的流，empty 变成 0 个元素的流。原文“集合中一堆 Optional 再 flatMap(Optional::stream)”的写法，可以在 [[10-Lambda与Stream]] 学完后再看：

```java
List<Optional<String>> results = List.of(Optional.of("小明"), Optional.empty());
List<String> names = results.stream().flatMap(Optional::stream).toList(); // [小明]
```

该片段需相同 imports，toList 需要 JDK 16+；它只保留有值项，得到的列表不可修改。

## ⚠️ 使用边界与常见错误

- `result.get()`：有值才能取，empty 会抛 NoSuchElementException。优先用明确表达缺值处理的 orElse、ifPresent 或 orElseThrow。
- `Optional<User> result = null`：容器本身又为空，调用它的方法还是会 NPE。无结果应返回 Optional.empty()。
- map 不会执行空容器的转换，但也不会吞掉转换函数内部的业务异常；函数返回 null 时 map 转成 empty，flatMap 的函数返回 null 则会 NPE。
- 优先用于“查一个结果，但可能没有”的返回值。不要机械地把所有字段、入参、局部变量都包成 Optional；Optional 不实现 Serializable，也不要拿它做锁或比较引用身份。
- 集合查询通常返回空 List 即可；只有明确需要额外的“整个结果缺失”语义时，才考虑 Optional<List<T>>。
- “没有查到用户”与“数据库故障”不同。故障应正常传播或转换异常，不要 catch 所有异常后返回 empty。
- 里面装的 User 仍可能可变，Optional 不会自动把它变成深不可变对象，见 [[17-对象语义与拷贝]]。

## 🎯 自测与面试问题

> [!question]- Optional<User> 与 User 的区别是什么？返回 empty 表示什么？
> User 是用户对象；Optional<User> 是表示“可能有用户”的容器。empty 表示这次没有用户结果，但容器本身存在；查询方法是否允许查不到，直接在返回类型上可见。

> [!question]- 查不到显示“游客”，与查不到必须报错，分别怎么写？
> 展示姓名可以 `findUser(id).map(u -> u.getName()).orElse("游客")`；必须存在的用户可以 `findUser(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"))`，后者返回 User 或抛异常。

> [!question]- 为什么 map 的 Lambda 不会因为容器为空而收到 null？
> Optional 为空时直接跳过转换。有值时才把其中的非 null 用户传给 Lambda；但用户内部字段仍可能为空，需要按业务处理。

> [!question]- 已经知道结果有值，orElse 为什么仍可能执行备用查询？
> 普通方法调用先求出实参，备用查询在进入 orElse 前已经发生；orElseGet 接收延迟操作，只有缺值才执行。

> [!question]- 什么时候需要 flatMap？
> 转换函数本来返回 Optional 时。map 会再包一层，flatMap 直接衔接返回的 Optional；转换函数返回普通值时用 map。

## 🧭 源码入口速查（理解用法后再读）

- Optional 内部的 value 字段区分有值与无值；这是实现层的表达，对外使用 isPresent/isEmpty 等 API 判断。
- `Optional#ofNullable`：根据传入值是否为 null，创建有值或为空的结果。
- `Optional#map` / `ifPresent`：先判断有值，再调用你传入的操作。
- `Optional#orElse` / `orElseGet`：直接取备用值，或在缺值时调用备用操作。

## ✅ 精通检查清单

- 能说明 Optional 是谁创建、谁返回、谁处理，以及它为什么不负责真正的数据库查询。
- 能把“有用户就取姓名，查不到显示游客”分别写成 if/else 和 Optional 两种形式。
- 能说出每一步返回的是 User、Optional<User>、Optional<String> 还是 String。
- 能跑出第 6 节输出，并预测切换用户编号后哪些分支会变化。
- 基础掌握后，再区分 map/flatMap 和 orElse/orElseGet。

## 🪝 记忆钩子

“可能没有”放进返回类型；拿到后再决定：有就取，没有用默认值；有就做，没有跳过；必须有，没有就报错。

## 🔗 关联笔记

- [[01-泛型与类型擦除]]：看懂 Optional<T> 的类型参数。
- [[10-Lambda与Stream]]：理解箭头函数、方法引用和流查询。
- [[02-异常处理最佳实践]]：无结果与操作失败；[[17-对象语义与拷贝]]：对象可变性。
- 本章导航：[[_index]]。

## 📚 参考资料

- [Optional API（JDK 21）](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Optional.html)
- [OpenJDK 21 Optional 源码](https://github.com/openjdk/jdk21u/blob/master/src/java.base/share/classes/java/util/Optional.java)
