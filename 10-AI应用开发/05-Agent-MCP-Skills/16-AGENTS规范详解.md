---
title: AGENTS.md 规范详解
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/AGENTS
  - 阶段/05
created: 2026-05-31
updated: 2026-05-31
status: 已完成
mastery: ⭐☆☆☆☆
---

# 📄 AGENTS.md 规范详解

> [!abstract] 一句话总结
> AGENTS.md 是放在项目根目录、**给编程 Agent（Cursor / Claude Code / Codex CLI / 通义灵码）看的项目说明书**。vendor-neutral 开放标准，告诉 AI 怎么 build、怎么 test、有什么规范和边界。

## 📌 这是什么

一个项目根目录的 Markdown 文件，专门承载「AI 编程助手需要知道、但不适合放进给人看的 README」的信息。

## 🤔 解决什么问题

> AI 编程助手不了解你的项目约定时，会用错构建命令、违反代码规范、改不该改的文件。AGENTS.md 把这些"操作性上下文"集中告诉它。

| 文件 | 给谁看 | 内容 |
| ---- | ------ | ---- |
| **README.md** | 人类开发者 | 项目介绍、功能、安装使用 |
| **AGENTS.md** | AI 编程 Agent | build/test 命令、规范、边界 |

两者并存，各司其职。

---

## 🧩 典型内容

```markdown
# AGENTS.md

## 构建命令
- `mvn clean package` 构建
- `mvn test` 跑所有测试
- `mvn spring-boot:run` 本地启动

## 代码规范
- Java 17，遵循阿里巴巴 Java 规约
- 新增接口必须写单元测试
- 日志用 SLF4J，禁止 System.out

## 项目结构
- controller/ 只做参数校验和路由
- service/ 业务逻辑
- 不要在 controller 直接写 SQL

## 边界（重要）
- `src/main/resources/legacy/` 目录不要改
- 数据库 schema 改动需先评审
- 不要提交 application-prod.yml
```

---

## ⚙️ 怎么用

```text
☑ 放在项目根目录，命名 AGENTS.md
☑ Cursor / Claude Code / Codex CLI / 通义灵码 会自动读取
☑ 多模块项目可以每个子模块放一个
☑ 跟着代码一起进 Git，团队共享
```

---

## 🆚 与 SKILL.md 的区别

| 维度 | AGENTS.md | SKILL.md |
| ---- | --------- | -------- |
| 作用域 | 整个代码项目 | 某一类任务 |
| 给谁 | 编程 Agent | 任意 Agent |
| 内容 | 项目的操作约定 | 做某事的流程 |
| 位置 | 项目根目录 | Skills 文件夹 |

---

## ⚠️ 踩坑与注意事项

- [ ] **别写成第二个 README**：只写 AI 需要的操作性信息
- [ ] **边界要明确**：哪些不能改一定要写清楚
- [ ] **保持更新**：构建命令变了要同步改

---

## 🎯 面试问题

> [!question] 高频问题
> - **Q: AGENTS.md 和 README.md 的区别？**
>   - A: README 给人看（项目介绍），AGENTS.md 给 AI 编程 Agent 看（build/test 命令、代码规范、修改边界）。
> - **Q: AGENTS.md 解决什么问题？**
>   - A: 让 AI 编程助手了解项目约定，避免用错命令、违反规范、改错文件。

---

## 🔗 关联笔记

- [[13-Skills详解]] —— SKILL.md 是另一个 Markdown 标准
- [[17-Agent-MCP-Skills组合实战]]
- [[_index]]

## 📚 参考资料

- [AGENTS.md 开放标准](https://agents.md/)
