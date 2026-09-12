---
title: Embabel Agent 环境搭建与第一个 Agent
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

# Embabel Agent 环境搭建与第一个 Agent

> [!abstract] 一句话总结
> 一个最小 Embabel 应用就是普通 Spring Boot 应用加 Embabel Platform、模型 Provider 和一个带 `@Agent`、`@Action`、`@AchievesGoal` 的 Spring Bean；先用 Shell 跑通“输入类型 → Action → 目标类型”，再学习复杂规划。

## 本节目标

学完后你应该能：

1. 新建一个 Embabel `1.5.0` Java 工程。
2. 解释三个 Starter 各自负责什么。
3. 编写并运行只有一个 Action 的 Agent。
4. 在 Shell 中确认 Agent、Action、Goal 都已注册。
5. 识别 `0.3.x` 旧教程与 `1.5.0` API 的差异。

## 1. 环境要求

| 工具 | 要求 | 检查命令 |
| ---- | ---- | -------- |
| JDK | Java 21+ | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| 模型密钥 | OpenAI、Anthropic、Google 等至少一个 | 检查对应环境变量 |
| Embabel Agent | 本章固定 `1.5.0` | 看 `pom.xml` |

Windows PowerShell 中检查：

```powershell
java -version
mvn -version
$env:OPENAI_API_KEY
```

不要把密钥写死在 Git 管理的 `application.yml` 中。本地可以使用环境变量，部署时交给密钥管理系统。

## 2. 创建 Maven 工程

目录结构：

```text
embabel-learning/
├── pom.xml
└── src/
    └── main/
        ├── java/com/example/learning/
        │   ├── LearningAgentApplication.java
        │   └── LearningPlanAgent.java
        └── resources/
            └── application.yml
```

### `pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>4.1.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>embabel-learning</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <properties>
        <java.version>21</java.version>
        <embabel-agent.version>1.5.0</embabel-agent.version>
    </properties>

    <dependencies>
        <!-- AgentPlatform、注解模型和自动配置 -->
        <dependency>
            <groupId>com.embabel.agent</groupId>
            <artifactId>embabel-agent-starter</artifactId>
            <version>${embabel-agent.version}</version>
        </dependency>

        <!-- OpenAI 模型注册；换 Provider 时替换这个 Starter -->
        <dependency>
            <groupId>com.embabel.agent</groupId>
            <artifactId>embabel-agent-starter-openai</artifactId>
            <version>${embabel-agent.version}</version>
        </dependency>

        <!-- 学习阶段用交互式 Shell 查看 Agent、Action、Goal 和执行轨迹 -->
        <dependency>
            <groupId>com.embabel.agent</groupId>
            <artifactId>embabel-agent-starter-shell</artifactId>
            <version>${embabel-agent.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <configuration>
                    <!-- Embabel 会读取 Action 参数名作为 Blackboard binding -->
                    <parameters>true</parameters>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

三个依赖不是重复的：

| 依赖 | 作用 | 能否省略 |
| ---- | ---- | -------- |
| `embabel-agent-starter` | 创建 `AgentPlatform`，扫描并注册 Agent | 不能；但 Shell Starter 会传递引入它 |
| `embabel-agent-starter-openai` | 把 OpenAI 模型注册到 Embabel 的 ModelProvider | 不用 OpenAI 时替换为其他 Provider Starter |
| `embabel-agent-starter-shell` | 提供 `x`、`agents`、`actions`、`goals` 等命令 | 正式 Web 服务可以省略 |

> [!warning] 不要照抄旧模板版本
> 截至 2026-08-14，官方 `java-agent-template` 主分支仍可能使用 Embabel `0.3.x` 和 Spring Boot `3.5.x`。学习本章时固定 `1.5.0`，依赖版本以 Maven Central 和 `v1.5.0` 源码为准。

## 3. 启动类

Embabel `1.5.0` 不要求在启动类上再加一个神秘的启用注解。普通 Spring Boot 启动类即可：

```java
package com.example.learning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class LearningAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LearningAgentApplication.class, args);
    }
}
```

因果链是：

```text
Spring Boot 启动
  → Starter 自动配置 AgentPlatform
  → 组件扫描发现 @Agent Bean
  → 读取 @Action/@AchievesGoal 元数据
  → 注册 Agent、Action 和 Goal
```

所以 Agent 类必须在启动类所在包或其子包下，除非你显式调整 Spring 的扫描范围。

## 4. 第一个单 Action Agent

先不引入 LLM，避免把“框架没注册”和“模型调用失败”混在一起。

```java
package com.example.learning;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.domain.io.UserInput;

@Agent(description = "Create a first learning plan from user input")
public class LearningPlanAgent {

    public record LearningPlan(String topic, int days) {}

    @AchievesGoal(description = "Return a usable learning plan")
    @Action(readOnly = true)
    public LearningPlan createPlan(UserInput input) {
        return new LearningPlan(input.getContent(), 7);
    }
}
```

逐行理解：

- `@Agent` 既是 Embabel 注解，也是 Spring stereotype，因此该类会成为 Spring Bean。
- `description` 不只是注释。Closed/Open 模式可能用它帮助选择 Agent。
- `UserInput` 是起点类型，Shell 会把用户文本包装成这个对象。
- `LearningPlan` 是 Action 的输出类型，同时成为 Blackboard 中的新事实。
- `@AchievesGoal` 把该 Action 的完成标记为一个 Goal。
- `readOnly = true` 表示 Action 不修改文件、数据库或外部系统。

## 5. 最小配置与运行

`application.yml`：

```yaml
embabel:
  models:
    default-llm: gpt-4.1-mini
```

设置密钥并启动：

```powershell
$env:OPENAI_API_KEY = "你的密钥"
mvn spring-boot:run
```

进入 Shell 后依次执行：

```text
agents
actions
goals
x "我想用 7 天学习 Embabel Agent"
blackboard
```

预期现象：

1. `agents` 中出现 `LearningPlanAgent`。
2. `actions` 中出现 `createPlan`。
3. `goals` 中出现“Return a usable learning plan”。
4. `x` 返回 `LearningPlan`。
5. `blackboard` 能看到 `UserInput` 和 `LearningPlan`。

此时才算“框架跑通”。只看到 Spring Boot 启动成功，还不能证明 Agent 被扫描并注册。

## 6. 常见启动问题

### `agents` 为空

排查顺序：

1. Agent 是否有 `@Agent`。
2. 类是否在 Spring 扫描范围内。
3. `@Agent(scan = false)` 是否被误用。
4. Starter 是否确实进入运行时依赖。

### 找不到 Goal

检查目标方法是否同时有：

```java
@AchievesGoal(description = "...")
@Action
```

只有 `@Action` 表示“可执行步骤”，不表示“执行到这里任务就结束”。

### 找不到模型

本节的第一个 Agent 没用 LLM，理论上仍能运行。加入 LLM 后再检查：

- Provider Starter 是否存在。
- 环境变量是否在启动 Maven 的同一个终端中。
- `embabel.models.default-llm` 是否与已注册模型名称一致。

### Java 方法参数名丢失

复杂 Agent 启动时报 binding/parameter name 错误时，确认编译器开启了 `-parameters`。Embabel 需要参数名区分同类型对象和 Blackboard binding。

## 7. 新旧 API 对照

| 旧资料常见写法 | `1.5.0` 建议写法 | 说明 |
| -------------- | ---------------- | ---- |
| Embabel `0.3.x` | Embabel `1.5.0` | 不要混用依赖 |
| Boot `3.5.x` 模板 | Boot `4.1.0` 依赖线 | 以目标版本 POM 为准 |
| Action 参数直接用 `Ai` | `OperationContext` 或 `Ai` 都受支持 | 后续主线用 `OperationContext` 获取完整上下文 |
| 只看 README 示例 | 看标签源码、Reference、Maven POM | README 可能滞后 |
| 把所有 Starter 都加入 | 按应用形态和 Provider 选择 | 避免无关自动配置 |

## 最小验证

- [ ] `java -version` 为 21+。
- [ ] `mvn -version` 使用同一套 JDK 21+。
- [ ] `agents`、`actions`、`goals` 都能看到刚写的对象。
- [ ] `x "学习 Java"` 能返回 `LearningPlan`。
- [ ] 能口头解释为什么本例即使不调用 LLM 仍然是 Embabel Agent。

## 自测题

> [!question]- Q1：`embabel-agent-starter` 和模型 Provider Starter 有什么区别？
> 前者负责 AgentPlatform、扫描、规划和执行基础设施；后者负责把具体模型注册到平台。没有 Provider Starter，纯 Java Action 仍可能运行，但 LLM Action 无法选择模型。

> [!question]- Q2：为什么启动成功后还要执行 `agents/actions/goals`？
> Spring Boot 成功只证明容器启动；这些命令才能证明 Agent 元数据已被扫描并注册到 Embabel Platform。

> [!question]- Q3：为什么显式开启 `-parameters`？
> Action 的普通参数来自 Blackboard，参数名参与 binding 解析；编译后丢失参数名会让同类型对象匹配和命名绑定出现问题。

## 关联阅读

- 上一篇：[[01-Embabel-Agent概览与桌面Agent选型]]
- 下一篇：[[03-领域对象与Blackboard状态模型]]
- Spring 基础：[[../SpringAI/01-Spring-AI项目搭建]]
- 子区索引：[[_index]]

## 参考资料

- [Embabel Getting the Binaries](https://docs.embabel.com/embabel-agent/guide/1.5.0-SNAPSHOT/#getting-started.installing)
- [Embabel Getting Running](https://docs.embabel.com/embabel-agent/guide/1.5.0-SNAPSHOT/#getting-started.running)
- [Maven Central: embabel-agent-starter 1.5.0](https://repo1.maven.org/maven2/com/embabel/agent/embabel-agent-starter/1.5.0/)

