---
title: SpringMVC请求处理流程
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Spring
  - 阶段/04
created: 2026-07-04
updated: 2026-09-11
status: 已完成
mastery: ⭐⭐⭐⭐⭐
---

# 🏥 06 · SpringMVC 请求处理流程（三甲医院分诊台与 11 步大链路）

> **一句话导读**：浏览器发来的一个 JSON 请求，是怎么变成 Controller 里的 Java 对象，又是怎么把结果格式化为标准响应返回的？用“大型三甲医院门诊就医”模型，带你轻松秒杀面试官最爱考察的 MVC 执行全链路。

---

## 🏥 核心心智模型：大型三甲医院的就医大厅

如果把整个 SpringMVC 架构比作一家**现代化的三甲综合医院**，各个组件的分工立刻一目了然：

```mermaid
flowchart TD
    Patient["🤒 外部患者 (HTTP 请求)"] --> DS["👩‍⚕️ 大厅总分诊台 (DispatcherServlet)<br/>👉 统一接客调度，自己不看病，负责全程统筹"]
    
    DS --> HM["📋 导医挂号处 (HandlerMapping)<br/>👉 根据挂号单科室，查出该去哪个科室找哪个医生 (定位 Controller)"]
    
    DS --> HA["👨‍⚕️ 诊室专职助理 (HandlerAdapter)<br/>👉 负责把患者的化验单翻译整理好，递给主治医生 (参数解析与转换)"]
    
    HA --> Controller["🩺 主治专家医生 (Controller 业务方法)<br/>👉 真正把脉看病开处方 (执行核心业务代码)"]
    
    Controller --> HMC["💊 自动化中药房包装机 (HttpMessageConverter)<br/>👉 把医生开的处方对象打包压制成标准包装盒 (转成 JSON 响应体)"]
```

---

## 🏃 经典 11 步完整就诊大链路时序图

SpringMVC 处理一个 HTTP 请求的**标准 11 步闭环**（面试背诵黄金标准）：

```mermaid
sequenceDiagram
    autonumber
    actor Client as 浏览器 / 客户端
    participant DS as DispatcherServlet (总调度台)
    participant HM as HandlerMapping (映射器)
    participant HA as HandlerAdapter (适配器)
    participant Interceptor as 拦截器 (Interceptor)
    participant Controller as 你的业务 Controller
    participant Conv as HttpMessageConverter (转换器)

    Client->>DS: 1. 发送 HTTP POST /orders 请求
    DS->>HM: 2. 查问：谁能处理这个 URL？
    HM-->>DS: 3. 返回执行链 HandlerExecutionChain (包含目标 Controller + 拦截器列表)
    
    DS->>HA: 4. 寻找适配器：谁能帮我调用这个 Controller？
    HA-->>DS: 5. 确定使用 RequestMappingHandlerAdapter
    
    DS->>Interceptor: 6. 执行拦截器 preHandle() 前置拦截 (查健康码/验权)
    
    DS->>HA: 7. 正式委托适配器执行
    HA->>Conv: 7.1 参数解析：把 JSON 字符串反序列化为 Java 对象
    HA->>Controller: 7.2 反射调用具体的业务方法
    Controller-->>HA: 7.3 业务执行完毕，返回结果 DTO 对象
    
    HA->>Conv: 8. 返回值处理：用 Jackson 把 DTO 对象序列化为 JSON 字符串写出
    DS->>Interceptor: 9. 执行拦截器 postHandle() 后置处理
    DS->>Interceptor: 10. 执行拦截器 afterCompletion() 收尾清理
    DS-->>Client: 11. 最终返回 HTTP 200 与 JSON 数据
```

---

## 🔍 核心四大法宝大白话拆解

### 1. 核心总调度：`DispatcherServlet`
- 它是一个标准的 Servlet，配置了拦截根路径 `/`。
- **所有进出 SpringMVC 的请求，必须经过它的统一接管与派发**。

### 2. 探路雷达：`HandlerMapping`
- 负责建立并保存 **“URL 路径 ⇄ 具体的 Controller 及其 Method”** 的映射字典。
- 最常用的是 `RequestMappingHandlerMapping`（扫描 `@RequestMapping`、`@PostMapping` 等注解建立索引）。

### 3. 万能翻译官与协调者：`HandlerAdapter`
- **为什么需要适配器？为什么不直接调用 Controller？**
  - 因为 SpringMVC 历经多年发展，支持多种风格的 Handler（注解版的 `@Controller`、老古董实现 `Controller` 接口的、实现 `HttpRequestHandler` 的）。
  - 通过**适配器模式**，抹平不同实现风格的调用差异；
  - 并且内部集成了强大的 **`HandlerMethodArgumentResolver`（参数解析器）**，自动帮你注入 `@PathVariable`、`@RequestParam`、`@RequestBody`！

### 4. 数据变形金刚：`HttpMessageConverter`
- 基于 HTTP 头部的 `Content-Type` 和 `Accept`，负责在“HTTP 字节流”和“Java 对象”之间完成格式转换。
- 最核心的实现是 **`MappingJackson2HttpMessageConverter`**，全权负责把 JSON 字符串反序列化为 Java 实体，以及把返回实体序列化为 JSON。

---

## ⚖️ 面试大热点：Filter（过滤器）vs Interceptor（拦截器）

这是 99% 的面试必考题：“它们都是做拦截的，到底有什么区别？分别用在什么场景？”

```mermaid
flowchart LR
    Request["HTTP 请求"] --> F1["Filter 1 (Servlet 规范)"]
    F1 --> DS["DispatcherServlet"]
    DS --> I1["Interceptor 1 (Spring 体系)"]
    I1 --> Ctrl["Controller"]
    Ctrl --> I1
    DS --> F1
    F1 --> Response["HTTP 响应"]
```

| 对比维度 | Filter（过滤器） | Interceptor（拦截器） |
| :--- | :--- | :--- |
| **出身与规范** | **Servlet 规范自带的标准组件**（依赖 Tomcat/Jetty 容器） | **SpringMVC 框架自己的专属组件**（依赖 Spring 容器） |
| **拦截范围** | 拦截所有经过 Servlet 容器的请求（包括静态资源 HTML、CSS、图片） | **只拦截经过 `DispatcherServlet` 映射成功的 Controller 请求** |
| **执行时机** | 在进入 `DispatcherServlet` **之前和之后** | 在经过 `HandlerMapping` 找到 Controller **之后，方法执行前后** |
| **能拿到什么信息？** | 只能拿到原始的 `HttpServletRequest` 和 `HttpServletResponse` | **能直接拿到目标方法（`HandlerMethod`）的元数据、反射注解等** |
| **典型应用场景** | 字符编码转换（`CharacterEncodingFilter`）、跨域 CORS 配置、敏感词通用过滤 | **用户登录会话检查、接口防刷限流、接口粒度的鉴权签名、操作日志记录** |

---

## 🛡️ 工业级生产实战：全局异常处理器（急救中心）

在生产开发中，**严禁在每个 Controller 里写恶心的 `try-catch`**！
必须使用 `@RestControllerAdvice` 搭配 `@ExceptionHandler`，打造企业级的统一全局异常急诊中心：

```java
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. 拦截参数校验异常 (@Valid 校验失败)
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleValidationException(BindException e) {
        String errorMsg = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return Result.fail(400, "参数校验失败: " + errorMsg);
    }

    // 2. 拦截自定义业务异常 (比如库存不足、余额不够)
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    // 3. 兜底未知异常 (代码空指针、数据库宕机)
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleUnexpectedException(Throwable e) {
        // 打印详细堆栈日志用于排查，但对前端隐藏内部报错细节，保证安全
        log.error("💥 系统未知崩溃异常: ", e);
        return Result.fail(500, "网络开小差了，请稍后重试");
    }
}
```

---

## 🧠 一分钟复盘核心记忆卡

1. **总调度室**：`DispatcherServlet` 负责接客统筹；
2. **三部曲**：`HandlerMapping` 定位诊室，`HandlerAdapter` 助理翻译参数，`HttpMessageConverter` 打包 JSON；
3. **拦截分工**：外层通用粗粒度用 `Filter`，内层业务细粒度用 `Interceptor`；全局报错统一交给 `@RestControllerAdvice`。

---

## 🧭 下一步

在 Controller 里把请求参数解析完成后，我们通常需要去数据库查存数据。
在 Java 后端领域，最主流、最灵活的持久层王者毫无疑问就是 **`MyBatis`**。
为什么写个接口连实现类都没有，它就能隔空去查数据库并返回对象？底层四大处理器是怎么运转的？
请进入下一篇：[[07-MyBatis原理]]！
