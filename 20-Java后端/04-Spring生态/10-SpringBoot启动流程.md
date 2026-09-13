---
title: SpringBoot启动流程
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Spring
  - 阶段/04
created: 2026-08-06
updated: 2026-09-11
status: 已完成
mastery: ⭐⭐⭐⭐⭐
---

# 🚀 10 · SpringBoot 启动流程（火箭点火升空与 run() 核心拆解）

> **一句话导读**：看似普普通通的一行 `SpringApplication.run(App.class, args)`，背后却经历了一场精密绝伦的**“重型火箭自检与点火升空”**。搞懂这 12 步启动时序，Spring Boot 的底层生命节拍与扩展点你便能了然于胸。

---

## 🏛️ SpringApplication.run() 核心启动流程宏观全景

你在 `main` 方法中敲下的那一行代码，就像发射基地指挥长按下了**总点火按钮**：

```mermaid
flowchart TD
    Button["🔴 点击运行 main()：SpringApplication.run(App.class, args)"] --> S1["1. 确定火箭类型 (推断 Web 环境：Servlet / Reactive / None)"]
    S1 --> S2["2. 铺设通信链路 (读取环境变量、配置数据 Environment)"]
    S2 --> S3["3. 建造核心驾驶舱 (创建 ApplicationContext 容器)"]
    S3 --> S4["4. 🔥 核心总点火！容器刷新 refreshContext()<br/>👉 加载自动配置、创建所有 Bean、启动内置 Tomcat！"]
    S4 --> S5["5. 执行起飞前核验 (调用 ApplicationRunner 预热缓存)"]
    S5 --> S6["6. 🚀 成功入轨！对外宣布应用完全就绪 (ApplicationReadyEvent)"]
```

---

## 🏃 启动核心生命节拍（大厂面试满分链路）

整个启动过程严密划分为 **两大阶段、六大步骤**：

```mermaid
sequenceDiagram
    autonumber
    actor Dev as 开发者执行 main()
    participant App as SpringApplication 实例
    participant Env as Environment (环境与配置)
    participant Context as ApplicationContext (IoC 容器)
    participant Runners as 启动任务 (Runners)

    Note over Dev, App: 【阶段一：构造 SpringApplication 实例】
    Dev->>App: 1. new SpringApplication(App.class)
    App->>App: 2. 推断 Web 类型 (检查是否存在 DispatcherServlet)
    App->>App: 3. 加载所有的 ApplicationContextInitializer 和 ApplicationListener

    Note over Dev, Context: 【阶段二：正式执行 run() 启动】
    App->>Env: 4. 准备 Environment (读取命令行、系统参数、application.yml)
    App->>Context: 5. 创建对应类型的 ApplicationContext 容器
    App->>Context: 6. 预处理容器 (prepareContext：注入主类 BeanDefinition)
    
    Note over Context: 7. 🌟 终极核心：refreshContext(context)<br/>(调用 Spring 底层著名的 refresh() 12 步！)
    Context->>Context: 7.1 解析所有 @Configuration 与自动配置类
    Context->>Context: 7.2 🚀 启动嵌入式 Web 容器 (启动内置 Tomcat 监听端口！)
    Context->>Context: 7.3 实例化并装配所有单例 Bean
    
    App->>App: 8. 发布 ApplicationStartedEvent 事件 (容器刷新完毕)
    App->>Runners: 9. callRunners：执行所有的 CommandLineRunner 和 ApplicationRunner
    App->>App: 10. 发布 ApplicationReadyEvent 事件 (应用完全就绪，开始接单！)
    App-->>Dev: 11. 返回可用的 context
```

---

## 🔬 终极核心拆解：refresh() 里到底干了啥惊天动地的事？

在第 7 步 `refreshContext()` 中，Spring Boot 委托调用了 Spring 底层最著名的 **`AbstractApplicationContext.refresh()`** 方法。
这是 Spring 整个框架中最繁重、最核心的“心脏搏动点”：

1. **`invokeBeanFactoryPostProcessors`**：
   - 执行工厂后置处理器。我们在前面学到的 **`@ComponentScan` 扫描、`@EnableAutoConfiguration` 自动配置类解析**，全都在这一步彻底执行完成！所有的施工图纸（BeanDefinition）在这一刻全部就位！
2. **`registerBeanPostProcessors`**：
   - 注册所有的 `BeanPostProcessor`（也就是后面负责为 Bean 生成 AOP 动态代理的监理们全部就位！）。
3. **`onRefresh()`（🌟 Spring Boot 的神来之笔！）**：
   - 传统的 Spring 这里是空的；
   - **Spring Boot 在这里直接启动了内置的 Tomcat / Netty 服务器！**
   - 不再需要把 war 包放到外部的 Tomcat 目录下，项目直接内嵌服务器化身独立自包含进程！
4. **`finishBeanFactoryInitialization`**：
   - 将容器中所有的非懒加载单例 Bean 真正实例化（调用构造方法、属性填充、初始化方法、AOP 代理织入）。

---

## 🏃 启动扩展点：ApplicationRunner 与 CommandLineRunner

很多时候，我们希望在**项目完全启动好的一瞬间，自动执行一些预处理工作**（比如：把热点字典数据提前加载进 Redis 缓存、建立 MQ 消费者长连接）。

Spring Boot 提供了两个开箱即用的“起飞核验”接口：

```java
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

// 方式一：CommandLineRunner (入参是原始 String[] 数组)
@Component
@Order(1) // 控制执行顺序，数字越小越先执行
public class WarmUpCacheRunner implements CommandLineRunner {
    @Override
    public void run(String... args) {
        System.out.println("🔥 [Runner 1] 项目启动完成，开始预热 Redis 基础缓存...");
    }
}

// 方式二：ApplicationRunner (入参是封装好的高级 ApplicationArguments 对象，推荐！)
@Component
@Order(2)
public class InitSystemCheckRunner implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) {
        System.out.println("🔍 [Runner 2] 检查远程微服务与数据库连通性...");
    }
}
```

> 💡 **两者的唯一区别**：
> 功能完全一致，区别仅在于**入参解析方式**：
> `CommandLineRunner` 只能拿到原汁原味的原始参数数组 `String[] args`；
> `ApplicationRunner` 帮你把参数智能解析成了诸如 `--server.port=8080` 的 `OptionValues`，更易于提取键值对。

---

## 📜 生产高频避坑：配置属性覆盖优先级法则

面试和运维最容易遇到的抓狂问题：“为什么我在 `application.yml` 里改了端口，启动后它依然读的是旧端口？”
**因为 Spring Boot 有一套严格的配置覆盖金字塔：**

```mermaid
flowchart TD
    C1["1. 命令行参数 (最高优先级，直接压倒一切！)<br/>java -jar app.jar --server.port=9090"] --> C2["2. Java 系统属性 (-D 属性)<br/>java -Dserver.port=8081 -jar app.jar"]
    C2 --> C3["3. 操作系统环境变量<br/>export SERVER_PORT=8082"]
    C3 --> C4["4. 外部 application.yml (与 jar 包同级目录下的外置配置)"]
    C4 --> C5["5. 内部 application.yml (打包在 jar 包内部的内置配置，最低优先级)"]
```

> [!TIP]
> **线上运维救急神技**：
> 生产环境临时修改某个配置，**完全不需要重新打 jar 包上传**！直接在启动命令的最后加上 `--配置项名=新值`，就可以以最高优先级瞬间覆盖配置文件里的默认值！

---

## 🧠 一分钟复盘核心记忆卡

1. **两大阶段**：构造阶段（推断 Web 类型、加载监听器） + `run()` 阶段（准备环境、建容器、刷新容器、调 Runner）。
2. **核心脉搏**：`refreshContext()` 内部启动内嵌 Tomcat，完成自动配置解析与单例 Bean 实例化。
3. **启动钩子**：预热缓存与业务初始化，使用 `@Order` 排序的 `ApplicationRunner`。

---

## 🧭 下一步

掌握了现代单体 Spring Boot 的全套魔法，当业务飞速扩张，单体应用拆分为成百上千个微服务时，服务间如何互相发现？流量如何负载均衡？网络故障如何熔断？
请进入第四关：[[08-SpringCloud组件概览]]！
