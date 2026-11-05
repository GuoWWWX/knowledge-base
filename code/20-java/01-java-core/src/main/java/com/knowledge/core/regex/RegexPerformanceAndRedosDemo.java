package com.knowledge.core.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 正则表达式性能陷阱与 ReDoS (正则表达式拒绝服务攻击) 深度剖析：
 * 1. 为什么阿里巴巴 Java 开发手册严禁在循环或高频方法内调用 String.matches()？
 *    - 每次 String.matches() 内部都会重新调用 Pattern.compile() 解析有限状态机 (NFA)，开销极其昂贵！
 * 2. 静态常量 Pattern.compile() 预编译提升性能达 20~30 倍实测
 * 3. ReDoS (Regular Expression Denial of Service) 灾难性指数级回溯 (Catastrophic Backtracking) 原理与防御
 */
public class RegexPerformanceAndRedosDemo {

    // 推荐做法：预编译为静态常量
    private static final Pattern COMPILED_EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");

    public static void main(String[] args) {
        System.out.println("========== 1. Pattern 预编译 vs 每次 matches() 性能压测 (100,000 次) ==========");
        benchmarkRegexCompilation();

        System.out.println("\n========== 2. ReDoS 灾难性回溯导致 CPU 100% 假死原理揭秘 ==========");
        explainReDosVulnerability();
    }

    private static void benchmarkRegexCompilation() {
        int count = 100_000;
        String email = "architect.antigravity@knowledge-base.com";
        String regex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

        // 1. 错误做法: 循环内部调用 String.matches()
        long t1 = System.currentTimeMillis();
        boolean m1 = false;
        for (int i = 0; i < count; i++) {
            m1 = email.matches(regex);
        }
        long t2 = System.currentTimeMillis();

        // 2. 正确做法: 预编译 Pattern
        long t3 = System.currentTimeMillis();
        boolean m2 = false;
        for (int i = 0; i < count; i++) {
            m2 = COMPILED_EMAIL_PATTERN.matcher(email).matches();
        }
        long t4 = System.currentTimeMillis();

        System.out.printf("1. 每次 matches() 临时编译 Pattern 耗时: %d ms%n", (t2 - t1));
        System.out.printf("2. 静态预编译 COMPILED_PATTERN 耗时      : %d ms (性能飙升 %.1f 倍！)%n",
                (t4 - t3), (double) (t2 - t1) / Math.max(1, (t4 - t3)));
    }

    private static void explainReDosVulnerability() {
        System.out.println("【ReDoS 漏洞典型正则范式】: (a+)+$ 或 (a|aa)+$ 等嵌套量词");
        System.out.println("当目标字符串为 'aaaaaaaaaaaaaaaaaaaaaaaaaaaa!' (末尾不匹配) 时，");
        System.out.println("由于非确定性有限自动机 (NFA) 贪婪匹配与穷举回溯，尝试次数将呈 2^N 指数级爆炸！");
        System.out.println("短短 30 个字符即可让单个核心 CPU 占满 100% 陷入数分钟假死，引发整个 Java 进程线程耗尽拒绝服务！");
        System.out.println("\n>>> 【防御最佳实践】:");
        System.out.println("1. 杜绝嵌套量词 (如 (a+)+ 改为 a+)；");
        System.out.println("2. 使用独占模式 (Possessive Quantifiers: a++、a*+) 或固化分组防止无效回溯；");
        System.out.println("3. 生产网关设置入参最大长度限制 (如限制手机号/邮箱不超过 50 字符)。");
    }
}
