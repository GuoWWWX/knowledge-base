---
title: Embabel Agent Action、Goal、Condition 核心注解
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/Agent
  - 主题/EmbabelAgent
  - 阶段/02
created: 2026-08-14
updated: 2026-08-14
status: 已完成
mastery: ⭐☆☆☆☆
---

# Embabel Agent Action、Goal、Condition 核心注解

> [!abstract] 一句话总结
> `@Action` 定义系统允许执行的状态变换，`@AchievesGoal` 定义何时可以结束，`@Condition` 补充类型之外的布尔约束；类型依赖应优先，显式条件只表达类型无法表达的事实。

## 1. 三者分别回答什么

| 注解 | 回答的问题 | 典型例子 |
| ---- | ---------- | -------- |
| `@Action` | 系统会做什么，输入和产出是什么？ | 根据需求生成草稿 |
| `@AchievesGoal` | 执行到哪里算完成？ | 返回经过审核的学习计划 |
| `@Condition` | 除了类型存在，还必须满足什么？ | 审核结果必须通过 |

三者的因果关系：

```text
Goal 给出终点
  → Planner 反查能实现 Goal 的 Action
  → Action 的输入类型和 pre 决定是否可执行
  → Action 的返回类型和 post 改变世界状态
  → Condition 重新求值
  → Goal 满足后终止
```

## 2. `@Agent`

```java
@Agent(
        name = "learning-plan-agent",
        description = "Create and review a structured learning plan",
        version = "1.0.0"
)
public class LearningPlanAgent {
}
```

常用属性：

| 属性 | 用途 | 建议 |
| ---- | ---- | ---- |
| `description` | 文档、Agent 选择 | 必填，写清能完成的用户目标 |
| `name` | 稳定标识 | 多 Agent 项目显式填写 |
| `version` | Agent 版本 | 生产跟踪时填写 |
| `planner` | 规划器 | 默认 GOAP，先不要改 |
| `scan` | 是否被扫描 | 测试夹具可设 `false` |
| `opaque` | 是否隐藏内部动作/条件 | 组合和暴露边界时使用 |
| `actionRetryPolicy*` | Agent 级 Action 重试策略 | 先理解幂等后再配置 |

`description` 不要写成“学习 Agent”这种空话。Closed 模式要在多个 Agent 之间选择，描述应包含输入和可交付结果。

## 3. `@Action`

### 最小 Action

```java
@Action
public DraftPlan draft(LearningRequest request) {
    return new DraftPlan(request, List.of());
}
```

方法签名已经隐式声明：

```text
需要 LearningRequest
产生 DraftPlan
```

### 常用属性

```java
@Action(
        description = "Review whether the draft covers every learning day",
        pre = {"draftReady"},
        post = {"planReviewed"},
        readOnly = true,
        canRerun = false,
        cost = 0.2,
        value = 0.8
)
public ReviewedPlan review(DraftPlan draft) {
    // ...
}
```

| 属性 | 真实含义 | 注意 |
| ---- | -------- | ---- |
| `description` | 日志、文档及部分 Planner 可见描述 | 应描述动作，不是目标 |
| `pre` | 类型之外的前置条件 | 条件名或 `spel:` 表达式 |
| `post` | 动作后可能成立、需要重算的条件 | 不等于无条件写入 `true` |
| `readOnly` | 无外部副作用 | 只分析和派生对象时设 `true` |
| `canRerun` | 同一 Process 中是否允许再次执行 | 开启前确认幂等和终止条件 |
| `clearBlackboard` | 完成后清旧状态，只留输出 | 循环或明确阶段切换才用 |
| `cost` / `value` | 0～1 的静态成本/价值 | 对 Utility/路径选择更重要 |
| `costMethod` / `valueMethod` | 引用 `@Cost` 动态计算 | 运行时状态相关时使用 |
| `trigger` | 最后新增特定类型时才触发 | 事件式行为，比复杂条件更直接 |
| `actionRetryPolicy*` | 本 Action 重试策略 | 有副作用的 Action 必须谨慎 |

### Action 必须是业务原子步骤

过大：

```java
@Action
public FinalReport doEverything(UserInput input) {
    // 理解需求、查资料、生成、审核、发送邮件全写在一起
}
```

问题是 Planner 无法重用中间能力，也无法在失败后从已有状态继续。

过小：

```java
@Action String trim(String text) { ... }
@Action String addPeriod(String text) { ... }
```

问题是规划图充满无业务意义的细节，增加搜索和观测噪声。

合适的 Action 通常代表一个可独立测试、产出明确领域事实的业务步骤。

## 4. `@AchievesGoal`

```java
@AchievesGoal(
        description = "Return a concrete learning plan approved by review",
        value = 0.9,
        tags = {"learning", "planning"},
        examples = {
                "Create a 7-day Embabel learning plan",
                "Plan two weeks of JVM study"
        }
)
@Action
public LearningPlan finish(ApprovedPlan approvedPlan) {
    return new LearningPlan(
            approvedPlan.topic(),
            approvedPlan.tasks());
}
```

它不是单独的 `@Goal` 方法，而是贴在某个 `@Action` 上：完成这个 Action，就产出对应目标。

Goal 描述尤其重要，因为 Open/Closed 选择可能基于用户意图匹配目标。好的描述写“达成什么结果”，而不是“调用 finish 方法”。

### 为什么不能只有最后一个 Action

如果没有 `@AchievesGoal`：

- GOAP 不知道终点。
- `AgentInvocation.create(platform, LearningPlan.class)` 找不到能产生该目标类型的 Agent。
- Process 可能无法判断何时结束。

## 5. `@Condition`

类型只能表达“审核结果存在”，不能表达“审核通过”。这时才需要条件：

```java
@Condition(name = "planApproved")
public boolean planApproved(ReviewedPlan reviewedPlan) {
    return reviewedPlan.approved();
}

@AchievesGoal(description = "Return an approved learning plan")
@Action(pre = {"planApproved"})
public LearningPlan finish(ReviewedPlan reviewedPlan) {
    return new LearningPlan(
            reviewedPlan.draft().request().topic(),
            reviewedPlan.draft().tasks());
}
```

条件方法的领域参数也来自 Blackboard。`ReviewedPlan` 不存在时，`planApproved` 不能成立；存在后框架才能求值。

条件方法必须：

- 无副作用。
- 可重复调用。
- 只判断事实，不执行补救动作。
- 结果稳定地取决于传入状态。

不要在 `@Condition` 中写数据库更新、发消息或修改 Blackboard，因为 Planner 可能多次求值。

## 6. `pre` 与 `post` 的正确理解

```java
@Action(post = {"planApproved"})
public ReviewedPlan review(DraftPlan draft) {
    // ...
}
```

`post` 告诉 Planner：这个 Action 执行后，`planApproved` 这个条件可能获得求值所需的新状态，应该重新判断。它不是把条件强行设置为真。

```java
@Action(pre = {"planApproved"})
public LearningPlan finish(ReviewedPlan reviewed) {
    // ...
}
```

`pre` 表示：即使输入类型 `ReviewedPlan` 已存在，条件为真之前也不能执行。

一个可达路径必须同时满足：

```text
输入类型可获得
AND pre 条件可满足
AND Action 能产生后续需要的类型/条件
```

如果写了 `pre = "planApproved"`，却没有任何 `@Condition(name = "planApproved")` 或其他方式建立它，Goal 会不可达。

## 7. SpEL 条件

简单属性判断可以直接写：

```java
@Action(pre = {"spel:reviewedPlan.approved"})
public LearningPlan finish(ReviewedPlan reviewedPlan) {
    // ...
}
```

选择建议：

| 场景 | 用法 |
| ---- | ---- |
| 简单字段判断，只用一次 | `spel:` |
| 逻辑复杂或多处复用 | `@Condition` |
| 最后新增某个类型才触发 | `trigger` |
| 可以用新领域类型表达 | 优先用类型，不写条件 |

例如与其用布尔条件表达“已审核”，更强的建模是让审核 Action 产出 `ApprovedPlan`；类型本身就证明审核阶段完成。

## 8. `cost`、`value` 与 `@Cost`

两条路径都能到 Goal 时，GOAP 倾向选择成本更低的路径：

```java
@Action(cost = 0.1)
public RepositoryInfo readLocalMetadata(ProjectPath path) { /* ... */ }

@Action(cost = 0.8)
public RepositoryInfo askLargeModel(ProjectPath path) { /* ... */ }
```

成本随状态变化时：

```java
@Cost(name = "draftReviewCost")
public double draftReviewCost(@Nullable DraftPlan draft) {
    if (draft == null) {
        return 0.1;
    }
    return draft.tasks().size() > 30 ? 0.9 : 0.2;
}

@Action(costMethod = "draftReviewCost")
public ReviewedPlan review(DraftPlan draft) {
    // ...
}
```

`@Cost` 在规划阶段执行，领域参数必须可空，因为对应对象可能还没进入 Blackboard。返回值必须在 `0.0` 到 `1.0` 之间。

## 9. 副作用、重试与幂等

```java
@Action(readOnly = false, canRerun = true)
public EmailSent sendEmail(ApprovedPlan plan) {
    // 危险：重试或重新规划可能重复发送
}
```

在打开 `canRerun` 或重试前，先回答：

1. 同一个 Process 重复调用会不会重复扣款、发信、提交代码？
2. 是否有业务幂等键，例如 `processId + actionName`？
3. 失败发生在外部系统成功之后、本地返回之前怎么办？
4. 是否需要补偿 Action？

`readOnly` 是元数据，不会自动阻止副作用。它要求开发者如实建模。

## 10. 完整小例子

```java
@Agent(description = "Create a reviewed learning plan")
public class LearningPlanAgent {

    record Request(String topic, int days) {}
    record Draft(Request request, List<String> tasks) {}
    record Review(Draft draft, boolean approved) {}
    record Plan(String topic, List<String> tasks) {}

    @Action(readOnly = true)
    public Request understand(UserInput input) {
        return new Request(input.getContent(), 7);
    }

    @Action(readOnly = true, post = {"approved"})
    public Review review(Draft draft) {
        return new Review(draft, draft.tasks().size() == draft.request().days());
    }

    @Condition(name = "approved")
    public boolean approved(Review review) {
        return review.approved();
    }

    @AchievesGoal(description = "Return an approved learning plan")
    @Action(pre = {"approved"}, readOnly = true)
    public Plan finish(Review review) {
        return new Plan(review.draft().request().topic(), review.draft().tasks());
    }
}
```

这个片段故意省略了产生 `Draft` 的 Action。请先判断：Goal 为什么不可达？答案不是“模型不够聪明”，而是规划图缺少 `Request → Draft` 的边。

## 最小验证

1. 给小例子补上 `draft(Request)`。
2. 让任务数等于天数，确认能到 `Plan`。
3. 让任务数少一个，观察 Process 为什么停在 `Review`。
4. 删除 `pre = "approved"` 再运行，比较“有审核对象”和“审核通过”的区别。

## 自测题

> [!question]- Q1：输入类型已经存在，为什么 Action 仍可能不能执行？
> 因为它还可能声明 `pre` 条件、trigger，或已执行且 `canRerun=false`。输入类型只是前置条件的一部分。

> [!question]- Q2：`post = "approved"` 是否会把 approved 直接设为 true？
> 不会。它表示 Action 后该条件可能可以重新求值，真实值仍由 `@Condition` 或表达式根据当前状态计算。

> [!question]- Q3：何时应把 Condition 改成领域类型？
> 当某个布尔事实代表稳定的业务阶段，例如“已审批”“已验证”时，可以产出 `ApprovedPlan`、`VerifiedPatch` 等类型，让类型本身成为证明。

## 关联阅读

- 上一篇：[[03-领域对象与Blackboard状态模型]]
- 下一篇：[[05-规划器与三种执行模式]]
- Agent 模式：[[../../05-Agent-MCP-Skills/01-Agent设计模式详解]]
- 子区索引：[[_index]]

## 参考资料

- [Embabel Annotation Model](https://docs.embabel.com/embabel-agent/guide/1.5.0-SNAPSHOT/#reference.annotations)
- [Embabel Planners](https://docs.embabel.com/embabel-agent/guide/1.5.0-SNAPSHOT/#reference.planners)

