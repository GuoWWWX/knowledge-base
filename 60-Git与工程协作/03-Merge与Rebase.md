---
title: Git Merge 与 Rebase
tags:
  - 领域/通用
  - 类型/知识
  - 主题/Git
created: 2026-08-19
updated: 2026-08-19
status: 整理中
mastery: ⭐☆☆☆☆
---

# Git Merge 与 Rebase

## 1. Merge：保留分叉事实

```bash
git switch feature/login
git fetch origin
git merge origin/develop
```

当两个分支都有新提交时，merge 产生一个新的合并提交：

```text
      B---C---M  feature
     /       /
A---D---E---/    develop
```

优点是不会改写已有提交，适合公共分支。`--no-ff` 可以在本来能快进时仍保留功能分支边界：

```bash
git merge --no-ff release/v1.2.0
```

## 2. Rebase：把提交重放到新基线

```bash
git switch feature/login
git fetch origin
git rebase origin/develop
```

原来的 `B`、`C` 会被重放为 `B'`、`C'`。因为父提交和提交元数据变化，新提交 ID 通常不同。

## 3. 什么时候选哪一个

| 场景 | 选择 | 原因 |
| --- | --- | --- |
| 同步自己独占的功能分支 | rebase | 保持线性历史 |
| 多人共用的分支 | merge | 不改写他人已有历史 |
| 功能合入主干 | 按团队规则，通常 merge/平台 squash | 保留可审查的合入边界 |
| release/hotfix 合入受保护分支 | merge | 发布和修复边界清晰 |
| `main`、`develop` | 不 rebase 后强推 | 公共历史不可改写 |

## 4. Rebase 后如何推送

如果分支已经推送过但明确只有自己使用：

```bash
git push --force-with-lease origin feature/login
```

不要默认使用 `--force`。`--force-with-lease` 会在远端出现你未知的新提交时拒绝覆盖，但它仍然会改写远程历史。

## 5. 交互式 Rebase

```bash
git rebase -i HEAD~4
```

| 动作 | 作用 |
| --- | --- |
| `pick` | 保留提交 |
| `reword` | 只改提交说明 |
| `squash` | 合并提交并编辑说明 |
| `fixup` | 合并提交并丢弃当前说明 |
| `drop` | 删除提交 |

只对未共享或明确允许改写的个人分支使用。已经合入主干的提交不要再交互式 rebase。

## 6. 冲突后的控制命令

```bash
# merge
git merge --abort

# rebase
git rebase --abort
git rebase --continue
git rebase --skip
```

解决冲突后必须重新检查 diff 并运行测试；冲突标记消失不代表业务逻辑正确。

## 相关

- 总参考：[[01-Git全流程实战手册#5. Merge 与 Rebase]]
- 前一篇：[[02-分支模型与团队协作]]
- 下一篇：[[04-冲突处理与历史整理]]

