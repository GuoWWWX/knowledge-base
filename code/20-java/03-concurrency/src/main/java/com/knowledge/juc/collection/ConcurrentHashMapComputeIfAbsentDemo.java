package com.knowledge.juc.collection;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ConcurrentHashMap 高级并发实战：
 * 1. 传统 check-then-act (先 get 后 put) 复合操作非原子性导致的重复初始化 Bug
 * 2. computeIfAbsent 底层 CAS + synchronized 桶锁的原子性保障
 * 3. 规避 computeIfAbsent 递归嵌套调用同一 Map 引发线程活锁死循环的生产陷阱 (JDK-8161372)
 */
public class ConcurrentHashMapComputeIfAbsentDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 1. 传统 get-then-put 导致昂贵资源被多次重复初始化 ==========");
        simulateCheckThenActFlaw();

        System.out.println("\n========== 2. computeIfAbsent 保证绝对原子性单次初始化 ==========");
        simulateAtomicComputeIfAbsent();

        System.out.println("\n========== 3. computeIfAbsent 递归死锁陷阱警示 ==========");
        System.out.println("【避坑警示】: 绝不可在 computeIfAbsent 的 mappingFunction 中递归调用同一 Map 的 compute/put！");
        System.out.println("因为内部已经对桶节点施加了 synchronized 锁，同一个桶内的递归操作会导致线程自我死锁或抛出 IllegalStateException！");
    }

    private static void simulateCheckThenActFlaw() throws InterruptedException {
        ConcurrentHashMap<String, ExpensiveObject> map = new ConcurrentHashMap<>();
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger initCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    String key = "DB_POOL";
                    // 典型错误代码：非原子组合
                    ExpensiveObject obj = map.get(key);
                    if (obj == null) {
                        obj = new ExpensiveObject(key, initCount);
                        map.put(key, obj);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();
        System.err.println("非原子 get-then-put 下昂贵资源实际被重复初始化次数: " + initCount.get() + " (严重并发浪费与连接泄漏！)");
    }

    private static void simulateAtomicComputeIfAbsent() throws InterruptedException {
        ConcurrentHashMap<String, ExpensiveObject> map = new ConcurrentHashMap<>();
        int threads = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger initCount = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    String key = "DB_POOL";
                    // 优雅原子计算：底层对桶头节点加锁，MappingFunction 仅由胜出的单个线程执行一次
                    map.computeIfAbsent(key, k -> new ExpensiveObject(k, initCount));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        pool.shutdown();
        System.out.println("computeIfAbsent 保证昂贵资源实际初始化次数: " + initCount.get() + " (严格 1 次，绝对幂等原子)");
    }

    static class ExpensiveObject {
        public ExpensiveObject(String name, AtomicInteger counter) {
            counter.incrementAndGet();
            // 模拟耗时的建连与内存分配
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
        }
    }
}
