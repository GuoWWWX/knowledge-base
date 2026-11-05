---
title: MCP调试-Inspector
tags:
  - 领域/AI应用
  - 类型/知识
  - 主题/MCP
  - 阶段/05
created: 2026-07-04
updated: 2026-11-01
status: 已完成
mastery: ⭐☆☆☆☆
---

# MCP 调试：Inspector

> [!abstract] 一句话总结
> Inspector 是官方 MCP 调试客户端。先用它连接 Server、查看工具和手动调用，可以判断问题在服务端还是模型接入环节。

## 🎯 为什么先用 Inspector？

用户问“订单 A100 到哪了？”时，如果 Agent 没有给出订单状态，可能是 Server 没启动、工具没注册、参数不对，也可能是模型没有选择工具。Inspector 不需要模型，能直接查看 Server 暴露了什么并发送调用请求。
本篇继续使用 [[10-MCP-Server开发-Java实战|订单 Server]]：工具名是 `queryOrderStatus`，本机地址是 `http://127.0.0.1:8080/mcp`，返回内容是模拟订单状态。

## 🚀 启动并连接

先启动订单 Server，再在终端运行：

```bash
npx @modelcontextprotocol/inspector
```

首次运行可能需要下载包；Node.js 版本要求随 Inspector 版本变化，当前要求和启动方式见[官方文档](https://modelcontextprotocol.io/docs/tools/inspector)。打开终端打印的地址，在 Servers 中添加 HTTP / Streamable HTTP 服务，填入 `http://127.0.0.1:8080/mcp` 并连接。界面名称会随 Inspector 版本变化，重点是传输方式和完整地址与 Server 一致。
Inspector 自己的网页地址与订单 MCP Server 地址是两个地址。Inspector 后端可以启动本机进程、调用工具，不要把带访问令牌的地址分享出去，也不要随意暴露到公网。

## 🧪 查看并调用订单工具

连接成功后打开 Tools，找到 `queryOrderStatus`，检查描述和输入 schema：工具是否要求 `orderId`、参数类型是否为字符串。填入订单号并执行，相当于发送一次 `tools/call`：

```json
{ "orderId": "A100" }
```

如果使用的是上一节的模拟代码，返回内容应包含“配送中”；改成其他订单号，应看到“订单不存在”。再试空字符串和缺失参数，观察表单、schema 校验及服务端返回，确认调用方能理解错误。
调用是真实执行，不是只预览参数。测试写入、退款或发消息工具前，先确认连接的是测试环境和测试数据。

> [!tip] 只想验证协议，不需要接模型
> Inspector 能列出并调用工具，说明当前测试条件下 Server 的这条 MCP 链路可用；它不代表模型一定会选择工具，也不能替代生产环境的权限和并发测试。

## 🔍 根据现象定位

| 现象 | 先检查什么 |
| ---- | ---------- |
| 连接失败 | Server 是否启动、端口与 `/mcp` 路径、传输方式是否一致 |
| 返回 401 / 403 | 凭据、用户权限和网关安全配置 |
| 已连接但找不到工具 | Bean 扫描、工具注册和 Server 能力配置 |
| 能找到工具但调用失败 | 参数 schema、工具返回及 Server 日志 |
| Inspector 正常，Agent 没调用 | `ChatClient` 是否拿到工具、工具描述和模型能力 |

在 Inspector 的 Protocol 中可以查看请求与响应，HTTP 场景还可检查 Network；同时查看 Spring Boot 日志。协议请求格式错误可能返回顶层 `error`，工具执行失败可以在结果中标记 `isError: true`，连接中断或超时则属于传输异常，处理位置不同。

## 🖥️ stdio 和 HTTP 有什么区别？

本篇用 HTTP 连接已经启动的 Server；stdio 场景通常由 Inspector 按配置的命令启动本地 Server 进程，命令、参数和环境变量应查看该 Server 的说明。stdio 使用标准输出传输协议消息，业务日志应写到 stderr 或文件，避免破坏通信。

## ❓ 自测

> [!question]- Inspector 能成功调用工具，为什么 Agent 还可能不调用？
> Inspector 是直接调用；Agent 还要把工具提供给模型，再由模型选择工具。应检查工具注册、描述、模型支持情况和实际调用记录。

> [!question]- 为什么要测试缺失参数和错误参数？
> 可以检查 schema、服务端校验和错误返回是否符合预期，避免只验证正常输入。

> [!question]- 为什么 Inspector 的调用需要谨慎？
> 它会真正执行工具。对写操作应使用测试环境，并按业务要求处理权限和确认。

## 🔗 关联笔记

- [[_index]] · [[09-MCP协议详解]] · [[10-MCP-Server开发-Java实战]]
- [[11-MCP-Client开发实战]] · [[../00-总览/学习仪表盘]]

## 📚 参考资料

- [MCP Inspector 官方文档](https://modelcontextprotocol.io/docs/tools/inspector)
- [Inspector Web 界面说明](https://modelcontextprotocol.io/docs/tools/inspector/web)
- [Spring AI MCP Server Starter](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-server-boot-starter-docs.html)
