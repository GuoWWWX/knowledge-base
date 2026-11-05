---
title: 日期时间API
tags:
  - 领域/Java后端
  - 类型/知识
  - 主题/Java核心
  - 阶段/01
created: 2026-11-01
updated: 2026-11-01
status: 整理中
mastery: ⭐☆☆☆☆
---

# 日期时间API

> [!abstract] 一句话总结
> 日期、本地时间和时间线上的瞬间不是同一种数据；java.time 用独立类型表达这些差异，时区负责映射，Clock 让“现在”成为可控制的依赖。

## 🎯 本节定位

- 常用业务 API，前置：[[06-面向对象与方法调用]]、[[02-异常处理最佳实践]]。
- java.time 自 JDK 8 提供，本篇完整示例在 JDK 21 运行。

## 🧩 核心概念与选型

| 类型 | 表达什么 | 常见场景 |
| --- | --- | --- |
| LocalDate | 年月日，没有时间和时区 | 生日、营业日期 |
| LocalTime | 时分秒，没有日期和时区 | 每天开门时间 |
| LocalDateTime | 本地日期与时间，没有时区 | 尚未指定时区的预约时间 |
| Instant | 时间线上的一个瞬间 | 事件发生时间、跨系统排序 |
| OffsetDateTime | 日期时间与 UTC 偏移 | 接口传输带偏移的时间 |
| ZonedDateTime | 日期时间、区域时区和实际偏移 | 涉及时区规则的日历计算 |
| Duration | 按秒、纳秒度量的时间量 | 经过多久、超时时长 |
| Period | 年月日组成的日历量 | 一个月、一个日历年 |
| ZoneId / ZoneOffset | 区域规则 / 固定偏移 | Asia/Singapore / +08:00 |
| Clock | 当前时刻与默认时区的来源 | 让测试不依赖真实系统时间 |

java.time 中主要日期时间类型不可变；DateTimeFormatter 不可变且线程安全。旧的 Date 是可变的瞬间表示，不包含业务时区；SimpleDateFormat 是可变对象，不应共享给多个线程无保护使用。

## ⚙️ 为什么 LocalDateTime 不是时间戳

“2026-11-01 09:00”在新加坡和纽约对应不同瞬间。LocalDateTime 只有表盘数字，要变成 Instant 必须提供时区或偏移；直接拿服务器默认时区转换，部署后可能变成另一个时刻。

区域时区包含规则，固定偏移不包含夏令时规则。在夏令时切换处，本地时间可能不存在，或对应两个瞬间。`atZone` 会按 API 规则处理空缺和重叠；预约业务应明确接受调整、拒绝输入还是让用户选择偏移。

### 日历的一天与 24 小时

`ZonedDateTime.plusDays(1)` 走本地日期时间线；`plus(Duration.ofHours(24))` 增加实际经过的 24 小时。夏令时切换日两者可能相差 1 小时。生日等日历逻辑用 LocalDate；测耗时用 `System.nanoTime()` 的差值，不用可被校时影响的墙上时间。

### 格式化与解析

- 常见日期格式用 `uuuu-MM-dd`；`YYYY` 是基于周的年，跨年附近会产生意外结果。
- 严格解析可用 `withResolverStyle(ResolverStyle.STRICT)`，拒绝 2 月 30 日等非法日期。
- 文本月份等内容还依赖 Locale，跨系统交换优先采用明确的 ISO 格式。
- 秒级时间戳用 `Instant.ofEpochSecond`，毫秒级用 `ofEpochMilli`，不能混用单位。

## 💻 代码实践 / 🧪 最小验证

保存为 `TimeDemo.java`，运行 `java TimeDemo.java`。固定时间避免测试随日期变化；纽约示例用于观察 2026 年夏令时切换规则。

```java
import java.time.*;
import java.time.format.*;

public class TimeDemo {
    public static void main(String[] args) {
        ZoneId sg = ZoneId.of("Asia/Singapore");
        Clock fixed = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), sg);
        System.out.println(LocalDateTime.now(fixed)); // 2026-11-01T08:00
        LocalDateTime local = LocalDateTime.of(2026, 11, 1, 8, 0);
        System.out.println(local.atZone(sg).toInstant()); // 2026-11-01T00:00:00Z
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("uuuu-MM-dd")
                .withResolverStyle(ResolverStyle.STRICT);
        try {
            LocalDate.parse("2026-02-30", fmt);
        } catch (DateTimeParseException e) {
            System.out.println("invalid date");
        }
        ZonedDateTime before = ZonedDateTime.of(2026, 3, 7, 12, 0, 0, 0,
                ZoneId.of("America/New_York"));
        System.out.println(Duration.between(before, before.plusDays(1)).toHours()); // 23
        System.out.println(Duration.between(before,
                before.plus(Duration.ofHours(24))).toHours()); // 24
        System.out.println(java.util.Date.from(fixed.instant()).toInstant()
                .equals(fixed.instant())); // true，本例无纳秒精度损失
    }
}
```

## 🛠️ 排查 / 实践要点

- 差 8 小时：检查数据含不含偏移、解析时区、数据库列类型、JDBC 映射和展示时区。
- 差 1000 倍：检查秒与毫秒单位，不靠时间戳位数猜业务协议。
- 旧 Date 与 Instant 通过 `toInstant` / `Date.from` 互转；Date 只有毫秒精度，纳秒信息可能丢失。
- 不把 LocalDateTime 的无时区文本当作跨系统唯一瞬间；也不强行把生日变成 UTC 时间戳。

## 🎯 面试问题

> [!question]- LocalDateTime 和 Instant 有什么区别？
> 前者是没有时区的本地日期时间，后者是时间线上的瞬间；两者转换必须补充时区或偏移。服务器默认时区不能替代业务契约。

> [!question]- 为什么加一天不一定等于加 24 小时？
> 区域时区的夏令时切换会改变偏移，一天的日历跨度可能是 23 或 25 小时。按日历排程与按实际经过时长计时应选不同操作。

> [!question]- 如何测试“今天到期”的逻辑而不让测试过一天就失效？
> 接受 Clock 作为依赖，测试传入 Clock.fixed，并显式指定业务时区；不要在方法内部到处直接调用无参数 now。

## 🧭 源码入口速查

- `LocalDateTime#atZone`、`ZonedDateTime#ofLocal`：本地时间映射。
- `ZoneRules#getValidOffsets`：查看空缺与重叠。
- `Clock#fixed`、`DateTimeFormatter`：可重复验证与解析。

## ✅ 精通检查清单

- 能为生日、订单支付时间、每日定时任务选择不同时间类型。
- 能复现严格日期解析、固定 Clock 与 23 小时日历跨度。
- 能在接口中写清单位、时区与精度，而不只写“时间字段”。

## 🔗 关联笔记

- [[07-BigDecimal与数值精度]]；下一步：[[09-Optional与空值处理]]。
- 时间类型用于 [[14-Java新特性-Record与密封类]] 中的数据建模；本章导航：[[_index]]。

## 📚 参考资料

- [java.time 包说明](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/package-summary.html)
- [ZonedDateTime API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/ZonedDateTime.html)
- [DateTimeFormatter API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/time/format/DateTimeFormatter.html)
