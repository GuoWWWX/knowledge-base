---
title: Skills（SKILL.md）详解
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/Skills
  - 阶段/05
created: 2026-05-31
updated: 2026-11-01
status: 已完成
mastery: ⭐☆☆☆☆
---

# 📦 Skills（SKILL.md）详解

> [!abstract] 一句话总结
> Skill 把一类任务的操作指引和配套资源放进一个目录；支持 Skills 的宿主可以在需要时提供这些内容，让 Agent 复用做事的方法。

## 📌 为什么需要 Skill

假设每周都要整理周报：收集工作记录、区分完成与进行中、标出风险、按模板输出。如果每次都把要求重新发给 AI，既麻烦，也容易漏掉某一步。把这些要求写成 `weekly-report` Skill 后，支持 Skills 的宿主就可以在处理周报任务时使用这套指引。
Skill 适合封装重复使用的流程、检查项和模板。它仍然是给 Agent 的指引，不是写进去就一定会执行的程序，也不能凭空获得访问 Jira、Git 等系统的权限。

## 🧩 一个 Skill 里面有什么

一个 Skill 至少包含 `SKILL.md`；脚本、模板和参考资料按需添加：

```text
weekly-report/
├── SKILL.md                     # 必需：名称、描述与主要步骤
├── references/
│   └── report-template.md       # 可选：周报模板
└── scripts/                     # 可选：程序或脚本
```

`SKILL.md` 上半部分是 YAML 元数据，下半部分是 Markdown 指引。例如：

```markdown
---
name: weekly-report
description: 根据用户提供的工作记录整理周报，适用于生成周报草稿。
---

# 整理周报

1. 确认统计范围，读取用户提供或已授权的数据。
2. 按已完成、进行中、风险归类；缺失的信息标记待确认。
3. 参考 references/report-template.md 输出草稿，不编造内容。
```

按照 [Agent Skills 格式规范](https://agentskills.io/specification)，`name` 必填、长度为 1～64 个字符，须与目录名一致，只能使用小写字母、数字和连字符，连字符不能在开头、结尾或连续出现；`description` 必填且非空，最多 1024 个字符，应说明做什么、何时使用。上面的例子引用了模板，实际创建时还需要把模板文件放到对应位置；完整实作见 [[14-设计自己的Skill实战]]。

## ⚙️ Skill 是怎么被使用的

以支持渐进式加载的宿主为例，用户说“帮我整理周报”后，通常会经历下面几步：
1. 宿主发现可用 Skill，向模型提供名称和描述。
2. 模型或宿主判断 `weekly-report` 与任务有关，读取 `SKILL.md` 正文。
3. 如果流程需要，再读取模板、参考资料，或请求调用可用工具。
4. 宿主负责执行允许的工具调用，Agent 根据结果生成周报。

这里的“渐进式”是先提供简短信息、需要时再读取详情，有助于减少一次性放入上下文的内容；名称和描述本身仍会占用空间。**发现位置、自动选择、加载时机和上下文保留都由具体宿主决定**，不能认为所有产品实现完全一样。
例如 Claude Code 支持项目内 `.claude/skills/weekly-report/SKILL.md`，也支持通过 `/weekly-report` 显式调用；其他产品应查自己的安装和调用方式。只把文件放到任意目录，不代表当前 Agent 一定能发现它。

## 🆚 Skill、MCP 和其他文件

| 名称 | 主要作用 | 周报例子 |
| ---- | -------- | -------- |
| Skill / `SKILL.md` | 提供某类任务的流程与资源 | 规定如何整理工作记录、使用周报模板 |
| MCP | 规定宿主与外部能力通信的协议 | 通过 MCP Server 查询已授权的工单 |
| `AGENTS.md` | 向支持它的编程 Agent 提供项目约定 | 告知 Java 项目的测试命令和修改边界 |
| `README.md` | 主要介绍项目，供开发者使用 | 说明周报项目如何安装和启动 |

四者可以配合，也可以独立使用：Skill 告诉 Agent 如何整理周报，MCP 可提供查工单的工具；如果用户已经贴出全部记录，就不一定需要 MCP。`AGENTS.md` 也能被人阅读，`README.md` 也能被 Agent 阅读，区别主要在用途，而不是谁能打开文件。
类似地，LLM Wiki 更偏向组织“业务知识是什么”，Skill 更偏向整理“任务怎么做”；实际内容也可能交叉，相关知识见 [[../03-知识库技术-RAG/_index]]。

> [!warning] 指引不等于授权
> Skill 可以建议使用某个工具，但实际能否调用取决于宿主、工具配置和用户授权。外部 Skill 的指令和脚本也需要审查，不能因为它是 `SKILL.md` 就直接信任。

## 🧪 怎么检查一个 Skill 是否好用

先用明确的周报请求检查能否被发现和使用，再用相似但不需要生成周报的问题检查是否误触发。还要检查模板是否存在、记录不足时是否标记待确认，以及工具不可用时能否说明缺失的数据。输出像周报并不能单独证明 Skill 被加载；应结合宿主的 Skill 列表或执行记录判断。

## 🎯 自测

> [!question]- `SKILL.md` 和 Skill 是什么关系？
> Skill 是一个目录，`SKILL.md` 是其中必需的入口文件，包含名称、描述和任务指引；模板、脚本等是可选配套资源。

> [!question]- Skill 和 MCP 分别解决什么问题？
> Skill 提供任务指引；MCP 让宿主按协议连接工具和其他上下文。Skill 可以指导 Agent 使用 MCP 工具，但不能代替工具或授权。

> [!question]- 为什么不能只看最终周报判断 Skill 已加载？
> 模型即使没读取 Skill，也可能根据常识生成类似格式；需要查看宿主提供的加载或执行记录，并核对指引是否生效。

## 🔗 关联笔记

- [[01-Agent设计模式详解]] —— Agent 如何组织多步任务
- [[09-MCP协议详解]] —— MCP 提供哪些连接能力
- [[14-设计自己的Skill实战]] —— 创建并测试周报 Skill
- [[16-AGENTS规范详解]] —— 项目指引与 Skill 的区别
- [[17-Agent-MCP-Skills组合实战]] —— 三者如何协同
- [[../03-知识库技术-RAG/_index]] —— 知识库与知识组织
- [[../08-项目实战/07-项目-业务Agent系统]] —— Agent 项目实战
- [[_index]]

## 📚 参考资料

- [Agent Skills 格式规范](https://agentskills.io/specification)
- [Claude Code Skills 文档](https://code.claude.com/docs/en/skills)
- [Anthropic Skills 官方仓库](https://github.com/anthropics/skills)
- [Microsoft Agent Framework - Skills](https://learn.microsoft.com/en-us/agent-framework/agents/skills)
