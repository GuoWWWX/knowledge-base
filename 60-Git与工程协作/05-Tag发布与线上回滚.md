---
title: Git Tag 发布与线上回滚
tags:
  - 领域/通用
  - 类型/知识
  - 主题/Git
created: 2026-08-19
updated: 2026-08-19
status: 整理中
mastery: ⭐☆☆☆☆
---

# Git Tag 发布与线上回滚

## 1. Merge、Tag 和部署不是一回事

| 对象 | 含义 |
| --- | --- |
| merge | 把一条历史整合到另一条历史 |
| tag | 给某个 commit 起一个稳定名称 |
| 制品 | 从源码构建出的镜像、压缩包或安装包 |
| 部署 | 把指定制品放到目标环境并验证 |

正确顺序通常是：合入发布分支 -> 验收 commit -> 创建 tag -> CI 构建制品 -> 部署 -> 验证。

## 2. 创建发布 Tag

```bash
git switch main
git pull --ff-only origin main
git status
git log -1 --oneline --decorate

git tag -a v1.2.0 -m "release: v1.2.0"
git show v1.2.0
git push origin v1.2.0
```

发布标签应指向已经验收的准确 commit。已使用的标签不要静默移动。

## 3. Tag 的追溯关系

至少记录：

- Git commit ID 和 tag。
- CI 构建编号及制品摘要，例如镜像 digest。
- 部署环境、时间、操作者和验证结果。
- 对应的数据库迁移、配置版本和回滚限制。

`git show v1.2.0` 可以确认源码指向，但不能证明制品已经成功部署。

## 4. 线上故障先恢复服务

线上回滚优先选择上一个已验证、可复现的制品：

1. 停止继续发布和危险变更。
2. 确认当前版本、上一个稳定版本和数据库状态。
3. 通过发布平台重新部署旧制品。
4. 验证健康检查、核心接口、数据读写和关键业务链路。
5. 记录回滚结果，再修正仓库历史和后续版本。

不要把生产服务器临时切到 detached HEAD 当作完整发布方案。源码回退和制品回滚必须可审计、可重复。

## 5. 用 Revert 修正仓库

普通提交：

```bash
git switch -c fix/revert-payment origin/main
git revert <bad-commit>
git push -u origin fix/revert-payment
```

如果错误版本是一个合入 `main` 的 merge commit：

```bash
git show --no-patch --pretty=raw <merge-commit>
git revert -m 1 <merge-commit>
```

`-m 1` 表示把第一个父提交当作主线，不能机械套用，必须先看父提交关系。

通过评审后创建补丁版本：

```bash
git tag -a v1.2.1 -m "release: v1.2.1 revert faulty payment change"
git push origin v1.2.1
```

`git revert` 不会自动创建新 tag，也不会自动部署。

## 6. 数据库回滚边界

应用代码可以回到旧版本，不代表数据库也能回到旧版本。发布前要明确迁移兼容性、消息格式、缓存结构、外部接口以及数据恢复方案。如果数据库已发生不可逆变化，应优先采用向前修复或兼容版本，不要盲目回滚应用。

## 相关

- 总参考：[[01-Git全流程实战手册#7. Tag 与上线]]
- 前一篇：[[04-冲突处理与历史整理]]
- 下一篇：[[06-撤销与误操作恢复]]

