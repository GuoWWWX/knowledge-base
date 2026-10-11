---
title: Spring AI 与 LangChain4j 对比与选型
tags:
  - 领域/AI应用
  - 类型/对比
  - 主题/SpringAI
  - 主题/LangChain4j
  - 阶段/02
created: 2026-07-04
updated: 2026-09-28
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# ⚖️ Spring AI 与 LangChain4j 深度对比与架构选型

> [!abstract] 一句话总结
> **Spring AI** 深度集成 Spring Boot 自动配置、`ChatClient` 链式 API、`Advisor` 责任链与 Micrometer 可观测体系；**LangChain4j** 提供框架中立的 Java 组件与 `AiServices` 声明式接口代理。工程选型建议结合现有技术栈与目标特性统一主框架，降低多套抽象并存的维护成本。

---

## 📌 设计理念与技术架构对比

```mermaid
graph TD
    subgraph SpringAI["Spring AI 架构特点 (Spring Ecosystem Native)"]
        S_BOOT["Spring Boot Starter 自动装配"]
        S_API["Fluent API：ChatClient 链式调用门面"]
        S_AOP["拦截增强：Advisor 请求/响应责任链"]
        S_REACTIVE["Project Reactor：Flux 响应式流支持"]
        S_OBS["Micrometer 原生可观测性集成"]
    end

    subgraph LC4J["LangChain4j 架构特点 (Pure Java & Declarative)"]
        L_STAND["Java 17+ 独立核心库 (框架中立)"]
        L_API["AiServices：基于动态代理的声明式接口"]
        L_EXT["Listener 监听钩子与 SPI 扩展机制"]
        L_STREAM["StreamingChatResponseHandler / TokenStream 流式回调"]
        L_MODEL["广泛的模型与向量数据库适配模块"]
    end

    style SpringAI fill:#e8f5e9,stroke:#2e7d32,stroke-width:1px
    style LC4J fill:#e3f2fd,stroke:#1565c0,stroke-width:1px
```

### 1. 核心维度对比

| 对比维度 | Spring AI | LangChain4j |
| :--- | :--- | :--- |
| **维护主体** | Spring 官方团队（VMware / Broadcom） | 开源社区驱动，核心维护活跃 |
| **运行环境要求** | 与 Spring Boot 版本线对应（1.x 基于 Boot 3.x，2.x 基于 Boot 4.x，要求 Java 17/21+） | 核心库基于 Java 17+，可脱离 Spring 独立运行，也提供 Spring Boot 与 Quarkus 集成 |
| **调用交互范式** | **Fluent API 链式构建**（`chatClient.prompt()...call()`） | **声明式接口动态代理**（`AiServices.builder(MyService.class)...build()`） |
| **切面增强机制** | **`Advisor` 责任链**（面向模型请求 `ChatClientRequest` 与响应的专用拦截链） | `ChatModelListener` 监听器钩子与 `AiServices` 内部编排扩展 |
| **流式响应支持** | 基于 **Project Reactor**，直接返回 `Flux<String>` / `Flux<ChatResponse>` | 基于 `StreamingChatResponseHandler` 回调接口或 `TokenStream` |
| **监控与可观测** | 原生接入 **Micrometer Observation**，指标与追踪可直接对接 Prometheus / OpenTelemetry | 提供请求/响应日志开关与 `ChatModelListener`，可通过监听器对接自定义监控 |
| **生态适配范围** | 覆盖主流企业级模型与向量库（PGVector、Milvus、Redis、Elasticsearch 等） | 模型与向量存储适配模块数量多，社区新增速度快 |

---

## 💻 编程范式对比：ChatClient vs AiServices

以实现一个**包含系统提示词、动态参数注入与结构化对象返回**的订单审核服务为例，对比两者的代码风格：

### 1. Spring AI：Fluent API 链式调用
```java
// Spring AI：通过 ChatClient 链式组装请求并映射结果
@Service
public class OrderService {
    private final ChatClient chatClient;

    public OrderService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public OrderAnalysisResult analyzeOrder(String orderText) {
        return chatClient.prompt()
                .system("你是一个专业的电商风控审核员。")
                .user(u -> u.text("请审查以下订单信息：{order}").param("order", orderText))
                .call()
                .entity(OrderAnalysisResult.class); // 结构化反序列化
    }
}
```

### 2. LangChain4j：AiServices 声明式接口
```java
// LangChain4j：通过接口与注解声明契约，运行时生成动态代理
public interface OrderFraudAuditor {

    @SystemMessage("你是一个专业的电商风控审核员。")
    @UserMessage("请审查以下订单信息：{{order}}")
    OrderAnalysisResult audit(@V("order") String orderText);
}

// 运行时构建代理实例
OrderFraudAuditor auditor = AiServices.builder(OrderFraudAuditor.class)
        .chatModel(model)
        .build();

OrderAnalysisResult result = auditor.audit(orderText);
```

> [!TIP]
> **编程模型特点**：
> - **Spring AI (`ChatClient`)** 与 `RestClient`、`WebClient` 的链式调用风格一致，适合在单次请求中灵活调整参数、动态挂载 `Advisor` 拦截器或直接处理 `Flux` 流；
> - **LangChain4j (`AiServices`)** 将提示词模板与业务方法签名绑定，隐藏了底层调用细节，便于上层业务像调用普通 Java 接口一样使用 AI 服务并进行 Mock 测试。

---

## 🎯 架构选型决策参考

```mermaid
flowchart TD
    Start([开始项目 AI 框架选型]) --> Q1{项目是否基于对应版本的<br/>Spring Boot 架构?}
    
    Q1 -->|否 / 纯 Java / Quarkus| LC4J1["选用 LangChain4j<br/>(框架中立，依赖轻量)"]
    Q1 -->|是| Q2{团队更倾向哪种编程模型与生态特性?}
    
    Q2 -->|倾向声明式接口代理<br/>或需使用特定社区适配模块| LC4J2["选用 LangChain4j<br/>(配合 langchain4j-spring-boot-starter)"]
    Q2 -->|倾向 Spring 官方组件规范<br/>WebClient 链式风格 / Advisor / Micrometer| SAI["选用 Spring AI<br/>(与 Spring 生态组件深度对齐)"]
    
    SAI --> Check1["复用 Actuator / Micrometer 观测与 Advisor 拦截链"]
    LC4J1 & LC4J2 --> Check2["使用 AiServices 声明式接口组织业务服务"]

    style SAI fill:#e8f5e9,stroke:#2e7d32,stroke-width:2px
    style LC4J1 fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
    style LC4J2 fill:#e3f2fd,stroke:#1565c0,stroke-width:2px
```

---

## ⚠️ 工程选型与治理注意事项

### 1. 保持单一模块内的框架收敛
- **混用成本**：在同一微服务模块中同时引入 `spring-ai` 与 `langchain4j` 会带来额外的工程复杂度：
  - **抽象不互通**：两套框架的 `Message`、`ChatMemory`、`@Tool` 及向量检索接口彼此独立，会话上下文与工具定义无法直接跨框架共享；
  - **依赖管理成本**：两者传递依赖的 HTTP 客户端、JSON 序列化库或字节码工具可能存在版本差异，增加依赖仲裁负担；
  - **团队维护分化**：链式调用与声明式接口并存会导致日志审计、异常处理与监控埋点需要维护两套实现。
- **工程建议**：同一业务服务优先统一使用一套主框架；若因特定模块能力确需共存，应按服务边界或独立适配层（Adapter）进行隔离。

### 2. 版本锁定与 BOM 管理
- 两个框架均处于持续迭代阶段（如 Spring AI 从 1.x 升级到 2.0 时调整了 Options Builder 与 `ChatMemory` 接口，移除了 `PromptChatMemoryAdvisor`）。
- **工程建议**：生产项目应锁定正式发布版本（GA），通过官方 BOM（`spring-ai-bom` 或 `langchain4j-bom`）统一管理子模块版本，并在升级前通过集成测试验证核心链路。

### 3. 补齐统一的可观测性与安全治理
- 无论选择哪个框架，生产环境均需确保模型调用具备超时控制、Token 用量统计、链路追踪（TraceId/MDC）以及输入输出敏感信息过滤。若使用 LangChain4j，可通过自定义 `ChatModelListener` 对接企业现有的 Micrometer / OpenTelemetry 体系。

---

## 🎯 高频面试问题

> [!question] 常见面试问题解析
> 
> **Q1：请从设计理念、编程范式和流式处理三个方面，对比 Spring AI 与 LangChain4j 的异同？**
> - **答**：
>   1. **设计理念**：Spring AI 是 Spring 官方项目，深度集成 Spring Boot 自动配置、依赖注入与 Micrometer 可观测性体系；LangChain4j 则是框架中立的 Java 库，既支持纯 Java 与 Quarkus，也提供 Spring Boot Starter；
>   2. **编程范式**：Spring AI 以 `ChatClient` 的 **Fluent 链式 API** 为核心，通过 `Advisor` 责任链实现记忆、RAG 和日志等横切逻辑；LangChain4j 主打 **`AiServices` 声明式接口**，基于 JDK 动态代理将注解与接口方法转化为模型调用链；
>   3. **流式处理**：Spring AI 原生基于 Project Reactor 返回 `Flux<String>` / `Flux<ChatResponse>`，与 WebFlux 响应式编程模型无缝衔接；LangChain4j 则主要基于 `StreamingChatResponseHandler` 回调或 `TokenStream` 处理增量输出。
>
> **Q2：在实际项目中如何进行技术选型？**
> - **答**：
>   主要评估三个维度：① **基础技术栈**：若核心服务基于匹配的 Spring Boot 体系，且已广泛使用 WebFlux、Micrometer 和 Spring Security，优先选择 **Spring AI** 以保持生态统一；若为非 Spring 环境、Quarkus 或轻量级独立应用，选择 **LangChain4j**；② **编程偏好**：偏好显式链式编排与统一拦截器链选 Spring AI，偏好面向接口声明服务契约选 LangChain4j；③ **特定组件支持**：核对目标大模型或向量数据库在对应框架中的适配成熟度。
>
> **Q3：为什么通常建议在一个服务模块中统一主框架，而不是混用 Spring AI 与 LangChain4j？**
> - **答**：
>   虽然两者在技术上可以共存，但在同一模块混用会带来三方面工程成本：① **核心抽象隔离**：两者的消息模型、`ChatMemory`、`@Tool` 注解及向量存储接口互不兼容，同一会话或工具链需要重复适配；② **依赖版本仲裁**：各自引入的底层网络库与工具包可能引发版本冲突；③ **治理与观测重复建设**：审计日志、异常重试与 Token 监控需要针对两套 API 分别实现。因此在单一服务内统一主框架能显著降低维护成本。

---

## 🪝 知识复盘卡

```
┌────────────────────────────────────────────────────────┐
│             Spring AI vs LangChain4j 复盘卡             │
├────────────────────────────────────────────────────────┤
│  1. 生态定位：Spring 官方深度集成 vs 社区驱动框架中立     │
│  2. 调用范式：ChatClient 链式 API vs AiServices 接口代理 │
│  3. 流式模型：Reactor Flux 响应式流 vs TokenStream 回调  │
│  4. 扩展机制：Advisor 责任链切面 vs ChatModelListener    │
│  5. 选型原则：结合现有技术栈统一主框架，用 BOM 锁定版本   │
└────────────────────────────────────────────────────────┘
```

---

## 🔗 关联笔记

- [[00-Java-AI框架全景与心智模型]] - 全局分层架构与核心抽象
- [[SpringAI/02-ChatClient与ChatModel]] - Spring AI 调用门面与底层接口
- [[LangChain4j/02-AiServices声明式AI服务]] - LangChain4j 声明式 AI 服务详解
- [[_index]] - 返回本章索引
