---
title: 微调-LoRA-QLoRA-LLaMA-Factory入门
tags: [领域/AI应用, 类型/知识, 主题/微调, 主题/LLM]
created: 2026-07-05
updated: 2026-07-05
status: 已完成
mastery: ⭐☆☆☆☆
---

# 微调：LoRA / QLoRA / LLaMA-Factory 入门

> [!abstract] 一句话总结
> 应用开发岗不一定要会训练大模型，但必须能判断什么时候用 Prompt、RAG、工具调用，什么时候才值得微调。

## 先记结论

| 问题 | 优先方案 |
| ---- | -------- |
| 模型不知道企业知识 | RAG |
| 模型输出格式不稳定 | 结构化输出 + 校验 |
| 模型不会调用业务工具 | Tool Calling / Agent |
| 模型风格不符合要求 | Few-shot 或微调 |
| 大量稳定标注样本、任务模式固定 | 微调 |
| 想让模型记住频繁变化的数据 | 不要微调，用 RAG |

## LoRA 和 QLoRA 是什么

- **LoRA**：冻结原模型参数，只训练少量低秩适配器参数，成本低。
- **QLoRA**：在量化基础上做 LoRA，进一步降低显存需求。
- **LLaMA-Factory**：常用开源微调工具，支持多模型、多训练方式和 Web UI。

## 应用开发岗要掌握到什么程度

不要求手写训练框架，但要能说清：

1. 数据怎么准备：指令、输入、输出、拒答样例。
2. 什么时候微调：任务稳定、样本足够、Prompt/RAG 解决不了。
3. 怎么评估：固定 eval set，对比微调前后效果。
4. 怎么上线：模型版本、灰度、回滚、成本和延迟。
5. 风险是什么：过拟合、遗忘、安全能力下降、许可证问题。

## 数据格式示例

```json
{
  "instruction": "把客服工单归类到指定标签",
  "input": "用户反馈发票抬头开错，需要重新开票",
  "output": "发票问题"
}
```

## 微调与 RAG 的边界

```mermaid
flowchart TD
    A[问题] --> B{是知识缺失吗}
    B -->|是| C[RAG/知识库]
    B -->|否| D{是行为风格或固定任务吗}
    D -->|是| E[微调/Few-shot]
    D -->|否| F[Prompt/工具/流程优化]
```

## 面试讲法

> 我不会把微调当成万能方案。企业知识变化快时优先 RAG；需要模型学会稳定风格、分类标准或固定输出模式时才考虑微调。微调前要有高质量样本和评估集，微调后要做安全、幻觉、格式和成本回归。

## 学习检查

- [ ] 能解释 LoRA 与全量微调的区别。
- [ ] 能说清 RAG 与微调的边界。
- [ ] 能设计一个客服意图分类微调数据格式。
- [ ] 能说明微调上线前要跑哪些回归测试。

## 📚 参考资料

- [LLaMA-Factory GitHub](https://github.com/hiyouga/LLaMA-Factory)
- [Hugging Face PEFT Documentation](https://huggingface.co/docs/peft/index)
- [QLoRA Paper](https://arxiv.org/abs/2305.14314)

