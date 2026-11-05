package com.knowledge.shortlink.util;

import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;

/**
 * 哈希计算工具类
 * 基于 Google Guava 的 MurmurHash3 高性能非加密哈希算法
 * MurmurHash3 具有计算速度极快、雪崩效应好、散列分布高度均匀的优良特性
 */
public final class HashUtil {

    private HashUtil() {
        // 禁止实例化
    }

    /**
     * 使用 MurmurHash3 32位算法将字符串哈希为 32 位无符号正整数
     *
     * @param input 原始文本（如长链接 URL）
     * @return 32 位无符号 long 整数 [0, 2^32 - 1]
     */
    public static long murmur32AsUnsigned(String input) {
        if (input == null) {
            throw new IllegalArgumentException("哈希计算输入不可为空");
        }
        int hash32 = Hashing.murmur3_32_fixed().hashString(input, StandardCharsets.UTF_8).asInt();
        return Integer.toUnsignedLong(hash32);
    }

    /**
     * 带加盐后缀的 MurmurHash3 计算（用于解决哈希碰撞时加盐重散列）
     *
     * @param input 原始长链接
     * @param salt  混淆盐值 / 递增重试序列
     * @return 32 位无符号 long 整数
     */
    public static long murmur32WithSalt(String input, int salt) {
        return murmur32AsUnsigned(input + "[COLLISION_SALT_" + salt + "]");
    }
}
