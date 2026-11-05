---
title: 与Spring生态集成
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/LangChain4j
  - 阶段/02
created: 2026-08-04
updated: 2026-09-28
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# 与Spring生态集成

> [!abstract] 一句话总结
> LangChain4j 虽然不强制绑定 Spring，但官方提供 **Spring Boot Starter** 让模型、工具、记忆自动装配成 Bean；与 Spring AI 相比，它"可脱离 Spring 使用、融入后也很顺手"——本课讲清集成方式与工程选型。

## 🎯 本节定位

了解 [[01-LangChain4j核心概念]]、[[02-AiServices声明式AI服务]] 和 [[03-记忆与RAG]] 后，可以在本篇学习 Spring Boot 集成，并了解纯 Java、Quarkus 等非 Spring 使用方式；选型参考 [[../01-SpringAI与LangChain4j对比|两框架对比]]。

## 📌 这是什么

LangChain4j 的 Spring Boot 集成分两层：

```xml
<!-- ① 通用 Starter：自动配置核心 Bean -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-spring-boot-starter</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
<!-- ② 模型 Starter：按 Provider 选一个，如 OpenAI -->
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai-spring-boot-starter</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

配置（application.yml）：

```yaml
langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY}
      model-name: gpt-4o-mini
```

启动后自动注入：

```java
@Autowired ChatModel chatLanguageModel;          // 自动装配的对话模型 Bean
// 若需注入 EmbeddingModel，可在配置文件中同步配置 langchain4j.open-ai.embedding-model.*
// 手动构建 AiServices 时显式传入工具；自动发现取决于装配模式
```

## 🤔 解决什么问题 / 为什么需要

- **免手工装配**：模型、向量模型由 Starter 自动创建为 Bean，配置走 yaml。
- **工具接入**：`@Tool` 方法可由 `@AiService` 的自动装配模式发现；手动使用 `AiServices.builder()` 时应显式传入需要的工具。自动发现的工具范围要核对，不能把工具发现当成权限控制。
- **两种集成深度**：轻量（只用模型 Bean）到完整（AiServices + 记忆 + RAG 全自动）。

## 🧩 与 Spring AI 的集成对照

| 维度 | Spring AI | LangChain4j |
| ---- | ---- | ---- |
| Starter | `spring-ai-starter-model-openai` | `langchain4j-open-ai-spring-boot-starter` |
| 配置前缀 | `spring.ai.openai.*` | `langchain4j.open-ai.*` |
| 自动配置的 Bean | `ChatClient.Builder`、`ChatModel` 等 | `ChatModel`、`EmbeddingModel` 等 |
| 核心调用对象 | `ChatClient`（链式） | `AiServices`（声明式）或直接用模型 Bean |
| 运行环境适配 | 深度集成 Spring Boot（也支持手动构造组件） | 核心库框架中立，支持纯 Java、Spring Boot 与 Quarkus |
| 生态归属 | Spring 官方 | 社区 + Spring 集成 |

## 💻 代码实践

下面的订单工具返回固定演示文本，不连接订单服务，不能作为真实查询结果。

```java
// 方式一：直接用自动配置的模型 Bean（轻量）
@RestController
class ChatController {
    private final ChatModel model;
    ChatController(ChatModel model) { this.model = model; }

    @GetMapping("/chat")
    String chat(String msg) {
        return model.chat(msg);
    }
}

// 方式二：AiServices + 工具 Bean（完整）
@Component
class OrderTool {
    @Tool("查询订单状态")
    public String getStatus(@P("订单号") String id) { return "演示状态：配送中"; }
}

@Configuration
class AssistantConfig {
    @Bean
    Assistant assistant(ChatModel model, OrderTool orderTool) {
        return AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(orderTool)          // Spring Bean 直接传入
                .build();
    }
}
```

## ⚠️ 踩坑与注意事项

- **版本管理**：和 Spring AI 一样用 BOM（`langchain4j-bom`）管版本，别混写各模块版本。
- Spring AI 与 LangChain4j 可以在同一工程中使用，但需要检查 Bean、依赖和配置是否冲突。
- Starter 自动配置的模型 Bean 若想自定义（如超时、baseUrl），在 yaml 里配 `langchain4j.*` 属性，或用 `@Bean` 覆盖。
- 纯 Java 环境可以不使用 Starter，手动通过 `OpenAiChatModel.builder()` 创建模型；所需依赖以项目版本为准。

## 🧪 最小验证

```bash
# 一个能跑的 Spring Boot + LangChain4j 最小工程：
# 1. 加 langchain4j-spring-boot-starter + langchain4j-open-ai-spring-boot-starter
# 2. yaml 配 api-key
# 3. 注入 ChatModel 写一个 /chat 接口
# 4. curl "http://localhost:8080/chat?msg=你好" 看到回复即通
```

## ✅ 精通检查清单

- 能说出 LangChain4j 的两个 Starter 与配置前缀。
- 能写出"自动配置 Bean + AiServices + 工具 Bean"的完整集成。
- 能说出 LangChain4j 与 Spring AI 在"非 Spring 可用性"上的差异。

## 🔗 关联笔记

- 本子区：[[01-LangChain4j核心概念]] · [[02-AiServices声明式AI服务]] · [[03-记忆与RAG]]
- 选型：[[../01-SpringAI与LangChain4j对比|两框架对比]]
- 项目落地：[[../../08-项目实战/_index|08-项目实战]]

## 📚 参考资料

- [LangChain4j Spring Boot 集成](https://docs.langchain4j.dev/tutorials/spring-boot-integration)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)
