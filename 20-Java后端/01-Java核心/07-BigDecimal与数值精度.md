---
title: BigDecimal与数值精度
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

# BigDecimal与数值精度

> [!abstract] 一句话总结
> BigDecimal 用整数与十进制缩放表示小数，避免二进制浮点的表示误差；正确性仍取决于输入来源、scale、舍入规则和比较语义。

## 🎯 本节定位

- 前置：[[05-基本类型与包装类]]、[[02-异常处理最佳实践]]；本篇示例使用 JDK 21。
- 应用：金额、税率、折扣；作为集合 key 时衔接 [[04-集合框架/01-核心机制与选型]]。

## 🤔 为什么需要

`0.1` 无法用有限位二进制小数精确表示，因此 `0.1 + 0.2` 可能打印成 `0.30000000000000004`。这不是 double 加法随意出错，而是输入已经是近似值。

如果业务要求精确十进制运算，从十进制文本开始构造 BigDecimal；如果金额单位固定为分，也可以使用 long，但必须约定币种、精度并检查溢出。

## 🧩 核心概念

| 概念 | 含义 | 例子 |
| --- | --- | --- |
| unscaledValue | 未缩放的任意精度整数 | 12.30 对应 1230 |
| scale | 10 的负指数；可为负 | 12.30 的 scale 为 2 |
| precision | 未缩放整数的十进制有效位数 | 12.30 为 4 |
| MathContext | 有效数字精度与舍入方式 | 与“固定两位小数”不是同一要求 |
| RoundingMode | 丢弃数字时的取舍规则 | HALF_UP、HALF_EVEN、DOWN 等 |

核心关系是 `value = unscaledValue × 10^(-scale)`。BigDecimal 不可变，`add`、`setScale` 等返回结果，不修改原值。任意精度不代表资源无限，巨大输入仍会消耗内存与计算时间。

## ⚙️ 从输入到输出的精度边界

### 构造：别先损失再补救

`new BigDecimal("0.1")` 直接表达十进制 0.1；`new BigDecimal(0.1)` 精确展开 double 中已有的近似值，会出现很长的小数。`BigDecimal.valueOf(0.1)` 基于 double 的标准字符串表示，通常更符合直觉，但不能恢复此前浮点运算已丢失的精度。

### 运算：明确舍入发生在哪一层

加减通常沿用较大 scale，乘法的首选 scale 为两者之和。精确除法遇到无限循环小数（如 1/3）会抛异常，应指定结果 scale 与 RoundingMode。

`setScale(2, HALF_UP)` 是保留两位小数；`round(new MathContext(2))` 是保留两位有效数字。`HALF_UP` 在恰好半数时向远离 0 的方向舍入；`DOWN` 向 0 截断，不等于负数的向下取整。

逐行舍入后求和与总和后舍入可能不同。例如两笔 0.005，各自 HALF_UP 到两位再加得到 0.02；先加再舍入得到 0.01。必须由业务约定，不能在每次计算时随手 setScale。

### 比较：数值相等不等于表示相等

`new BigDecimal("1.0").equals(new BigDecimal("1.00"))` 为 false，因为 scale 不同；`compareTo` 返回 0。因此 HashSet 可能保留两项，TreeSet 按自然顺序只保留一项。

做金额大小比较用 compareTo；做哈希 key 则先定义统一单位和规范化策略，不要只重写 equals 却不配套 hashCode。`stripTrailingZeros()` 可能产生负 scale 和科学计数法，面向固定格式输出时用明确 scale 与 `toPlainString()`。

## 💻 代码实践 / 🧪 最小验证

保存为 `DecimalDemo.java`，运行 `java DecimalDemo.java`。

```java
import java.math.*;
import java.util.*;

public class DecimalDemo {
    public static void main(String[] args) {
        System.out.println(0.1 + 0.2); // 0.30000000000000004
        System.out.println(new BigDecimal("0.1").add(new BigDecimal("0.2"))); // 0.3
        BigDecimal a = new BigDecimal("1.0"), b = new BigDecimal("1.00");
        System.out.println(a.equals(b));          // false
        System.out.println(a.compareTo(b));      // 0
        System.out.println(new HashSet<>(List.of(a, b)).size()); // 2
        System.out.println(new TreeSet<>(List.of(a, b)).size()); // 1
        System.out.println(BigDecimal.ONE.divide(new BigDecimal("3"),
                2, RoundingMode.HALF_UP));       // 0.33
        System.out.println(new BigDecimal("12.30").movePointRight(2).longValueExact()); // 1230
        try {
            BigDecimal.ONE.divide(new BigDecimal("3"));
        } catch (ArithmeticException e) {
            System.out.println("rounding rule required");
        }
        BigInteger large = new BigInteger("9223372036854775808");
        System.out.println(large.add(BigInteger.ONE)); // 9223372036854775809
    }
}
```

## 🧩 BigInteger 怎么选

整数超过 long 范围且需要精确运算时用 BigInteger，支持 add、multiply、divide、mod 等；它同样不可变。转换回基本类型时，`intValue/longValue` 可能截断，要求严格转换时使用 Exact 版本。

## ⚠️ 踩坑与注意事项

- 忘记接收 `amount.add(...)` 返回值，原 amount 不会变化。
- `new BigDecimal("1.239").longValue()` 变成 1；需要拒绝小数或溢出时用 `longValueExact()`。
- `BigDecimal` 不能弥补数据库字段、接口格式或中间 double 转换造成的精度丢失。
- 不把展示格式当作存储和运算规则；币种不一定都保留两位小数。

## 🎯 面试问题

> [!question]- BigDecimal 为什么能精确表示 0.1，却仍可能在除法中抛异常？
> 有限十进制可由整数和 scale 精确表示，但 1/3 需要无限位小数；要求有限结果时必须明确精度或 scale 与舍入策略。

> [!question]- 1.0 和 1.00 放进 HashSet、TreeSet，为什么数量不同？
> HashSet 依据 equals/hashCode，BigDecimal 的 equals 区分 scale；TreeSet 依据比较结果去重，compareTo 只比较数值大小。

## 🧭 源码入口速查

- `BigDecimal#valueOf`、构造器：数值来源。
- `BigDecimal#equals` / `compareTo` / `setScale`：表示、顺序与舍入。
- `BigInteger#longValueExact`：严格转换。

## ✅ 精通检查清单

- 能解释构造、运算、比较、转换四处精度边界。
- 能写出 1/3 的明确舍入版本，并复现两种集合的去重差异。
- 能说清逐行舍入和汇总后舍入为什么可能不等价。

## 🔗 关联笔记

- [[05-基本类型与包装类]] → 本篇 → [[08-日期时间API]]。
- [[04-集合框架/07-排序详解]]；本章导航：[[_index]]。

## 📚 参考资料

- [BigDecimal API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html)
- [RoundingMode API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/RoundingMode.html)
- [BigInteger API](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigInteger.html)
