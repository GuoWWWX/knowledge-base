---
title: ChatMemory 记忆
tags:
  - 类型/知识
  - 主题/SpringAI
  - 阶段/02
created: 2026-07-04
updated: 2026-07-04
status: 已完成
mastery: ⭐☆☆☆☆
---

# 🧠 ChatMemory 记忆

> [!abstract] 一句话总结
> ChatMemory 负责让模型“看见必要的历史对话”，但它不是无限记忆，而是受 Token、隐私和会话边界约束的上下文管理。

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

```java
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

- [ ] **历史全塞进去**：Token 爆炸且容易引入噪声。
- [ ] **会话 ID 设计太粗**：不同场景互相污染。
- [ ] **把长期事实和短期聊天混存**：后续很难清理。
- [ ] **忽略用户删除权**：记忆是数据资产，也可能是合规风险。

## 🎯 面试速记

> - **Q: ChatMemory 的本质是什么？**
>   - A: 应用侧保存历史消息，并在下一轮调用时把必要历史重新注入 Prompt。
> - **Q: 记忆和向量库有什么区别？**
>   - A: 记忆偏会话上下文，向量库偏长期知识检索；语义记忆会用到向量库。

## 🔗 关联阅读

- [[07-Advisor拦截器]]
- [[09-VectorStore向量存储]]
- [[02-ChatClient与ChatModel]]

## 📚 参考资料

- [Spring AI Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html)
