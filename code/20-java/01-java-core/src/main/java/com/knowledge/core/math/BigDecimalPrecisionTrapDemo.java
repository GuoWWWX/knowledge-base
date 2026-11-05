package com.knowledge.core.math;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 金融高精度计算 BigDecimal 避坑全指南：
 * 1. new BigDecimal(double) 构造器精度失真事故
 * 2. equals() 与 compareTo() 判等语义大坑（精度 scale 差异）
 * 3. divide 除不尽未设置舍入模式抛 ArithmeticException
 * 4. 浮点数加减累计误差与正确写法
 */
public class BigDecimalPrecisionTrapDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. new BigDecimal(double) 精度失真惨案 ==========");
        double val = 0.1d;
        BigDecimal badDecimal = new BigDecimal(val);
        BigDecimal goodDecimal1 = BigDecimal.valueOf(val);
        BigDecimal goodDecimal2 = new BigDecimal("0.1");

        System.out.println("new BigDecimal(0.1)        = " + badDecimal);
        System.out.println("BigDecimal.valueOf(0.1)     = " + goodDecimal1);
        System.out.println("new BigDecimal(\"0.1\")       = " + goodDecimal2);
        System.out.println(">>> 根因分析: 0.1 的二进制是无限循环小数，new BigDecimal(double) 会忠实还原其不精确的二进制值！");
        System.out.println(">>> 规范: 必须使用 BigDecimal.valueOf() 或基于字符串的 new BigDecimal(String)！");

        System.out.println("\n========== 2. equals() vs compareTo() 精度陷阱 ==========");
        BigDecimal amount1 = new BigDecimal("1.0");
        BigDecimal amount2 = new BigDecimal("1.00");

        System.out.println("amount1.scale(): " + amount1.scale() + ", amount2.scale(): " + amount2.scale());
        System.out.println("amount1.equals(amount2)   : " + amount1.equals(amount2) + " (不仅比数值，还比 scale 精度！)");
        System.out.println("amount1.compareTo(amount2): " + (amount1.compareTo(amount2) == 0) + " (只比较纯数学数值大小)");
        System.out.println(">>> 规范: 在金钱比对、HashSet 去重或数据库对账时，绝不可盲目使用 equals()，应使用 compareTo() == 0！");

        System.out.println("\n========== 3. divide 除不尽未设 RoundingMode 崩溃 ==========");
        try {
            BigDecimal one = new BigDecimal("1");
            BigDecimal three = new BigDecimal("3");
            one.divide(three); // 抛出异常
        } catch (ArithmeticException e) {
            System.err.println("未设舍入模式 divide 崩溃: " + e.getMessage());
        }

        BigDecimal safeDivide = new BigDecimal("1").divide(new BigDecimal("3"), 4, RoundingMode.HALF_UP);
        System.out.println("安全除法 (保留4位小数，四舍五入 HALF_UP): " + safeDivide);
    }
}
