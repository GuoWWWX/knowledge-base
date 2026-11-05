package com.knowledge.shortlink.util;

/**
 * Base62 编码转换工具类
 * 支持 10 进制无符号整数与 62 进制（0-9, a-z, A-Z）字符串互相转换
 * 62 进制字符集：62^6 ≈ 568 亿种组合，可完美满足百亿级短链发号需求
 */
public final class Base62Converter {

    /**
     * 62 进制标准字符表 (严格区分大小写)
     * 索引 0-9 对应 '0'-'9'
     * 索引 10-35 对应 'a'-'z'
     * 索引 36-61 对应 'A'-'Z'
     */
    private static final String BASE62_ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final char[] DIGITS = BASE62_ALPHABET.toCharArray();
    private static final int BASE = DIGITS.length; // 62

    private Base62Converter() {
        // 禁止外部实例化
    }

    /**
     * 将 10 进制正整数编码为 Base62 字符串（默认不足 6 位前面补 '0'）
     *
     * @param number 待编码数值（无符号处理）
     * @return 6 位 Base62 短码字符串
     */
    public static String toBase62(long number) {
        return toBase62(number, 6);
    }

    /**
     * 将 10 进制整数编码为固定最小长度的 Base62 字符串
     *
     * @param number    待编码数值
     * @param minLength 最小输出字符长度（不足补 '0'）
     * @return Base62 编码字符串
     */
    public static String toBase62(long number, int minLength) {
        if (number == 0) {
            return "0".repeat(Math.max(1, minLength));
        }

        StringBuilder sb = new StringBuilder();
        long n = number;
        while (n != 0) {
            int remainder = (int) Long.remainderUnsigned(n, BASE);
            sb.append(DIGITS[remainder]);
            n = Long.divideUnsigned(n, BASE);
        }

        while (sb.length() < minLength) {
            sb.append('0');
        }

        return sb.reverse().toString();
    }

    /**
     * 将 Base62 编码字符串解码还原为 10 进制 long 整数
     *
     * @param base62Str 62 进制短码
     * @return 原始 10 进制整数
     */
    public static long fromBase62(String base62Str) {
        if (base62Str == null || base62Str.isEmpty()) {
            throw new IllegalArgumentException("Base62 字符串不能为空");
        }

        long result = 0;
        for (int i = 0; i < base62Str.length(); i++) {
            char c = base62Str.charAt(i);
            int charIndex = BASE62_ALPHABET.indexOf(c);
            if (charIndex == -1) {
                throw new IllegalArgumentException("非法 Base62 字符: " + c);
            }
            result = result * BASE + charIndex;
        }
        return result;
    }
}
