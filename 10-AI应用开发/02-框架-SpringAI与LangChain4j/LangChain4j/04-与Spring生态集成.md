---
title: 与Spring生态集成
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/LangChain4j
  - 阶段/02
created: 2026-08-04
updated: 2026-08-04
status: 已完成
mastery: ⭐⭐⭐☆☆
---

# 与Spring生态集成

> [!abstract] 一句话总结
> LangChain4j 虽然不强制绑定 Spring，但官方提供 **Spring Boot Starter** 让模型、工具、记忆自动装配成 Bean；与 Spring AI 相比，它"可脱离 Spring 使用、融入后也很顺手"——本课讲清集成方式与工程选型。

## 🎯 本节定位

- **LangChain4j 子区第 4 课（收尾）**。前置：[[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/01-LangChain4j核心概念]]、[[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/02-AiServices声明式AI服务]]、[[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/03-记忆与RAG]]。
- 收尾后读 [[10-AI应用开发/02-框架-SpringAI与LangChain4j/01-SpringAI与LangChain4j对比|两框架对比]] 做最终选型。
- 延伸：非 Spring 环境（Quarkus、纯 Java）的用法只做概念了解。

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
@Autowired ChatLanguageModel chatLanguageModel;          // 模型
@Autowired EmbeddingModel embeddingModel;                // 向量化
// 工具类 @Tool 方法所在 Bean 自动被发现（配合 AiServices）
```

## 🤔 解决什么问题 / 为什么需要

- **免手工装配**：模型、向量模型由 Starter 自动创建为 Bean，配置走 yaml。
- **工具即 Bean**：`@Tool` 标注的方法在 Spring 容器里自动注册，`AiServices` 装配时直接取。
- **两种集成深度**：轻量（只用模型 Bean）到完整（AiServices + 记忆 + RAG 全自动）。

## 🧩 与 Spring AI 的集成对照

| 维度 | Spring AI | LangChain4j |
| ---- | ---- | ---- |
| Starter | `spring-ai-starter-model-openai` | `langchain4j-open-ai-spring-boot-starter` |
| 配置前缀 | `spring.ai.openai.*` | `langchain4j.open-ai.*` |
| 自动配置的 Bean | `ChatClient.Builder`、`ChatModel` 等 | `ChatLanguageModel`、`EmbeddingModel` 等 |
| 核心调用对象 | `ChatClient`（链式） | `AiServices`（声明式）或直接用模型 Bean |
| 非 Spring 可用性 | 不能脱离 Spring | 可纯 Java 使用 |
| 生态归属 | Spring 官方 | 社区 + Spring 集成 |

## 💻 代码实践

```java
// 方式一：直接用自动配置的模型 Bean（轻量）
@RestController
class ChatController {
    private final ChatLanguageModel model;
    ChatController(ChatLanguageModel model) { this.model = model; }

    @GetMapping("/chat")
    String chat(String msg) {
        return model.chat(msg);
    }
}

// 方式二：AiServices + 工具 Bean（完整）
@Component
class OrderTool {
    @Tool("查询订单状态")
    public String getStatus(@ToolParam("订单号") String id) { ... }
}

@Configuration
class AssistantConfig {
    @Bean
    Assistant assistant(ChatLanguageModel model, OrderTool orderTool) {
        return AiServices.builder(Assistant.class)
                .chatLanguageModel(model)
                .tools(orderTool)          // Spring Bean 直接传入
                .build();
    }
}
```

## ⚠️ 踩坑与注意事项

- **版本管理**：和 Spring AI 一样用 BOM（`langchain4j-bom`）管版本，别混写各模块版本。
- **两个框架同工程**：Spring AI 与 LangChain4j 可以共存（Bean 名不冲突），但没必要——选型见对比篇，避免"框架内耗"。
- Starter 自动配置的模型 Bean 若想自定义（如超时、baseUrl），在 yaml 里配 `langchain4j.*` 属性，或用 `@Bean` 覆盖。
- 纯 Java（非 Spring）环境：不用 Starter，手动 `OpenAiChatModel.builder()...build()` 即可——这是 LangChain4j 相对 Spring AI 的灵活性优势。

## 🧪 最小验证

```bash
# 一个能跑的 Spring Boot + LangChain4j 最小工程：
# 1. 加 langchain4j-spring-boot-starter + langchain4j-open-ai-spring-boot-starter
# 2. yaml 配 api-key
# 3. 注入 ChatLanguageModel 写一个 /chat 接口
# 4. curl "http://localhost:8080/chat?msg=你好" 看到回复即通
```

## ✅ 精通检查清单

- 能说出 LangChain4j 的两个 Starter 与配置前缀。
- 能写出"自动配置 Bean + AiServices + 工具 Bean"的完整集成。
- 能说出 LangChain4j 与 Spring AI 在"非 Spring 可用性"上的差异。

## 🔗 关联笔记

- 本子区：[[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/01-LangChain4j核心概念]] · [[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/02-AiServices声明式AI服务]] · [[10-AI应用开发/02-框架-SpringAI与LangChain4j/LangChain4j/03-记忆与RAG]]
- 选型：[[10-AI应用开发/02-框架-SpringAI与LangChain4j/01-SpringAI与LangChain4j对比|两框架对比]]
- 项目落地：[[10-AI应用开发/08-项目实战/_index|08-项目实战]]

## 📚 参考资料

- [LangChain4j Spring Boot 集成](https://docs.langchain4j.dev/tutorials/spring-boot-integration)
- [LangChain4j 官方文档](https://docs.langchain4j.dev/)


