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

本节学习用 `PromptTemplate` 管理和渲染提示词，前置是 [[02-ChatClient与ChatModel]] 中的调用方式。提示词写法见 [[../../01-LLM基础与Prompt/07-Prompt工程基础|Prompt 工程基础]]，检索结果如何注入模板见 [[09-VectorStore向量存储]]。

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

> [!tip] 为什么使用 PromptTemplate？
> `{name}` 这样的具名占位符不用依赖参数顺序，模板也可以单独渲染和测试。`create()` 能生成 `Prompt`；需要多个角色的消息时，仍要分别创建并组装。相比之下，`String.format` 中的百分号需要按格式规则转义。

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
| Java 字符串 | 很短、变化少 | 修改需要重新发布应用 |
| `resources/prompts/*.st` | 长模板、随代码管理 | 便于代码审查和版本管理 |
| 数据库 / 配置中心 | 多租户或需要频繁调整 | 需要权限、审批、灰度和版本管理 |

`PromptTemplate` 负责渲染，模板可以从文件、数据库或配置中心读取。假设 `templateService` 返回包含 `{company}` 和 `{question}` 的模板，下面只展示读取后的渲染过程：

```java
String text = templateService.get("customer_service", tenantId, version);
Prompt prompt = new PromptTemplate(text)
        .create(Map.of("company", company, "question", question));
```

外部模板可按场景、租户和版本管理，并根据需要缓存；同时要考虑配置缺失时如何降级或报错。

## 🧠 Prompt 分层

```text
System Prompt：角色、边界、输出规范
User Prompt：本次任务和输入数据
Few-shot：示例
Context：检索结果、历史摘要、工具返回
```

`PromptTemplate` 最常用于 User Prompt 和复杂 System Prompt。不要把所有内容都塞进一个巨型模板。

---

## 🚀 进阶用法（不止变量替换）

### 1. ChatClient 内联模板（日常最高频）

```java
// s 是 SystemSpec(系统消息构建器), u 是 UserSpec(用户消息构建器)
// 它们是 .system()/.user() 传入的 Consumer 回调参数——作用域隔离，
// system 和 user 可以各自声明 {company} 等占位符而不冲突
String answer = chatClient.prompt()
        .system(s -> s.text("你是 {company} 的客服，语气 {tone}")
                .param("company", c).param("tone", t))
        .user(u -> u.text("用户问题：{question}").param("question", q))
        .call()
        .content();
// 每条消息各自维护模板和参数，互不干扰
```

### 2. 从 Java 对象提供变量

下面先取出 record 的字段，再传给模板；字段访问方式不依赖模板引擎对 Java record 的适配。

```java
record User(String name, String city) {}
User user = new User("小明", "上海");
Prompt prompt = new PromptTemplate("用户 {name} 来自 {city}")
        .create(Map.of("name", user.name(), "city", user.city()));
```

### 3. 把不同角色组成 Prompt

`Prompt` 能包含多条不同角色的消息，但一次 `PromptTemplate.create()` 不会把普通字符串自动识别成多角色对话；需要分别创建消息并组装。下面是片段，省略 imports。

```java
Message system = new SystemMessage("你是订单助手。");
Message user = new PromptTemplate("查询订单 {id}").createMessage(Map.of("id", "A123"));
Prompt prompt = new Prompt(List.of(system, user));
```

### 4. 动态选择模板（结合配置中心做灰度）

```java
String key = user.isVip() ? "vip-greeting" : "normal-greeting";
String template = templateService.get(key);          // 从 DB 或配置中心加载
Prompt prompt = new PromptTemplate(template).create(vars);
```

### 5. 其他工程实践

- **模板组合**：先渲染子模块文本，再作为变量注入主模板。
- **结构化输出提示**：结合 `BeanOutputConverter` 生成的格式说明注入模板。
- **RAG 与记忆注入**：将检索到的文档片段与历史摘要作为上下文变量填入模板。
- **模板单元测试**：在单元测试中直接调用 `template.render()` 校验渲染结果，无需发起真实模型调用。

> [!note] 模板语法边界
> Spring AI 默认基于 StringTemplate（ST4）实现变量替换，主要面向静态文本占位填充。复杂的条件分支、循环过滤或数据格式化建议在 Java 业务代码中处理完毕后，再作为标准变量传入模板。

---

## ✅ 学习检查点

- [ ] 能用变量渲染一个 Prompt
- [ ] 能区分 System Prompt 和 User Prompt
- [ ] 能把 RAG 检索结果放进模板
- [ ] 能解释为什么不要用字符串拼接管理复杂 Prompt

## ⚠️ 常见坑

- 模板变量名要和传入的键一致，例如 `{question}` 对应 `question`。
- 过长的模板难以维护，可以按角色、任务、上下文和输出要求组织。
- 长提示词可以放入独立文件，便于审查和版本管理。
- 需要程序处理结果时，应说明输出格式并验证返回值。

## 🎯 面试速记

> [!question]- PromptTemplate 解决什么问题？
> 解决提示词参数化、复用、测试和维护问题，避免业务代码里到处拼字符串。

> [!question]- RAG 中 PromptTemplate 常放什么？
> 用户问题、检索文档、引用约束、输出格式。

## 🔗 关联阅读

- [[02-ChatClient与ChatModel]]
- [[09-VectorStore向量存储]]
- [[07-Advisor拦截器]]
- [[../_index]]

## 📚 参考资料

- [Spring AI Prompts](https://docs.spring.io/spring-ai/reference/api/prompt.html)
