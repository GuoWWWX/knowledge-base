---
title: Obsidian LLM Wiki
tags:
  - 领域/AI应用
  - 类型/项目
  - 主题/LLM-Wiki
created: 2026-07-04
updated: 2026-10-30
status: 规划中
mastery: ⭐☆☆☆☆
repo: 未绑定
---

# Obsidian LLM Wiki

> [!info] 项目简介
> 用 AI 辅助维护 Obsidian 知识库，实现笔记索引、双链建议、重复内容发现和学习复盘生成。

## 🎯 本节定位

本项目尝试用 AI 辅助维护知识库，建议先了解 [[../03-知识库技术-RAG/02-RAG工业级进阶与检索调优|LLM-Wiki 与文档直读]]。双链和标签建议可参考 [[../05-Agent-MCP-Skills/16-AGENTS规范详解|AGENTS 规范]]，由人审核后再应用。

## 📌 项目背景

Markdown 知识库长期积累后容易出现重复笔记、孤立笔记、标签混乱和过期内容。这个项目用 LLM 做辅助维护，目标是给出建议和报告，而不是自动覆盖用户原文。

## 🎯 项目目标

- [ ] 扫描本地 Markdown 知识库并生成结构化索引
- [ ] 为新笔记推荐双链和标签
- [ ] 发现重复、过期或孤立笔记
- [ ] 生成阶段学习复盘和下一步行动清单

## 🛠️ 技术栈

| 类别 | 选型 |
| ---- | ---- |
| 语言/框架 | Spring Boot 3 / Python 脚本 |
| 模型 | DeepSeek / 通义 / OpenAI 兼容接口 |
| 存储 | 本地 Markdown + SQLite |
| 向量库 | pgvector / 本地向量索引 |

## 🏗️ 架构设计

```mermaid
flowchart TD
    A[Obsidian Vault] --> B[Markdown Scanner]
    B --> C[Metadata Parser]
    C --> D[Index Builder]
    D --> E[LLM Analyzer]
    E --> F[双链建议]
    E --> G[重复内容报告]
    E --> H[学习复盘]
```

## 📋 实现步骤

- [ ] 扫描指定目录内的 Markdown 文件
- [ ] 解析 frontmatter、标题、标签和双链
- [ ] 生成笔记索引和孤立笔记列表
- [ ] 实现双链推荐 Prompt
- [ ] 实现重复内容检测和过期内容提示
- [ ] 生成复盘报告；选择独立输出路径，不覆盖已有笔记

## 🗣️ 一条建议怎么产生？

例如新增一篇“Spring AI 文档切分”，扫描器读取它的标题和现有双链，发现知识库已有“文本切分”笔记；分析器可输出候选链接、相似内容位置和推荐理由。用户确认内容有关系后，再自行决定是否添加链接或合并。
索引应保留相对路径，以区分不同目录下同名笔记；检测章节和块引用时，还要确认目标标题或块 ID 存在。相似度只能作为线索，不能单凭分数自动认定两篇笔记重复或已过期。

## 🧠 难点与关键技术点

- Markdown frontmatter、双链和相对路径解析要尽量保留原始语义。
- LLM 生成的双链建议只能作为候选，需要给出理由和置信度。
- 对本地 vault 的写入要默认保守，避免误改用户笔记。

## ✅ 验收标准

- 能扫描指定 vault 并输出笔记数量、孤立笔记、缺失 frontmatter 和坏链统计。
- 双链推荐要给出推荐理由，且不会直接覆盖原笔记。
- 重复内容报告包含相似笔记、相似原因和建议处理动作。
- 生成的复盘文档保留生成时间和输入范围；已有笔记不会被覆盖，目标文件已存在时应另选名称或停止写入。

## 🎯 面试讲法

> [!question]- 知识库维护助手如何发现并提出改进建议？
> 扫描 Markdown 生成索引，通过规则和相似度识别坏链、孤立笔记或可能重复的内容，再让模型生成双链建议与复盘报告。建议需人工审核，不直接覆盖原文。

## 🔗 关联笔记

- [[../03-知识库技术-RAG/02-RAG工业级进阶与检索调优]]
- [[../01-LLM基础与Prompt/07-Prompt工程基础]]
- [[../05-Agent-MCP-Skills/16-AGENTS规范详解]]
- [[../03-知识库技术-RAG/_index]]
- [[09-项目-CAG-vs-RAG对比实验]]
- [[_index|本区项目索引]]

## 📚 参考资料

- [Obsidian Help - Internal Links](https://help.obsidian.md/links)
- [CommonMark Spec](https://spec.commonmark.org/)
- [Spring AI Reference Documentation](https://docs.spring.io/spring-ai/reference/)
