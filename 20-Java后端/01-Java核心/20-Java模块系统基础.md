---
title: Java模块系统基础
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

# Java模块系统基础

> [!abstract] 一句话总结
> Java 模块系统 JPMS 给一组包增加明确边界：声明依赖哪些模块、公开哪些包、允许哪些深反射。它让“public 类但属于内部实现”也能受到跨模块访问限制。

## 🎯 先说学习优先级与用途

**本篇是进阶选读。** 当前在补语法、集合、对象和常用 API，可以先读第 1～2 节，知道用途；不必为了继续学 Spring 而先把整个项目改成模块化。

使用 JDK 21 不意味着你的应用必须写 module-info.java；应用仍然可以按 classpath 方式构建和运行。以下情况再深入本篇更有针对性：

- 正在开发模块化 Java 库，希望明确哪些包是对外 API，哪些只是内部实现。
- 遇到 package is not visible、does not read、InaccessibleObjectException 等模块访问问题。
- 需要了解 JDK 自身的模块划分，或用 jlink 组装所需模块的运行时。

本篇实验使用 JDK 21，JPMS 从 JDK 9 引入。前置：[[06-面向对象与方法调用]]；反射部分读过 [[12-反射与动态代理/_index|反射与动态代理]] 再看。

## 1. 🤔 它解决什么问题：库里 public 的类都该暴露给别人吗

假设你写了一个用户查询库：

```text
demo.library
├─ demo.api        对外提供的查询入口
└─ demo.internal   仅供库内部不同包协作的实现类
```

内部类有时要跨包协作，因此不能全部改成包可见。但你也不希望使用方依赖这些实现细节，否则内部一调整，对方项目就可能出问题。

JPMS 可以声明：只导出 demo.api，不导出 demo.internal。这样即使内部类声明为 public，其他命名模块的普通源码也不能随意访问它。

因此需要区分两层限制：

| 检查层次 | 主要回答什么 |
| --- | --- |
| public / private / protected / 包可见 | Java 类型与成员本身是否可访问 |
| 模块可读关系与包导出 | 调用模块能否使用目标模块这个包 |

“有 public”只是满足部分条件，不自动满足模块边界。本篇主要讲正常模块规则下的访问限制；命令行兼容参数等可以显式调整这些关系。

## 2. 🧩 先理解 requires、exports、opens

| 声明 | 谁写 | 表达什么 |
| --- | --- | --- |
| requires demo.library | 使用方模块 | 我要读取、依赖这个模块 |
| exports demo.api | 提供方模块 | 这个包的可访问 API 可以供其他读取者使用 |
| opens demo.api to demo.app | 提供方模块 | 允许指定模块对该包做深反射 |

requires 相当于把“用谁”写清楚，exports 把“给别人用哪一部分”写清楚。opens 则针对反射访问，和普通源码能直接调用哪些成员不是同一件事。

这些声明通常写在各自模块根部的 module-info.java 中。下面做一个真正跨模块调用的实验，直接看到删掉声明后发生什么。

## 3. 💻 完整实验：一个模块使用另一个模块的类

在新的实验目录创建下面四个文件，无第三方依赖。模块名是 demo.library、demo.app；包名是 demo.api、demo.app，两者是不同概念，不要求相同。

```text
src/
├─ demo.library/
│  ├─ module-info.java
│  └─ demo/api/Greeting.java
└─ demo.app/
   ├─ module-info.java
   └─ demo/app/Main.java
```

### 文件一：提供方声明对外包

`src/demo.library/module-info.java`：

```java
module demo.library {
    exports demo.api;
}
```

### 文件二：提供一个可调用的类

`src/demo.library/demo/api/Greeting.java`：

```java
package demo.api;

public class Greeting {
    private String token = "demo-token"; // 后面的反射实验使用，无真实敏感数据
    public String hello() {
        return "hello from library";
    }
}
```

### 文件三：使用方声明读取哪个模块

`src/demo.app/module-info.java`：

```java
module demo.app {
    requires demo.library;
}
```

### 文件四：普通 Java 代码照常 import 和调用

`src/demo.app/demo/app/Main.java`：

```java
package demo.app;

import demo.api.Greeting;

public class Main {
    public static void main(String[] args) {
        System.out.println(new Greeting().hello());
    }
}
```

在实验根目录执行 PowerShell 命令：

```powershell
javac -encoding UTF-8 -d out --module-source-path src src/demo.library/module-info.java src/demo.library/demo/api/Greeting.java src/demo.app/module-info.java src/demo.app/demo/app/Main.java
java --module-path out -m demo.app/demo.app.Main
```

应输出：

```text
hello from library
```

第一条编译四个源文件并按模块组织到 out。第二条在 out 中找模块，`-m` 后的 `demo.app/demo.app.Main` 分别是“模块名 / main 类完整类名”。

相比普通类调用，多出的工作就是让提供方声明 exports，让使用方声明 requires；真正的方法调用仍是普通 Java 代码。

## 4. 🧪 不背定义，删一行看结果

每次只改一个地方，改回后再做下一项。为避免复用旧产物，验证失败场景时将编译命令的 `-d out` 改为新的输出目录名，例如 `-d out-no-requires`；编译失败后不要运行原 out 中的旧程序。

| 修改 | 再编译会发生什么 | 说明 |
| --- | --- | --- |
| 删掉 app 中的 requires demo.library | 编译失败，调用模块不读取目标模块 | 有 import 仍然不够 |
| 恢复 requires，再删掉 library 中的 exports demo.api | 编译失败，目标模块没有导出这个包 | 类是 public 仍然不够 |
| 两行都恢复 | 编译通过，可以调用 | 可读关系、包导出和 Java 访问权限都满足 |

这就是模块系统的实际效果：把某些越过边界的依赖变成编译错误，而不是只有一份“请勿调用内部类”的约定。

## 5. 🔍 选读：为什么 exports 后仍不能随意反射 private 字段

将实验中的 `Main.java` 暂时替换成下面版本，其他文件不变：

```java
package demo.app;

import demo.api.Greeting;

public class Main {
    public static void main(String[] args) throws Exception {
        Greeting greeting = new Greeting();
        var field = Greeting.class.getDeclaredField("token");
        field.setAccessible(true);
        System.out.println(field.get(greeting));
    }
}
```

重新编译可以通过，运行到 setAccessible 时会抛 InaccessibleObjectException：普通调用可以访问已导出包的 public API，但该包没有向调用模块开放所需深反射。

如果提供方确实同意这个访问，再将 `src/demo.library/module-info.java` 改为：

```java
module demo.library {
    exports demo.api;
    opens demo.api to demo.app;
}
```

重新编译运行，才会输出 demo-token。opens 允许相应范围的深反射，不会让 `greeting.token` 这种直接访问 private 字段的源码变得合法。

实际报错时，先看异常给出的“哪个模块、哪个包、调用方是谁”，再查库版本和开放配置。历史兼容有时使用 `--add-opens target.module/package=caller.module`；classpath 调用方常用 ALL-UNNAMED。它是调整访问边界的参数，不应在不清楚目标包的情况下盲目添加。

## 6. 🆚 它和 Maven 多模块是不是一回事

不是，同一个词在不同层次使用：

| 概念 | 主要职责 | 能否仅凭存在它就说明用了 JPMS |
| --- | --- | --- |
| Java package | 组织类名与包级访问 | 不能 |
| jar 文件 | 打包类与资源 | 不能 |
| Maven / Gradle 多模块 | 组织构建、依赖下载与项目产物 | 不能 |
| JPMS 模块 | 用描述符与模块路径建立可读和封装边界 | 本篇讲的就是它 |

因此，一个项目有 common、service、web 三个 Maven 子模块，不代表已经配置 module-info.java 或按 JPMS 模块方式运行。

## 7. 📘 需要时再查的术语

- java.base 被所有模块隐式依赖，String、List 等常用类型不要求你显式写 requires java.base。
- classpath 中的应用类属于对应类加载器的未命名模块；本篇命名模块使用 --module-path，不是简单给项目文件夹换个名字。
- 无 module-info 的 jar 放到 module-path 时可能成为自动模块，名称与访问规则需要检查，不能把 classpath 依赖直接搬过去就假设完全兼容。
- requires transitive 处理可读关系传递，不等于把依赖 jar 打包进自己的 jar。
- exports 不递归导出子包；open module 的反射开放范围比定向 opens 更广。
- --add-exports 调整包导出，--add-opens 调整深反射开放，--add-reads 调整可读关系。
- uses / provides 表达模块化服务的消费与提供；SPI 深入复用 [[../02-JVM/05-类加载机制与双亲委派|类加载章节]]。
- `java --describe-module java.sql` 可查看 JDK 模块描述；jdeps 帮助检查依赖；jlink 可以组装所需模块的运行时。它们不是继续学习 Spring 的强制前置。

## 🎯 自测与面试问题

> [!question]- 目前不写 module-info.java，能不能继续用 JDK 21 学 Spring？
> 可以。应用可以继续按 classpath 方式构建运行。本篇帮助理解模块化库、JDK 封装和访问异常，不要求为了学习而迁移所有项目。

> [!question]- 本篇两个模块为什么同时需要 requires 和 exports？
> 使用方需要读取目标模块，提供方需要导出相应包；此外类与成员还要满足 Java 访问规则。只写 import 或只把类声明为 public 不能替代这两层条件。

> [!question]- exports 和 opens 的实验结果有什么区别？
> exports 让普通源码能使用相应 public API；跨模块深反射访问 private 字段还需要合适的 opens。opens 不赋予源码直接访问 private 成员的权限。

> [!question]- Maven 多模块项目是否就是 JPMS 项目？
> 不是。一个描述构建与依赖组织，一个描述 Java 模块的可读与封装关系，项目可以只用前者。

## ✅ 学到什么程度就够

- 现在先能说出它用于模块依赖、公开包和反射边界，知道这是选读主题。
- 深入时能跑通两个模块，并通过删除 requires / exports 复现对应错误。
- 遇到反射报错时，能区分是否需要普通访问、深反射或库版本适配。
- 不把 Maven module、包、jar 和 JPMS 模块混为一谈。

## 🪝 记忆钩子

requires：我要用谁；exports：我公开什么包；opens：我允许谁深入反射。

## 🔗 关联笔记

- [[06-面向对象与方法调用]]：Java 访问修饰符。
- [[12-反射与动态代理/_index|反射与动态代理]]：反射调用与访问控制。
- [[../02-JVM/05-类加载机制与双亲委派|类加载机制]]；复习见 [[99-本章总结与复习]]；本章导航：[[_index]]。

## 📚 参考资料

- [JLS 7：包与模块](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html)
- [AccessibleObject API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AccessibleObject.html)
- [java 命令与模块路径](https://docs.oracle.com/en/java/javase/21/docs/specs/man/java.html)
- [jlink 工具](https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html)
