---
title: ChatMemory 记忆
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

# 🧠 ChatMemory 记忆

> [!abstract] 一句话总结
> ChatMemory 负责让模型“看见必要的历史对话”，但它不是无限记忆，而是受 Token、隐私和会话边界约束的上下文管理。

## 🎯 本节定位

本篇介绍 `ChatMemory` 的记忆类型和会话边界，并通过 [[07-Advisor拦截器|Advisor]] 将历史消息加入请求。滑动窗口、摘要和语义记忆等方案见 [[../../04-多轮对话与上下文工程/_index|多轮对话与上下文工程]]。

## 📌 这是什么

LLM 本身不会记住你上一次请求。所谓“记忆”，本质是应用把历史消息重新放进本次 Prompt。

```text
第 1 轮：用户说名字
应用存消息
第 2 轮：用户问“我叫什么”
应用把历史消息带给模型
模型回答
```

Spring AI 的 Chat Memory 相关能力通常和 Advisor 一起使用，让历史消息自动注入到 `ChatClient` 调用中。

## 🤔 解决什么问题

| 没有记忆 | 有记忆 |
| ---- | ---- |
| 每轮对话互相独立 | 能承接上下文 |
| 用户要反复说明背景 | 能复用历史信息 |
| 多轮任务难推进 | 能逐步完成 |
| 无法做会话级助手 | 能按 conversationId 管理 |

---

## 🔍 为什么“历史记录”和“记忆”不是同一份数据？

完整聊天历史用于让用户回看或供应用审计；ChatMemory 用于选出这次模型需要的上下文。窗口超出限制后，较早消息可能不再进入 Prompt，因此不能依赖窗口记忆作为完整聊天记录。
例如用户第一轮给出订单号，第十轮追问“它到哪了”：如果订单号已被窗口裁掉，模型就可能不知道“它”指什么。可以按需要保存业务状态或摘要，但需要测试信息是否被遗漏。

## 🧩 记忆类型

| 类型 | 说明 | 适合场景 |
| ---- | ---- | ---- |
| 窗口记忆 | 保留最近 N 条消息 | 普通聊天 |
| 摘要记忆 | 旧消息压缩成摘要 | 长对话 |
| 持久化记忆 | 存 Redis / DB | 多端会话 |
| 语义记忆 | 向量化后按需检索 | 长期偏好、知识回忆 |

学习阶段先掌握窗口记忆；做产品时再考虑摘要和语义记忆。

---

## 💻 示例：基于 Advisor 注入记忆

> [!info] ChatMemory 的三个角色
> `ChatMemory`（接口）→ `MessageWindowChatMemory`（内置窗口实现，默认 20 条，超限淘汰旧消息但保留 system 消息）→ `ChatMemoryRepository`（存储实现：`InMemoryChatMemoryRepository`、`JdbcChatMemoryRepository`、`RedisChatMemoryRepository`、`MongoChatMemoryRepository`、`CassandraChatMemoryRepository`、`Neo4jChatMemoryRepository`）。

```java
@Bean
ChatMemory chatMemory() {
    return MessageWindowChatMemory.builder()
            .maxMessages(20)                                     // 窗口大小（默认 20）
            .chatMemoryRepository(new InMemoryChatMemoryRepository())  // 换 JDBC/Redis 即可持久化
            .build();
}

@Bean
ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
    return builder
            .defaultAdvisors(
                    MessageChatMemoryAdvisor.builder(chatMemory).build()
            )
            .build();
}
```

调用时要带会话 ID：

```java
String answer = chatClient.prompt()
        .user("我叫小李，喜欢 Java。")
        .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, "user-1001"))
        .call()
        .content();
```

> [!warning] 会话 ID 要明确传入
> 本文写法按 Spring AI 当前文档的记忆 Advisor 接口组织：调用时传入 `ChatMemory.CONVERSATION_ID`，否则会报错。不同版本的接口和默认行为应查看对应版本文档，不要依赖固定默认会话。

后续同一个 `conversationId` 的请求就能带上历史。

---

## ⚙️ 会话边界

```text
conversationId = 用户 + 会话 + 场景
```

不要只用 `userId` 当 conversationId。一个用户可能同时有“客服咨询”“代码助手”“简历修改”多个会话，混在一起会污染上下文。
推荐：

```text
chat:{userId}:{sessionId}
```

## 🔐 隐私与安全

- 敏感信息进入记忆前要脱敏或避免存储
- 用户要能清空会话
- 企业场景要区分租户
- 工具返回的机密数据不要长期记忆
- Prompt Injection 内容不要直接写入长期记忆

---

## ✅ 学习检查点

- [ ] 能解释 LLM 本身没有记忆
- [ ] 能用 conversationId 区分会话
- [ ] 能说出窗口记忆、摘要记忆、语义记忆区别
- [ ] 能说明记忆和 VectorStore 的关系

## ⚠️ 常见坑

- 把全部历史放进 Prompt 会增加 Token 消耗和无关信息。
- 不同用户或场景共用会话 ID，容易造成上下文串线。
- 长期事实和短期聊天混存，后续更新或删除会比较困难。
- 设计记忆时要考虑用户删除数据的需求。

## 🎯 面试速记

> [!question]- ChatMemory 的本质是什么？
> 应用侧保存和选择历史消息，在下一轮调用时把必要上下文加入 Prompt；是否长期保存完整历史取决于存储设计。

> [!question]- 记忆和向量库有什么区别？
> 记忆是对话上下文管理需求，向量库是可用于相似度查询的存储方式；语义记忆可以使用向量库实现。

## 🔗 关联阅读

- [[07-Advisor拦截器]]
- [[09-VectorStore向量存储]]
- [[02-ChatClient与ChatModel]]
- [[../_index]]

## 📚 参考资料

- [Spring AI Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)
