---
title: Spring AI 项目搭建
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/SpringAI
  - 阶段/02
created: 2026-07-04
updated: 2026-08-03
status: 已完成
mastery: ⭐☆☆☆☆
---

# ☕ Spring AI 项目搭建

> [!abstract] 一句话总结
> Spring AI 项目搭建的核心不是“引很多包”，而是把 **Spring Boot 配置体系 + 模型 Starter + ChatClient 调用入口** 跑通。

## 🎯 本节定位

- **本章第 1 课，Spring AI 的入门课**。前置：上一章 [[10-AI应用开发/01-LLM基础与Prompt/_index|LLM 基础与 Prompt]] 的模型、Token、Prompt 概念；本节目标是跑通“Spring Boot + Starter + 配置 + 调用”的最小链路。
- **延伸**：调用入口的细节在下一课 [[02-ChatClient与ChatModel]]；多 Provider 接入在 [[05-多模型接入与切换]]。

## 📌 这是什么

Spring AI 是 Spring 生态里的 AI 应用框架。它把模型调用、提示词、工具调用、记忆、向量库、RAG 等能力抽象成 Spring Boot 熟悉的 Bean、AutoConfiguration 和 Starter。

最小项目只需要三件事：

```text
Spring Boot 应用
  + 一个模型 Starter
  + application.yml 中的 API Key / 模型配置
  + ChatClient 或 ChatModel 调用
```

## 🤔 解决什么问题

| 不用 Spring AI | 用 Spring AI |
| ---- | ---- |
| 每个模型 SDK 写法不同 | 用统一抽象切换 Provider |
| 业务代码直接拼 HTTP 请求 | 由 Starter 管连接、序列化和配置 |
| 工具调用、RAG、记忆各自散落 | 用 Spring Bean 组合 |
| 难接入企业 Spring Boot 工程 | 天然适配配置、监控、依赖注入 |

---

## 🧩 最小依赖

以 Maven 为例，真实版本以 Spring AI 官方 BOM 为准：

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.springframework.ai</groupId>
            <artifactId>spring-ai-bom</artifactId>
            <version>${spring-ai.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.ai</groupId>
        <artifactId>spring-ai-starter-model-openai</artifactId>
    </dependency>
</dependencies>
```

> [!tip] 版本原则
> Spring AI 版本变化较快，项目里优先用 BOM 管版本，不要手动给每个 Spring AI 模块写不同版本。

> [!info] 版本现状（2026-08）
> - **Spring AI 2.0.0 已 GA**（2026-06 发布）：基于 **Spring Boot 4.x**，MCP SDK 升级到 **2.0**。
> - 2.0 主要变化（影响写代码）：模型 Options 全面改为 **builder 模式**（setter 移除，如 `OpenAiChatOptions.builder().temperature(0.7).build()`）；`PromptChatMemoryAdvisor` 移除，记忆类 Advisor 必须显式传 conversationId；SAP HANA / Infinispan 向量库模块移除；ChatModel / Tool Calling 可观测性增强。
> - 维护线：1.1.8（Boot 3.5.x）、1.0.9；**新项目直接上 2.0**，老项目迁移参考[官方 Upgrade Notes](https://docs.spring.io/spring-ai/reference/upgrade-notes.html)。
> - 本课其余内容（BOM 管理、`ChatClient.Builder`、`spring.ai.openai.*` 配置）在 2.0 中保持不变。

## ⚙️ 基础配置

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4.1-mini
          temperature: 0.2
```

生产项目不要把 Key 写死在仓库里，优先使用环境变量、密钥管理系统或部署平台 Secret。

---

## 💻 最小调用

```java
@RestController
class ChatController {

    private final ChatClient chatClient;

    ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultSystem("你是一个简洁可靠的 Java AI 助手。")
                .build();
    }

    @GetMapping("/chat")
    String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
```

启动验证：

```bash
curl "http://localhost:8080/chat?message=用一句话解释Spring AI"
```

看到模型返回文本，就说明 Starter、配置、网络和调用链路都通了。

---

## ✅ 学习检查点

- [ ] 能解释 `spring-ai-bom` 为什么重要
- [ ] 能说出模型 Starter 的作用
- [ ] 能用 `ChatClient` 完成一次同步调用
- [ ] 能通过环境变量注入 API Key
- [ ] 能定位 401、模型名错误、网络超时这三类常见问题

## ⚠️ 常见坑

- [ ] **版本不一致**：Spring Boot、Spring AI、模型 Starter 版本不匹配，优先检查 BOM。
- [ ] **Key 写死**：学习可以临时写，项目中必须走环境变量或 Secret。
- [ ] **模型名过期**：模型 Provider 经常调整名称，报 400 时先查模型配置。
- [ ] **代理和网络**：本地能跑不代表服务器能连，部署前要验证出口网络。

## 🎯 面试速记

> - **Q: Spring AI 项目最小组成是什么？**
>   - A: Spring Boot 应用、模型 Starter、模型配置、`ChatClient` 或 `ChatModel` 调用入口。
> - **Q: 为什么推荐 BOM？**
>   - A: Spring AI 模块多且版本联动，BOM 能统一管理兼容版本，减少依赖冲突。

## 🔗 关联阅读

- [[02-ChatClient与ChatModel]]
- [[05-多模型接入与切换]]
- [[06-Function-Calling工具调用]]
- [[_index]]

## 📚 参考资料

- [Spring AI Reference Documentation](https://docs.spring.io/spring-ai/reference/)
- [Spring AI Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html)

