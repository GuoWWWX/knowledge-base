---
title: PromptTemplate
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

# 🧾 PromptTemplate

> [!abstract] 一句话总结
> `PromptTemplate` 用来把提示词从“字符串拼接”升级成“可复用、可传参、可测试”的模板。

## 🎯 本节定位

- **本章第 3 课，输入组织**。前置：[[02-ChatClient与ChatModel]] 的调用方式；上一章 [[10-AI应用开发/01-LLM基础与Prompt/07-Prompt工程基础|Prompt 工程基础]] 讲“怎么写提示词”，本节讲“怎么在代码里模板化”。
- **延伸**：把 RAG 检索结果注入模板见 [[09-VectorStore向量存储]]。

## 📌 这是什么

Prompt 是给模型的输入，Template 是可填变量的 Prompt。Spring AI 的 `PromptTemplate` 负责把模板文本和变量渲染成最终 Prompt。

```text
模板：请把 {topic} 解释给 {audience}
变量：topic=RAG, audience=Java 工程师
结果：请把 RAG 解释给 Java 工程师
```

## 🤔 解决什么问题

| 字符串拼接 / `String.format` | PromptTemplate |
| ---- | ---- |
| 容易漏空格、漏变量 | 变量显式 |
| 难复用 | 模板可沉淀 |
| 难测试 | 可单独渲染检查 |
| 容易把业务数据和提示词混在一起 | 结构更清楚 |

> [!tip] 为什么不用 `String.format("...%s...", var)`？三个硬伤
> 1. **顺序耦合**：占位符一多，参数顺序写反编译不报错，运行期 prompt 就错了；`{name}` 命名占位符顺序无关、一眼可读。
> 2. **`%` 冲突**：prompt 里若本身有"提升 50% 的性能"，`String.format` 会把 `%` 当占位符（抛异常或输出错乱，要 `%%` 转义）；`{var}` 模板无此问题。
> 3. **String.format 只能拼字符串**，`PromptTemplate.create()` 返回的是 **`Prompt` 消息对象**——能生成多条消息（system+user+历史）、能与工具/记忆/结构化输出集成（这些组件消费 Prompt 而非 String）。
>
> 另外两点工程价值：模板可放 `resources/prompts/*.st` 与代码分离（改话术不重新编译）；模板加载时能提前发现占位符缺失/多余（String.format 运行时才炸）。
> **String.format 何时够用**：一次性、不维护的简单拼接（日志、调试）；"prompt 是产品内容"的场景一律用 PromptTemplate——它换来的不是"能占位"，而是"prompt 工程化"。

---

## 🧩 基础用法

```java
PromptTemplate template = new PromptTemplate("""
你是一个 Java AI 应用导师。
请用 {style} 的方式解释：{topic}
""");

Prompt prompt = template.create(Map.of(
        "style", "简洁、带代码例子",
        "topic", "Spring AI 的 ChatClient"
));

ChatResponse response = chatModel.call(prompt);
```

也可以先渲染，再交给 `ChatClient`：

```java
String userPrompt = new PromptTemplate("请总结下面文本：{content}")
        .render(Map.of("content", article));

String summary = chatClient.prompt()
        .user(userPrompt)
        .call()
        .content();
```

---

## ⚙️ 模板放在哪里

| 放法 | 适合场景 | 注意 |
| ---- | ---- | ---- |
| Java 字符串 | 很短、变化少 | 不适合长提示词 |
| `resources/prompts/*.st` | 长模板、可维护 | 便于版本管理 |
| 数据库 / 配置中心 | 运营可调 Prompt | 需要审批和灰度 |

## 🧠 Prompt 分层

```text
System Prompt：角色、边界、输出规范
User Prompt：本次任务和输入数据
Few-shot：示例
Context：检索结果、历史摘要、工具返回
```

`PromptTemplate` 最常用于 User Prompt 和复杂 System Prompt。不要把所有内容都塞进一个巨型模板。

---

## ✅ 学习检查点

- [ ] 能用变量渲染一个 Prompt
- [ ] 能区分 System Prompt 和 User Prompt
- [ ] 能把 RAG 检索结果放进模板
- [ ] 能解释为什么不要用字符串拼接管理复杂 Prompt

## ⚠️ 常见坑

- [ ] **变量名不一致**：模板里 `{question}`，代码里传 `query`，运行时才暴露。
- [ ] **模板过长**：越长越难测，优先拆成角色、任务、上下文、输出格式。
- [ ] **把 Prompt 当代码常量乱放**：长提示词建议独立文件，方便 review。
- [ ] **缺少输出约束**：只告诉模型做什么，不告诉它怎么输出，后处理会很痛苦。

## 🎯 面试速记

> - **Q: PromptTemplate 解决什么问题？**
>   - A: 解决提示词参数化、复用、测试和维护问题，避免业务代码里到处拼字符串。
> - **Q: RAG 中 PromptTemplate 常放什么？**
>   - A: 用户问题、检索文档、引用约束、输出格式。

## 🔗 关联阅读

- [[02-ChatClient与ChatModel]]
- [[09-VectorStore向量存储]]
- [[07-Advisor拦截器]]
- [[10-AI应用开发/02-框架-SpringAI与LangChain4j/_index]]

## 📚 参考资料

- [Spring AI Prompts](https://docs.spring.io/spring-ai/reference/api/prompt.html)


