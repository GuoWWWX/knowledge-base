---
title: 01 · Java 核心（索引）
tags: [类型/索引, 领域/Java后端]
created: 2026-05-31
updated: 2026-07-04
---

# 📘 01 · Java 核心

> [!info] 本区说明
> 语言基础、集合、IO/NIO、泛型、反射、注解、异常、新特性（Lambda/Stream/Record/虚拟线程）。

## 📚 知识笔记清单
- [ ] [[01-集合框架-List-Map-Set源码]]
- [ ] [[02-HashMap原理与扩容]]
- [ ] [[03-泛型与类型擦除]]
- [ ] [[04-反射与动态代理]]
- [ ] [[05-异常处理最佳实践]]
- [ ] [[06-IO与NIO]]
- [ ] [[07-Lambda与Stream]]
- [ ] [[08-Java新特性-Record-密封类-虚拟线程]]

## 🧭 精通路线

1. 先读 [[01-集合框架-List-Map-Set源码]] 和 [[02-HashMap原理与扩容]]，把 `ArrayList.grow`、`HashMap.putVal/resize`、`TreeMap.fixAfterInsertion` 这些源码入口串起来。
2. 再读 [[03-泛型与类型擦除]]、[[04-反射与动态代理]]、[[07-Lambda与Stream]]，用 `javap -v` 看 `Signature`、桥接方法、`invokedynamic` 和代理类调用路径。
3. 最后读 [[06-IO与NIO]]、[[05-异常处理最佳实践]]、[[08-Java新特性-Record-密封类-虚拟线程]]，重点放在文件句柄、编码、异常链、虚拟线程 pinning 和生产容量边界。

## 🧪 验证目标

- 能从源码入口解释一次 `put/get/remove`、一次反射调用、一次 Stream 终端操作的真实执行路径。
- 能写出最小代码复现泛型擦除、HashMap key 变异、try-with-resources suppressed 异常和 ByteBuffer `flip` 错误。
- 能把 Java 核心知识落到线上排查：慢接口看容器复杂度，内存涨看对象分配和集合持有，句柄泄漏看 `lsof` 与资源关闭。

## 🧾 本章复习

- [[99-本章总结与复习]]

## 🔗 相关
- 上级：[[20-Java后端/00-总览/学习仪表盘|Java 学习仪表盘]]
