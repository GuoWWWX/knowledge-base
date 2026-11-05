package com.knowledge.core.regex;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 【正则表达式从零入门到实战 - 保姆级教学演练】
 * 
 * 核心心智模型：
 * 正则表达式（Regex）本质上就是一套“通配符语法”和“文本模式过滤器”。
 * 你给它一个模板规则，它帮你完成两件事：
 * 1. 【校验】：判断文本符不符合规则（如：是不是合法的 11 位手机号？）
 * 2. 【提取/替换】：从一大段杂乱文本里把想要的信息抠出来（如：把手机号中间 4 位换成 ****）
 */
public class RegexZeroToHeroDemo {

    public static void main(String[] args) {
        printStage1_ExactMatch();
        printStage2_MetaCharacters();
        printStage3_Quantifiers();
        printStage4_Boundaries();
        printStage5_GroupsAndExtract();
        printStage6_RealWorldUtils();
    }

    /**
     * 第 1 关：最朴素的精确字符匹配
     */
    private static void printStage1_ExactMatch() {
        System.out.println("======================================================================");
        System.out.println("【第 1 关】：最基本的文本匹配（不用任何特殊符号，写啥就搜啥）");
        System.out.println("======================================================================");
        String text = "I love Java, Java is awesome!";
        String regex = "Java";

        // 在 Java 里，最简单的查找
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        int count = 0;
        while (matcher.find()) {
            count++;
            System.out.printf("  找到第 %d 个匹配项: '%s'，在文本的下标范围: [%d, %d)%n",
                    count, matcher.group(), matcher.start(), matcher.end());
        }
        System.out.println("  >>> 说明: 没有任何符号时，它就是普通的关键字查找。");
    }

    /**
     * 第 2 关：常用元字符（代表一类字符的缩写）
     */
    private static void printStage2_MetaCharacters() {
        System.out.println("\n======================================================================");
        System.out.println("【第 2 关】：常用元字符（把数字、字母、空格用一个符号代表）");
        System.out.println("======================================================================");
        System.out.println("  * \\d  -> 代表任意一个数字 (0-9) [Digit 的缩写]");
        System.out.println("  * \\D  -> 代表任意一个非数字 (大写通常表示反义)");
        System.out.println("  * \\w  -> 代表任意一个字母、数字或下划线 (Word 字符: a-z, A-Z, 0-9, _)");
        System.out.println("  * \\s  -> 代表任意一个空白字符 (空格、制表符 \\t、换行 \\n) [Space 的缩写]");
        System.out.println("  * .   -> 代表任意单个字符（除了换行符）");

        String str = "商品编号: A-8092, 价格: 99元";
        // 抓出里面所有的数字
        Pattern digitPattern = Pattern.compile("\\d+");
        Matcher matcher = digitPattern.matcher(str);

        System.out.print("\n  测试从句子中提取纯数字: ");
        while (matcher.find()) {
            System.out.print("[" + matcher.group() + "] ");
        }
        System.out.println("\n  >>> 结果提取到了 [8092] 和 [99]");
    }

    /**
     * 第 3 关：量词（决定前面的东西出现多少次）
     */
    private static void printStage3_Quantifiers() {
        System.out.println("\n======================================================================");
        System.out.println("【第 3 关】：量词（控制字符出现的次数，初学者最容易被绕晕的地方）");
        System.out.println("======================================================================");
        System.out.println("  * ?     -> 出现 0 次或 1 次（可有可无）");
        System.out.println("  * *     -> 出现 0 次或多次（可以没有，也可以有无数个）");
        System.out.println("  * +     -> 出现 1 次或多次（至少得有 1 个，不能空着）");
        System.out.println("  * {n}   -> 必须正好出现 n 次");
        System.out.println("  * {n,m} -> 出现 n 到 m 次（比如 {6,16} 位密码）");

        // 举例 1: https? 中的 s? 说明 s 可以有也可以没有
        String url1 = "http://baidu.com";
        String url2 = "https://baidu.com";
        String regexHttp = "https?://.*";

        System.out.printf("  'http://' 匹配 'https?': %s%n", url1.matches(regexHttp));
        System.out.printf("  'https://' 匹配 'https?': %s%n", url2.matches(regexHttp));

        // 举例 2: \\d{6} 严格匹配 6 位邮政编码或验证码
        String code1 = "123456";
        String code2 = "12345";
        System.out.printf("  '123456' 是否刚好是 6 位数字(\\d{6}): %s%n", code1.matches("\\d{6}"));
        System.out.printf("  '12345'  是否刚好是 6 位数字(\\d{6}): %s%n", code2.matches("\\d{6}"));
    }

    /**
     * 第 4 关：边界符号 ^ 和 $（新手踩坑率 90% 的关键！）
     */
    private static void printStage4_Boundaries() {
        System.out.println("\n======================================================================");
        System.out.println("【第 4 关】：边界符 ^ 与 $（解决‘为什么包含非法字符却依然判断通过’的经典 Bug）");
        System.out.println("======================================================================");
        System.out.println("  * ^  -> 必须以它开头");
        System.out.println("  * $  -> 必须以它结尾");

        String input = "13800138000abcdef"; // 后面带了脏数据 abcdef
        String badRegex = "1[3-9]\\d{9}"; // 没加 ^ 和 $
        String goodRegex = "^1[3-9]\\d{9}$"; // 严格首尾锚定

        Pattern pBad = Pattern.compile(badRegex);
        Pattern pGood = Pattern.compile(goodRegex);

        System.out.printf("  测试脏输入: '%s'%n", input);
        System.out.printf("  不加 ^ 和 $ (部分子串包含即可): %s (错误！居然判定找到了有效号码)%n", pBad.matcher(input).find());
        System.out.printf("  加上 ^ 和 $ (全文严格匹配):     %s (正确！成功拦截带有尾缀脏字符的非法号码)%n", pGood.matcher(input).matches());
        System.out.println("  >>> 口诀: 表单数据全量校验时，务必头加 ^ 尾加 $！");
    }

    /**
     * 第 5 关：括号分组 () 与内容提取、脱敏替换（工作天天写）
     */
    private static void printStage5_GroupsAndExtract() {
        System.out.println("\n======================================================================");
        System.out.println("【第 5 关】：括号分组 () 与文本捕获/脱敏（实际项目中最常用）");
        System.out.println("======================================================================");
        System.out.println("  * 用小括号 () 把想拿出来的部分圈起来，叫做一个‘捕获组’");
        System.out.println("  * 第 1 个括号里的叫 group(1)，第 2 个叫 group(2)...");

        // 场景 1: 从日志里提取 IP、时间戳和访问状态码
        String logLine = "2026-11-04 10:30:00 [IP: 192.168.1.100] STATUS: 200 SUCCESS";
        String logRegex = "\\[IP:\\s*([0-9.]+)\\] STATUS:\\s*(\\d+)";

        Pattern p = Pattern.compile(logRegex);
        Matcher m = p.matcher(logLine);
        if (m.find()) {
            System.out.println("  原始日志: " + logLine);
            System.out.println("  提取第 1 组 (IP 地址): " + m.group(1));
            System.out.println("  提取第 2 组 (HTTP 状态码): " + m.group(2));
        }

        // 场景 2: 手机号隐私脱敏 (中间 4 位替换为 ****)
        String rawPhone = "13812345678";
        // 分成 3 组: 前 3 位(\\d{3}) + 中间 4 位(\\d{4}) + 后 4 位(\\d{4})
        // 替换表达式里 $1 代表第 1 组，$3 代表第 3 组
        String maskedPhone = rawPhone.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
        System.out.println("\n  手机号一键脱敏:");
        System.out.println("  原手机号: " + rawPhone + " -> 脱敏后: " + maskedPhone);
    }

    /**
     * 第 6 关：日常开发 4 大高频开箱即用规则清单
     */
    private static void printStage6_RealWorldUtils() {
        System.out.println("\n======================================================================");
        System.out.println("【第 6 关】：日常工作最常用的 4 大开箱即用模板速查");
        System.out.println("======================================================================");

        // 1. 大陆 11 位手机号 (1 开头，第二位是 3~9，后面跟着 9 位数字)
        String phoneRegex = "^1[3-9]\\d{9}$";
        System.out.printf("  1. 手机号校验 '13912345678': %s%n", "13912345678".matches(phoneRegex));
        System.out.printf("  1. 手机号校验 '12912345678': %s (第二位为2，非法)%n", "12912345678".matches(phoneRegex));

        // 2. 账号密码强弱 (6-16 位，必须包含字母和数字)
        String pwdRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,16}$";
        System.out.printf("  2. 强密码校验 'admin123': %s%n", "admin123".matches(pwdRegex));
        System.out.printf("  2. 弱密码校验 '123456':   %s (纯数字，不通过)%n", "123456".matches(pwdRegex));

        // 3. 中文字符匹配 ([\\u4e00-\\u9fa5] 是汉字 Unicode 编码范围)
        String chineseRegex = "^[\\u4e00-\\u9fa5]+$";
        System.out.printf("  3. 纯中文姓名校验 '张三丰': %s%n", "张三丰".matches(chineseRegex));
        System.out.printf("  3. 纯中文姓名校验 'Tony张': %s (含英文，不通过)%n", "Tony张".matches(chineseRegex));

        // 4. 正金钱金额 (最多保留两位小数)
        String moneyRegex = "^[0-9]+(\\.[0-9]{1,2})?$";
        System.out.printf("  4. 金额校验 '99.9':   %s%n", "99.9".matches(moneyRegex));
        System.out.printf("  4. 金额校验 '99.999': %s (超出两位小数，不通过)%n", "99.999".matches(moneyRegex));

        System.out.println("\n>>> 【学完总结口诀】：");
        System.out.println("    点号代表任意单，数字 \\d 字母 \\w；");
        System.out.println("    问号零一加至少，星号可有可没有；");
        System.out.println("    花括号内定次数，尖角开头美刀尾；");
        System.out.println("    圆括号内做分组，$1 $2 妙换位！");
    }
}
