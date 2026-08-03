---
title: 面试-Spring
tags:
  - 领域/求职
  - 领域/Java后端
  - 类型/面试
  - 主题/Spring
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# 面试-Spring

> [!abstract] 一句话总结
> 本文用于把 面试-Spring 的知识点整理成可直接复习和模拟面试的问答，回答时遵循“先结论、再原理、再项目落地、最后风险取舍”。

## 🎯 回答框架

1. **先给结论**：一句话回答面试官问题。
2. **展开原理**：讲核心机制、边界和常见误区。
3. **结合项目**：落到 RAG、Agent、Spring Boot、并发、数据库或系统设计项目。
4. **补充取舍**：说明为什么这样选，以及失败时怎么兜底。

## 高频问题

### Q1: Spring Bean 生命周期是什么？

> 实例化、属性填充、Aware、BeanPostProcessor 前置、初始化、后置、使用、销毁。扩展点集中在 BPP、FactoryBean、事件和条件装配。

### Q2: 事务为什么会失效？

> 常见原因有自调用、非 public 方法、异常被吞、传播行为不符合预期、没有被 Spring 管理。根因是声明式事务基于代理。

### Q3: Spring Boot 自动配置原理？

> 通过自动配置类、条件注解和配置属性，在满足条件时向容器注册 Bean。要会看 auto-configuration imports 和 condition report。

## 追问准备

- Bean 生命周期和循环依赖能否画出主流程？
- 事务失效能否结合自调用、异常、异步和多数据源说明？
- 自动配置没生效时如何用 condition report 排查？
- Spring MVC、Filter、Interceptor、AOP 的边界能否讲清？

## 🔗 关联笔记

- [[40-求职准备/_index|求职准备索引]]
- [[../20-Java后端/04-Spring生态/_index]]

## 📚 参考资料

- [[../20-Java后端/04-Spring生态/01-IoC容器与Bean生命周期]]
- [[../20-Java后端/04-Spring生态/03-Spring事务管理]]
- [[../20-Java后端/04-Spring生态/04-SpringBoot自动配置原理]]
- [[../20-Java后端/04-Spring生态/06-SpringMVC请求处理流程]]
