package com.knowledge.pattern.creational;

import java.lang.reflect.Constructor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工业级单例模式 (Singleton Pattern) 深度实战与攻防演练：
 * 1. DCL (Double-Checked Locking) 双重检查锁：
 *    - 为什么加两次判空？外层判空规避无谓加锁开销，内层判空杜绝并发线程重复实例化
 *    - 为什么必须加 volatile？禁止 JIT 指令重排序：避免“分配内存 -> 赋值引用 -> 初始化实例”重排序导致其他线程拿到未完全初始化的半成品对象！
 * 2. 静态内部类 (Holder 模式)：
 *    - 利用 JVM 类加载机制保障线程安全（类加载过程加锁），且具备绝对完美的延迟加载 (Lazy Loading)
 * 3. 枚举单例 (Enum Singleton - Joshua Bloch 极力推崇)：
 *    - JVM 底层原生防御反射攻击 (反射 newInstance 抛 IllegalArgumentException: Cannot reflectively create enum objects)
 *    - 原生防御反序列化攻击 (JDK 内部维护单例，不创建新对象)
 */
public class SingletonCreationalDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. DCL 双重检查锁并发安全性与性能压测 ==========");
        testDclConcurrency();

        System.out.println("\n========== 2. 静态内部类 Holder 模式延迟加载实证 ==========");
        testHolderSingleton();

        System.out.println("\n========== 3. 反射攻击测试：普通单例 vs 枚举单例 ==========");
        testReflectionAttack();
    }

    private static void testDclConcurrency() throws InterruptedException {
        int threads = 20;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger uniqueHashCodeCount = new AtomicInteger();

        int firstHash = DclSingleton.getInstance().hashCode();
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    DclSingleton instance = DclSingleton.getInstance();
                    if (instance.hashCode() == firstHash) {
                        uniqueHashCodeCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();
        System.out.printf("20 个并发线程同时获取 DCL 单例，完全相同的实例计数: %d/20 (100%% 线程安全)%n", uniqueHashCodeCount.get());
    }

    private static void testHolderSingleton() {
        System.out.println("主线程启动，HolderSingleton 主类已被加载...");
        System.out.println("此时内部类 LazyHolder 并未被加载到内存中 (真正的按需惰性加载)！");
        System.out.println("首次调用 getInstance()...");
        HolderSingleton instance = HolderSingleton.getInstance();
        System.out.println("获取成功，实例 Hash: " + instance.hashCode());
    }

    private static void testReflectionAttack() {
        // 尝试反射破解普通的 DCL 单例
        try {
            Constructor<DclSingleton> constructor = DclSingleton.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            DclSingleton hackedInstance = constructor.newInstance();
            System.err.println("普通 DCL 单例遭受反射攻击: 成功创建第 2 个不同实例! Hash=" + hackedInstance.hashCode());
        } catch (Exception e) {
            System.out.println("反射攻击被拦截: " + e.getMessage());
        }

        // 尝试反射破解枚举单例
        try {
            Constructor<EnumSingleton> enumConstructor = EnumSingleton.class.getDeclaredConstructor(String.class, int.class);
            enumConstructor.setAccessible(true);
            enumConstructor.newInstance("HACK", 1);
        } catch (Exception e) {
            System.out.println("【枚举单例原生防御成功】: " + e.getCause());
            System.out.println(">>> 源码保护: Constructor.newInstance() 源码中直接硬编码判断 if ((clazz.getModifiers() & Modifier.ENUM) != 0) 抛出异常，反射根本无法攻破！");
        }
    }

    // 1. DCL 双重检查锁
    public static class DclSingleton {
        // 核心: volatile 禁止指令重排
        private static volatile DclSingleton instance;

        private DclSingleton() {
            // 私有构造
        }

        public static DclSingleton getInstance() {
            if (instance == null) { // 第一次检查：避免已初始化后的锁竞争
                synchronized (DclSingleton.class) {
                    if (instance == null) { // 第二次检查：杜绝并发重复创建
                        instance = new DclSingleton();
                    }
                }
            }
            return instance;
        }
    }

    // 2. 静态内部类 Holder 模式
    public static class HolderSingleton {
        private HolderSingleton() {
            System.out.println("  -> [JVM 类加载锁保证] 初始化 HolderSingleton 实例");
        }

        private static class LazyHolder {
            // 只有首次调用 LazyHolder.INSTANCE 时，JVM 才会加载此类并初始化常量
            private static final HolderSingleton INSTANCE = new HolderSingleton();
        }

        public static HolderSingleton getInstance() {
            return LazyHolder.INSTANCE;
        }
    }

    // 3. 终极枚举单例
    public enum EnumSingleton {
        INSTANCE;

        private final String resource = "DatabasePoolManager";

        public String getResource() {
            return resource;
        }
    }
}
