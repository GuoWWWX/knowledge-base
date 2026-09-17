---
title: 04 · Spring 生态（学习导航与知识全景）
tags: [类型/索引, 领域/Java后端, 主题/Spring]
created: 2026-05-31
updated: 2026-09-11
---

# 🌱 04 · Spring 生态（从入门到架构精通指南）

> [!tip] 学习心法
> **Spring 的核心精髓是“把对象的创建与关系的维护交出去（IoC），把通用的逻辑与横切的关注点抽出来（AOP）”。**
> 本模块立足于第一性原理，采用 **【核心机制原理 + 源码与底层实现 + 生产实战与避坑指南】** 的结构化模型编写。示例以 **Java 21 LTS + Spring 6.x / Spring Boot 3.x** 为基准。

---

## 🗺️ 推荐通关路线（五大进阶关卡）

请按照以下由浅入深的技术演进路径循序渐进：

### 🟢 第一关：Spring 核心内功（底层基石）
- [x] [[00-Spring生态全景与心智模型]] — Spring 核心体系认知：IoC、AOP 与全家桶演进脉络
- [x] [[01-IoC容器与Bean生命周期]] — BeanDefinition、实例化、属性填充、初始化与销毁五大阶段及后置处理器扩展
- [ ] [[13-Spring循环依赖与三级缓存]] — 单例 Bean 循环依赖解决方案、二级缓存局限性与提前 AOP 代理机制
- [ ] [[02-AOP原理与应用]] — 动态代理底层机制（JDK Proxy vs CGLIB）、切面织入与内部自调用失效破解
- [ ] [[03-Spring事务管理]] — 声明式事务传播机制、底层连接管理与事务失效的 7 大高频场景与破解
- [ ] [[11-Spring事件机制]] — 事件发布与监听解耦、同步/异步事件处理与 `@TransactionalEventListener` 事务同步监听

### 🟡 第二关：Web 与持久层工业利器
- [ ] [[06-SpringMVC请求处理流程]] — DispatcherServlet 核心请求分发链路、HandlerMapping、HandlerAdapter 与全局异常处理
- [ ] [[07-MyBatis原理]] — Mapper 动态代理底层机制、SqlSession 执行流程与 Executor/StatementHandler 四大核心组件
- [ ] [[16-SpringCache与分布式缓存实战]] — 声明式缓存抽象、高并发防击穿（sync=true）、防穿透/防雪崩配置与两级缓存架构
- [ ] [[12-Spring-Security]] — SecurityFilterChain 过滤器链架构、认证与授权核心流程与前后端分离 JWT 实战

### 🟠 第三关：现代 SpringBoot 魔法引擎
- [ ] [[04-SpringBoot自动配置原理]] — `@EnableAutoConfiguration`、AutoConfigurationImportSelector 与 `@Conditional` 条件注解装配机制
- [ ] [[05-SpringBoot Starter机制]] — 约定优于配置、自动装配规范与手写企业级自定义 Starter 实战
- [ ] [[10-SpringBoot启动流程]] — `SpringApplication.run()` 核心步骤、环境准备、上下文刷新与各阶段扩展监听点
- [ ] [[15-Spring异步与线程池体系]] — `@Async` 异步机制、ThreadPoolTaskExecutor 生产配置、TaskDecorator 跨线程 Context 透传与优雅停机
- [ ] [[14-SpringBoot生产运维与Actuator]] — 生产级白盒监控、核心端点安全策略、Prometheus 集成、K8s 双探针与优雅停机

### 🔴 第四关：分布式微服务与前沿
- [ ] [[08-SpringCloud组件概览]] — 微服务治理全景九大支柱：服务注册发现（Nacos）、声明式调用（OpenFeign）、网关（Gateway）、流量防护（Sentinel）、分布式事务（Seata）、链路追踪与事件驱动
- [ ] [[09-WebFlux与响应式编程]] — Project Reactor 响应式编程模型（Flux/Mono）、背压机制（Backpressure）与虚拟线程选型对比

### 🟣 第五关：大结局与大厂面试冲刺
- [ ] [[99-本章总结与复习]] — Spring 全家桶知识全景收拢、高频面试题深度复盘与 Spring 经典设计模式盘点

---

## 🧩 跨模块串联

- [[../10-知识串联与专题/02-一次Java请求的全链路]]
- [[../10-知识串联与专题/04-从Spring事务到分布式一致性]]
- [[../10-知识串联与专题/06-线程池-CompletableFuture-MQ异步链路]]
- 上级导航：[[../00-总览/学习仪表盘|Java 学习仪表盘]]
