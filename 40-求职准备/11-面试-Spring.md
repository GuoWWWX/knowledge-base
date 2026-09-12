---
title: 面试-Spring全家桶通关宝典
tags:
  - 领域/求职
  - 领域/Java后端
  - 类型/面试
  - 主题/Spring生态
created: 2026-07-04
updated: 2026-09-12
status: 已完成
mastery: ⭐⭐⭐⭐⭐
---

# 面试：Spring 全家桶核心高频硬核题库与大厂绝杀宝典

> [!abstract] 一句话总结
> Spring 面试的答题主线是**“IoC/AOP 底座 $\to$ Bean 生命周期与三级缓存 $\to$ 声明式事务代理穿透 $\to$ Spring Boot 自动装配 SPI”**。回答必须跳出简单的“控制反转和面向切面”口号，深入源码关键类（`BeanPostProcessor`、`AbstractAutowireCapableBeanFactory`、`TransactionAspectSupport`），先给 30 秒结论，再展开 2 分钟原理，最后落到生产落地。

---

## 🎯 大厂答题黄金框架

1. **一句话直击源码核心类**：如三级缓存对应 `singletonFactories`，自动装配对应 `AutoConfigurationImportSelector`。
2. **生命周期与时序清晰**：分段讲述实例化、属性注入、Aware、BPP 前置、初始化、BPP 后置代理。
3. **生产排坑与避坑姿势**：指出事务自调用为什么失效、构造器循环依赖为什么报 `BeanCurrentlyInCreationException`。
4. **版本差异洞察**：Spring Boot 2.x/3.x 默认 CGLIB 代理、Spring Boot 3 的 `spring.factories` 废弃与新型 imports 文件机制。

---

## 🔥 核心高频绝杀考题

### Q1: Spring Bean 的完整生命周期是怎样的？有哪些关键扩展点？
> **30 秒绝杀版**：
> Bean 生命周期经历四大核心阶段：**实例化（Instantiation） $\to$ 属性填充（PopulateBean） $\to$ 初始化（Initialization） $\to$ 销毁（Destruction）**。最核心的扩展点是 **`BeanPostProcessor`（BPP）**，它是 AOP 动态代理生成、注解解析（如 `@Autowired`）的灵魂支撑。
>
> **2 分钟深度展开**：
> 1. **实例化（`createBeanInstance`）**：通过反射或 CGLIB 调用构造函数在堆中分配对象内存（此时仅是个毛坯房，字段全为默认零值）。
> 2. **属性赋值（`populateBean`）**：解析依赖并注入属性，执行 `@Autowired`、`@Value` 的依赖装配。
> 3. **Aware 接口回调**：若实现了 `BeanNameAware`、`BeanFactoryAware`、`ApplicationContextAware`，Spring 会将容器自身的基础设施注入给 Bean。
> 4. **初始化阶段**：
>    - 执行 `BeanPostProcessor.postProcessBeforeInitialization()` 前置处理；
>    - 执行 `@PostConstruct` 注解方法；
>    - 若实现了 `InitializingBean`，调用 `afterPropertiesSet()`；
>    - 执行自定义的 `init-method`；
>    - 执行 **`BeanPostProcessor.postProcessAfterInitialization()` 后置处理** —— **这里是 AOP 动态代理织入、包装出最终 Proxy 对象的黄金发生地**！
> 5. **投入使用（In Use）**：存入单例池 `singletonObjects` 供业务调用。
> 6. **销毁阶段**：容器关闭时触发 `@PreDestroy` $\to$ `DisposableBean.destroy()` $\to$ 自定义 `destroy-method`。

---

### Q2: 什么是三级缓存？为什么二级缓存无法彻底解决循环依赖？
> **30 秒绝杀版**：
> 三级缓存是 Spring 解决**单例 Setter/属性注入循环依赖**的核心机制：
> - 一级缓存 `singletonObjects`：存放完全初始化好的成品 Bean；
> - 二级缓存 `earlySingletonObjects`：存放提前暴露的半成品 Bean（已实例化、未填充属性）；
> - 三级缓存 `singletonFactories`：存放 **`ObjectFactory<?>` 函数式单例工厂**。
> **如果只有二级缓存，也能解决普通的循环依赖，但必须在实例化后立即无条件生成 AOP 代理对象！这严重破坏了 Spring“在 Bean 初始化完毕后的 BPP 阶段才生成代理”的标准生命周期原则！三级缓存的存在就是为了“延迟生成 AOP 代理，有循环依赖才提前生成，无循环依赖按正常流程走”！**
>
> **2 分钟深度展开**：
> - **循环依赖闭环流程（A 依赖 B，B 依赖 A）**：
>   1. 创建 A：实例化 A $\to$ 将 `() -> getEarlyBeanReference(A)` 放入三级缓存 $\to$ 准备填充属性，发现依赖 B；
>   2. 创建 B：实例化 B $\to$ 准备填充属性，发现依赖 A $\to$ 查一级缓存无，查二级缓存无，查三级缓存命中了 A 的 `ObjectFactory`；
>   3. 触发工厂执行：如果 A 需要 AOP，此时**提前为 A 生成 CGLIB 代理对象**，将该代理对象放入**二级缓存**，并清空三级缓存；
>   4. B 拿到 A 的早期代理对象引用，成功完成属性填充和初始化，晋升为成品放入一级缓存；
>   5. A 恢复执行：拿到完全就绪的 B，完成属性填充与初始化；在 BPP 后置处理中发现自身代理已在二级缓存中提前生成，直接复用二级缓存的代理对象，入库一级缓存！
> - **死穴追问：构造器循环依赖为什么解不开？**
>   因为构造器循环依赖发生在第一步“实例化”阶段，此时连毛坯对象都尚未创建出来，根本无法向三级缓存中存放任何工厂或引用，直接抛出 `BeanCurrentlyInCreationException`。

---

### Q3: Spring AOP 的底层实现原理是什么？JDK 动态代理与 CGLIB 有何本质区别？
> **30 秒绝杀版**：
> Spring AOP 是基于**动态代理模式**实现的，在运行时通过增强器拦截目标方法调用。
> - **JDK 动态代理**：要求目标类**必须实现接口**，基于 Java 反射机制在运行时动态生成实现相同接口的 `$Proxy0` 字节码；
> - **CGLIB 动态代理**：无需接口，基于底层 **ASM 字节码框架**生成目标类的一个**子类（Subclass）**，通过重写父类非 `final` 方法实现增强。Spring Boot 2.x 起默认全面切换为 CGLIB 代理。
>
> **2 分钟深度展开**：
> - **性能与执行效率对比**：
>   - JDK 动态代理调用时需通过反射执行 `InvocationHandler.invoke()`，JDK 8 之后优化极大；
>   - CGLIB 采用 **FastClass 机制**：为代理类和目标类各生成一个 FastClass，为每个方法分配一个 `int index`，调用时直接通过索引 `switch-case` 跳转调用目标方法，完全避免了反射调用开销，执行吞吐更高。
> - **Spring Boot 为什么要默认全部切换为 CGLIB（`spring.aop.proxy-target-class=true`）？**
>   为了消灭**依赖注入类型转换异常（BeanNotOfRequiredTypeException）**！
>   如果用 JDK 动态代理，生成的代理类只实现了目标接口，并不是目标实现类本身。如果开发者在代码中习惯性使用实现类进行注入（如 `@Autowired private UserServiceImpl userService`），Spring 容器注入代理对象时会因为类型不匹配直接报错；而 CGLIB 生成的是目标实现类的子类，天然支持按接口或按实现类注入，大大提升了开发体验与健壮性。

---

### Q4: Spring 声明式事务（`@Transactional`）在哪些场景下会失效？如何排查？
> **30 秒绝杀版**：
> 声明式事务失效的万恶之源是：**“绕过了 Spring 的 AOP 代理对象直接调用了目标实例的原始方法”**。最典型的场景包括：**内部同类方法自调用**、**修饰在非 public 方法上**、**异常被业务代码 try-catch 吞掉**、**抛出非 RuntimeException 的受检异常**、**多线程异步调用**。
>
> **2 分钟深度展开**：
> 1. **同类内部自调用（Self-Invocation）**：方法 A（无事务）调用方法 B（有事务），由于是通过 `this.b()` 调用的，执行的是未被切面代理的原始对象，AOP 事务拦截器根本不会被触发！
>    - *破解*：通过 `((MyService) AopContext.currentProxy()).b()` 或注入自身代理调用。
> 2. **修饰在非 public 方法上**：Spring 的 `AbstractFallbackTransactionAttributeSource.computeTransactionAttribute` 会显式检查方法访问修饰符是否为 `public`，非 public 方法默认忽略事务属性。
> 3. **异常被吞没或类型不匹配**：
>    - 业务代码显式 `catch` 了异常且未重新抛出；
>    - 抛出了 `SQLException`、`IOException` 等受检异常（Checked Exception），由于 Spring 默认只对 `RuntimeException` 和 `Error` 回滚，事务依然会静默提交！必须强制加上 `@Transactional(rollbackFor = Exception.class)`。
> 4. **多线程并发调用**：在主事务方法中启动子线程执行数据库操作，由于数据库连接 `Connection` 是绑定在父线程的 `ThreadLocal` 中的，子线程拥有独立的连接与事务上下文，父事务回滚无法影响子线程！

---

### Q5: Spring Boot 的自动装配（Auto-Configuration）底层原理是什么？
> **30 秒绝杀版**：
> 核心由 **`@SpringBootApplication`** 注解驱动，它是一个复合注解，内含 **`@EnableAutoConfiguration`**。该注解通过 **`@Import(AutoConfigurationImportSelector.class)`**，在容器启动时扫描并加载约定目录下的配置文件，读取候选自动配置类列表，结合 **`@Conditional` 条件注解**（按需加载），将满足环境条件的 Bean 自动注册到 IoC 容器中。
>
> **2 分钟深度展开**：
> - **SPI 发现机制的演进**：
>   - **Spring Boot 2.x**：读取 `META-INF/spring.factories` 文件中以 `org.springframework.boot.autoconfigure.EnableAutoConfiguration` 为 Key 的类名列表；
>   - **Spring Boot 3.x**：彻底废弃上述机制，全面迁移至新型格式：**`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`**，每行配置一个全限定类名，提升加载性能。
> - **按需加载的灵魂：`@Conditional` 条件装配族**：
>   - `@ConditionalOnClass`：当类路径中存在指定的 Class（如看到 `RedisOperations.class` 才装配 Redis）；
>   - `@ConditionalOnMissingBean`：当用户没有自定义该 Bean 时才提供官方默认实现，**赋予了开发者最高优先级的自定义覆盖权**；
>   - `@ConditionalOnProperty`：当 `application.yml` 中配置了特定属性开关时才生效。

---

### Q6: Filter（过滤器）、Interceptor（拦截器）和 AOP 切面三者的执行顺序与本质区别是什么？
> **30 秒绝杀版**：
> - **Filter**：基于 Servlet 规范，属于 **Servlet 容器级别**，在请求进入 `DispatcherServlet` 之前执行，主要处理底层编码、跨域、安全认证和流包装；
> - **Interceptor**：基于 Spring MVC 框架，属于 **Spring 容器级别**，在 `DispatcherServlet` 与 Controller 之间执行，能感知具体的 Controller 映射与方法上下文；
> - **AOP**：基于动态代理，属于 **业务方法级别**，包裹在 Controller、Service 或 DAO 的具体方法周围，粒度最细。
> **执行顺序：`Filter.doFilter()` $\to$ `Interceptor.preHandle()` $\to$ `AOP 环绕前` $\to$ `Controller 业务方法` $\to$ `AOP 环绕后` $\to$ `Interceptor.postHandle()` $\to$ `Filter 回调`**。

---

## 4. 一分钟复盘记忆卡

```text
======================= Spring面试通关口诀 =======================
Spring底座IoC牢，AOP切面代理跑。
Bean生命期分四段，BPP前后把身造；
三级缓存破依赖，延迟生成代理巧；
构造循环解不开，原型作用域难逃；
事务失效源代理，自调私有非pub遭；
异常类型未覆盖，rollbackFor写好；
自动装配SPI引，条件注解按需挑；
从容应答源码处，大厂通关乐逍遥！
======================================================================
```
