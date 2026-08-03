---
title: Codex执行模型-线程-沙箱-审批
tags: [领域/AI应用, 类型/知识, 主题/Codex, 主题/Agent安全]
created: 2026-07-08
updated: 2026-07-08
status: 已完成
mastery: ⭐☆☆☆☆
---

# Codex 执行模型：线程、沙箱、审批

> [!abstract] 一句话总结
> Codex 的执行模型可以理解为：在线程里规划和行动，在沙箱里运行工具，对越界或高风险动作触发审批。

## Thread：任务状态容器

Codex 以 thread 承载一次持续任务。一个 thread 里包含：

- 用户目标和约束。
- 已读文件和命令输出。
- 修改过的文件。
- 测试结果。
- 中途决策。
- 最终总结。

这解决了编程任务的长链路问题：不是一次性生成代码，而是在“读、改、跑、修、交付”的循环中推进。

## Local / Worktree / Cloud

| 模式 | 含义 | 适合 |
| ---- | ---- | ---- |
| Local | 直接在当前项目目录工作 | 小改动、用户想立即看到结果 |
| Worktree | 在 Git worktree 中隔离修改 | 并行任务、实验性改动 |
| Cloud | 在远程环境执行 | 后台任务、云端依赖、异步工作 |

Worktree 是成功产品的关键设计。它把“并行 Agent”从概念变成可控工程实践：每个任务有自己的工作副本，冲突更容易管理。

## Sandbox：技术边界

Codex 官方手册强调 sandbox mode 控制模型生成命令能做什么，例如：

- 能否写文件。
- 可写范围在哪里。
- 是否能访问网络。
- 是否能碰工作区外文件。

这对应企业 Agent 里的“最小权限原则”。Agent 不是因为聪明就可信，它必须被运行环境约束。

## Approval：人工决策边界

Approval policy 决定 Codex 什么时候必须停下来问用户，例如：

- 需要访问网络。
- 需要写工作区外文件。
- 需要执行高风险命令。
- MCP 或 app tool 声明有副作用。

审批不是体验负担，而是风险分层：低风险动作自动做，高风险动作让用户确认。

## 执行循环

```mermaid
flowchart TD
    A[用户目标] --> B[读取上下文]
    B --> C[形成计划]
    C --> D[修改文件/运行命令]
    D --> E{是否越过沙箱或高风险}
    E -->|否| F[继续执行]
    E -->|是| G[请求审批]
    G --> H{用户同意}
    H -->|是| F
    H -->|否| I[调整方案]
    F --> J[运行验证]
    J --> K{是否通过}
    K -->|否| C
    K -->|是| L[总结与交付]
```

## 对企业 Agent 的启发

Codex 的 sandbox / approval 可以抽象成企业 Agent 的三级控制：

| 层级 | Codex 里的体现 | 企业 Agent 类比 |
| ---- | -------------- | --------------- |
| 技术限制 | sandbox | 只能访问授权数据和工具 |
| 操作审批 | approval | 退款、发券、改订单前人工确认 |
| 结果验证 | tests / command output | 业务规则校验、回归测试、人工抽查 |

## 常见误区

- 误区一：Agent 越自动越好。实际应按风险分级。
- 误区二：Prompt 能代替权限。实际必须靠运行环境和服务端判断。
- 误区三：一次生成就是完成。实际要有验证循环。
- 误区四：并行 Agent 可以随便改同一目录。实际要靠 worktree 或明确写范围隔离。

## 面试讲法

> Codex 的执行模型给我的启发是：生产 Agent 必须有任务状态、工具边界和审批机制。线程负责持续上下文，sandbox 负责技术限制，approval 负责高风险动作的人类确认，验证循环负责交付质量。

## 关联

- [[../../05-Agent-MCP-Skills/21-Agent权限治理与分级自治]]
- [[../../06-平台与工程化/09-AI应用评估数据集与回归测试]]

## 📚 参考资料

- [OpenAI Codex Manual：Approvals, Sandboxing, and Security](https://developers.openai.com/codex/codex-manual.md#approvals-sandboxing-and-security)
- [OpenAI Codex Manual：Codex app features](https://developers.openai.com/codex/codex-manual.md#surface-behavior)

