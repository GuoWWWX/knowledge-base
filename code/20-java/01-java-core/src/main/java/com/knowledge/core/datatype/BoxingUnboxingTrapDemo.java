package com.knowledge.core.datatype;

/**
 * 包装类型与自动装箱拆箱陷阱实战：
 * 1. IntegerCache [-128, 127] 常量池缓存与 == 判等翻车
 * 2. 自动拆箱引发的 NullPointerException (NPE)
 * 3. 生产中最隐蔽的三目运算符 (Ternary Operator) 拆箱与类型提升 NPE 故障
 */
public class BoxingUnboxingTrapDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. IntegerCache [-128, 127] 缓存池 == 判等陷阱 ==========");
        Integer a = 127;
        Integer b = 127;
        System.out.println("Integer a = 127, b = 127 -> (a == b): " + (a == b) + " (命中缓存池同一对象引用)");

        Integer c = 128;
        Integer d = 128;
        System.out.println("Integer c = 128, d = 128 -> (c == d): " + (c == d) + " (超出127，分别 new 堆内存对象)");
        System.out.println("正确判等方式: c.equals(d) -> " + c.equals(d));

        System.out.println("\n========== 2. 基础类型自动拆箱 NPE ==========");
        try {
            Integer nullInt = null;
            int primitive = nullInt; // 底层编译为 nullInt.intValue()
        } catch (NullPointerException e) {
            System.err.println("直接赋值拆箱捕获 NPE: " + e.getClass().getSimpleName() + " (调用了 null.intValue())");
        }

        System.out.println("\n========== 3. 三目运算符隐式拆箱与精度提升 NPE 生产血泪史 ==========");
        simulateTernaryNpe();
    }

    private static void simulateTernaryNpe() {
        boolean flag = false;
        Integer boxedInteger = null;

        try {
            // 三目运算符规则 (JLS 15.25): 当第二操作数与第三操作数分别为基本类型 double 和包装类型 Integer 时，
            // 编译器为了类型对齐，会将整个三目表达式向上提升为 double，导致第三操作数 boxedInteger 被强制自动拆箱！
            Double result = flag ? 1.0d : boxedInteger;
            System.out.println("计算结果: " + result);
        } catch (NullPointerException e) {
            System.err.println("【三目运算符隐式拆箱 NPE 爆发】: " + e.getClass().getName());
            System.out.println(">>> 源码剖析: flag ? 1.0d : boxedInteger");
            System.out.println(">>> 字节码展开: flag ? Double.valueOf(1.0d) : Double.valueOf((double) boxedInteger.intValue())");
            System.out.println(">>> boxedInteger 为 null 时调用 intValue() 瞬间暴毙！避免混合基本类型与包装类型三目运算。");
        }
    }
}
