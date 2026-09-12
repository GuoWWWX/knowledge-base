---
title: Git 总结与速查
tags:
  - 领域/通用
  - 类型/总结
  - 主题/Git
created: 2026-08-19
updated: 2026-08-19
status: 整理中
mastery: ⭐☆☆☆☆
---

# Git 总结与速查

## 一、日常开发

```bash
git fetch --prune origin
git switch develop
git pull --ff-only origin develop
git switch -c feature/<name>

git status
git diff
git add <exact-files>
git diff --cached
git commit -m "feat: describe the change"
git push -u origin feature/<name>
```

## 二、命令选择

| 我想做什么 | 用什么 |
| --- | --- |
| 丢弃未暂存修改 | `git restore <file>` |
| 取消暂存 | `git restore --staged <file>` |
| 整理个人分支历史 | `git rebase -i` |
| 同步公共分支且保留事实 | `git merge` |
| 回退未推送提交 | `git reset` |
| 撤销公共提交 | `git revert` |
| 找回误删提交 | `git reflog` |
| 发布版本命名 | `git tag -a` |

## 三、不可违反的边界

- 公共分支不 reset、不 rebase 后强推。
- 私有分支改写历史时使用 `--force-with-lease`，不用裸 `--force`。
- Release 从待发布的 `develop` 创建；Hotfix 从生产基线 `main` 创建。
- Tag 指向验收 commit，不随意移动已发布标签。
- 线上先恢复已验证制品，再通过 revert 修正仓库。
- 数据库、消息和缓存的回滚边界必须单独确认。

## 四、发布检查清单

- [ ] 工作区 clean，提交内容和 diff 已检查。
- [ ] 测试、构建和 CI 通过。
- [ ] Tag 指向准确的发布 commit。
- [ ] 制品摘要、环境和部署记录可追溯。
- [ ] 上一个稳定制品可获取。
- [ ] 数据库迁移和回滚方案经过验证。
- [ ] 发布后健康检查和核心业务验证通过。

## 五、红宝书

> 本地同步可 rebase，公共协作靠 merge；
> 本地撤回用 restore/reset，公共撤销用 revert；
> 发布先验收 commit，再创建不可变 tag；
> 线上先回稳定制品，仓库再做反向提交；
> 误操作先查 reflog，恢复先建保护分支。

## 相关

- 总参考：[[01-Git全流程实战手册]]
- 上级：[[_index|Git 与工程协作索引]]

