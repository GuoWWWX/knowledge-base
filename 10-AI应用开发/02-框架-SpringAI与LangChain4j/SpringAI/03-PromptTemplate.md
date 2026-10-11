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

- **本章第 3 课，输入组织**。前置：[[02-ChatClient与ChatModel]] 的调用方式；上一章 [[../../01-LLM基础与Prompt/07-Prompt工程基础|Prompt 工程基础]] 讲“怎么写提示词”，本节讲“怎么在代码里模板化”。
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

> [!tip] 为什么推荐使用 `PromptTemplate` 而非 `String.format`？
> 1. **具名占位符解耦顺序**：`String.format` 依赖位置参数（`%s`），占位符较多时容易传错顺序；`PromptTemplate` 使用 `{name}` 具名占位符，与参数顺序无关且可读性更好。
> 2. **避免 `%` 符号转义冲突**：提示词文本中常包含百分比（如“提升 50%”），在 `String.format` 中未转义会抛出 `UnknownFormatConversionException`；`{var}` 语法不会产生此类冲突。
> 3. **直接生成结构化 `Prompt` 对象**：`String.format` 仅拼接字符串，而 `PromptTemplate.create()` 返回标准的 **`Prompt` 消息对象**，可直接承载多角色消息并与 `ChatClient`、工具调用及结构化输出组件衔接。
> 4. **支持外部化管理与静态校验**：模板可独立存放在 `resources/prompts/*.st` 或配置中心，实现提示词迭代与代码发布解耦，并在渲染阶段统一校验缺失变量。

> [!info] 提示词模板的存储方式选型
> | 存放方式 | 适用场景 | 工程特点 |
> | ---- | ---- | ---- |
> | 代码内字符串 / Text Block | 原型验证、简短固定指令 | 开发直接，修改需重新编译发版 |
> | `resources/prompts/*.st` 资源文件 | 中小型项目、长提示词版本化管理 | 随 Git 仓库做 Code Review，随应用打包发布 |
> | **数据库 / 配置中心（Nacos/Apollo）** | **多租户生产环境、高频运营调优** | 支持按租户定制、A/B 测试与热更新，无需重新发版 |
>
> **工程提示**：`PromptTemplate` 本身是**模板渲染组件**（将模板文本与变量合成为 `Prompt` 对象），与模板的存储位置正交。从数据库或配置中心读取模板字符串后，同样通过 `PromptTemplate` 渲染：
>
> ```java
> String templateText = templateService.get("customer_service", tenantId, version); // 从配置中心或 DB 读取
> Prompt prompt = new PromptTemplate(templateText)
>         .create(Map.of("company", c, "user", u, "question", q));
> ```
>
> 采用外部存储时，建议按 `场景标识 + 租户 + 版本号` 维度管理并配合本地缓存；同时在代码中保留内置默认模板作为降级兜底，防止因配置缺失导致调用失败。

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

### 2. 对象属性访问

```java
record User(String name, String city) {}

Prompt prompt = new PromptTemplate("用户 {user.name} 来自 {user.city}")
        .create(Map.of("user", new User("小明", "上海")));
// 支持通过属性路径直接读取对象字段，无需手动拆平为 Map
```

### 3. 多消息模板：渲染出消息序列

```java
Prompt prompt = new PromptTemplate(multiMessageTemplate).create(vars);
prompt.getInstructions();   // List<Message>：包含带角色的消息列表
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
- [[../_index]]

## 📚 参考资料

- [Spring AI Prompts](https://docs.spring.io/spring-ai/reference/api/prompt.html)


