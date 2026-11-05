---
title: LangChain4j Agentic 工作流与共享状态
tags:
  - 领域/AI应用
  - 主题/LangChain4j
  - 主题/Agent
created: 2026-10-30
updated: 2026-11-01
status: 待学习
mastery:
---

# LangChain4j Agentic 工作流与共享状态

## 1. 现有 AiServices 之外，还可以学什么？

[[02-AiServices声明式AI服务]] 主要解释如何用 Java 接口声明一个 AI 服务。`langchain4j-agentic` 进一步提供把多个服务组织成工作流或 Agent 系统的抽象。例如写作流程可以先生成草稿，再交给编辑修改，最后由审阅步骤检查结果。

```text
输入主题 → 写作 Agent → 草稿 → 编辑 Agent → 修改稿 → 审阅结果
```

如果顺序由程序确定，这是顺序工作流；如果下一步由协调者根据任务或当前状态选择，就需要额外考虑选择失败、执行次数和终止条件。不是每个步骤都必须由大模型完成，固定规则也可以用普通 Java 代码执行。

## 2. AgenticScope 是什么？

`AgenticScope` 是参与同一 Agent 系统的步骤共享数据的容器。写作步骤把结果写入共享状态，编辑步骤再读取草稿；它还记录调用顺序等信息。它不等同于模型的 `ChatMemory`：前者用于工作流步骤之间共享数据，后者通常用于向模型提供某个会话的对话历史。

## 🗣️ 共享状态具体怎么传？

假设输入是 `topic = "春日旅行"`。写作 Agent 以 `topic` 生成草稿，并将输出保存到键 `story`；编辑 Agent 的输入声明读取 `story`，得到草稿后返回修改稿，供后续步骤使用。这里的键名是步骤间的约定，名称不一致时可能拿不到想要的数据。
共享状态是本次工作流的数据容器，不自动等于持久化数据库；模型生成的草稿也需要检查事实和格式。是否能跨请求恢复，应按所用版本和存储方案验证。

## 3. 从顺序工作流入手

官方示例通过 `AgenticServices.agentBuilder(...)` 构造各个 Agent，并用 `sequenceBuilder().subAgents(...)` 把它们串起来。先跟踪每个 Agent 的输入变量、`outputKey` 和 Scope 中的数据变化，再比较并行、循环和 Supervisor 等模式。下面假设 `creativeWriter`、`editor` 已创建并配置模型；片段只展示组装，省略接口、依赖和模型连接，未实际运行。

```java
UntypedAgent workflow = AgenticServices.sequenceBuilder()
        .subAgents(creativeWriter, editor)
        .outputKey("story")
        .build();
```

`outputKey` 决定组合工作流返回哪个状态；每个子 Agent 自己的输出键也要与后续输入变量匹配。运行失败时应区分是缺少输入、模型调用失败，还是输出字段不符合预期。

| 场景 | 先检查什么 |
| :--- | :--- |
| 顺序执行 | 上一步输出的键名是否与下一步输入匹配 |
| 并行执行 | 多个步骤是否会同时写同一个状态，失败如何汇总 |
| Supervisor 选择步骤 | 选择是否可解释，如何限制调用次数和退出 |
| 恢复执行 | 状态和会话标识是否已持久化，副作用能否安全重试 |

> [!warning] 模块成熟度
> 官方文档将整个 `langchain4j-agentic` 模块标记为**实验性**，API 可能变化，不能因为核心模块处于 1.x 就把 Agentic 当成稳定接口。生产使用应固定版本并验证升级。官方截至 2026-10-30 的发布页列出核心 1.23.0 与配套 1.23.0-beta33；具体模块的版本应以发布说明为准。

## 4. 近期可以继续看什么？

1.23.0 发布说明列出了新的 Agentic 模式、决策 Agent，以及测试模块中的模型评判和请求断言。先掌握顺序工作流与共享状态，再视项目需要研究这些能力；模型评判本身也有误判，需要与确定性断言和人工抽检配合。

## 官方资料

- [Agents and Agentic AI 教程](https://docs.langchain4j.dev/tutorials/agents/)
- [LangChain4j 发布记录](https://github.com/langchain4j/langchain4j/releases)
