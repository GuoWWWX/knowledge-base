---
title: 04 · Spring 生态（索引）
tags: [类型/索引, 领域/Java后端, 主题/Spring]
created: 2026-05-31
---

# 04 · Spring 生态

> [!info] 本区说明
> Spring 核心容器、AOP、事务、Boot 自动配置、MVC、MyBatis 与 Spring Cloud 的主线是：容器如何创建对象，代理如何增强调用，基础设施如何通过自动配置接入业务。

## 🧭 学习路径

- 先读 IoC，再读 AOP 和事务；事务本质是 AOP + 连接绑定。
- Boot 自动配置和 Starter 解决工程装配；MVC/MyBatis 是 Web 与数据访问入口。
- Spring Cloud 放到微服务阶段复习，重点看调用链上的超时、重试、注册发现和网关。

## ✅ 验收标准

- 能从 refresh 讲到 Bean 创建和后处理器。
- 能解释事务失效、自调用、rollback 规则和连接绑定。
- 能用 conditions report 排查自动配置为什么生效或没生效。

## 📚 知识笔记清单
- [x] [[01-IoC容器与Bean生命周期]]
- [x] [[02-AOP原理与应用]]
- [x] [[03-Spring事务管理]]
- [x] [[04-SpringBoot自动配置原理]]
- [x] [[05-SpringBoot Starter机制]]
- [x] [[06-SpringMVC请求处理流程]]
- [x] [[07-MyBatis原理]]
- [x] [[08-SpringCloud组件概览]]

## 🧾 本章复习

- [[99-本章总结与复习]]

## 🧩 跨模块串联

- [[20-Java后端/10-知识串联与专题/02-一次Java请求的全链路]]
- [[20-Java后端/10-知识串联与专题/04-从Spring事务到分布式一致性]]

## 🔗 相关
- 上级：[[20-Java后端/00-总览/学习仪表盘|Java 学习仪表盘]]
- 关联：[[../../10-AI应用开发/02-框架-SpringAI与LangChain4j/_index|Spring AI]]
