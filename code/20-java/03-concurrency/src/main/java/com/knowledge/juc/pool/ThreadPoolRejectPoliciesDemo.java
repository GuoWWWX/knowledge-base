package com.knowledge.juc.pool;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池四大饱和拒绝策略（RejectedExecutionHandler）深度对比演示
 *
 * 核心原理说明：
 * 线程池任务提交流程：
 * 1. 核心线程数 corePoolSize 未满 -> 创建核心线程执行任务；
 * 2. 核心线程数已满 -> 任务压入工作队列 workQueue；
 * 3. 工作队列已满 -> 判断最大线程数 maximumPoolSize，未满则创建临时非核心线程执行；
 * 4. 最大线程数也已满 -> 触发拒绝策略 RejectedExecutionHandler。
 */
public class ThreadPoolRejectPoliciesDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===============================================================");
        System.out.println("          JUC 线程池四大拒绝策略执行行为与场景深度对比验证           ");
        System.out.println("===============================================================\n");

        testAbortPolicy();
        TimeUnit.MILLISECONDS.sleep(800);

        testCallerRunsPolicy();
        TimeUnit.MILLISECONDS.sleep(800);

        testDiscardPolicy();
        TimeUnit.MILLISECONDS.sleep(800);

        testDiscardOldestPolicy();
        TimeUnit.MILLISECONDS.sleep(800);

        printSummaryTable();
    }

    /**
     * 1. AbortPolicy（默认策略）：直接抛出 RejectedExecutionException 异常，中断提交流程
     */
    private static void testAbortPolicy() {
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【策略 1：AbortPolicy (默认)】-> 拒绝并抛出 RejectedExecutionException");
        ThreadPoolExecutor executor = createTestPool(new ThreadPoolExecutor.AbortPolicy());

        try {
            // 容量：2 (工作线程) + 2 (队列) = 4。提交 5 个任务，第 5 个必触发拒绝
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                System.out.printf("[提交任务 %d] 准备提交到线程池...\n", taskId);
                executor.execute(createTask(taskId, 500));
            }
        } catch (RejectedExecutionException e) {
            System.err.printf("  >>> [捕获异常] 任务触发 AbortPolicy 拒绝: %s\n", e.getMessage());
        } finally {
            shutdownPool(executor);
        }
    }

    /**
     * 2. CallerRunsPolicy（调用者运行）：让提交任务的线程（调用方）亲自同步执行该任务
     * 特点：天然形成“反压（Backpressure）机制”，减缓上游任务生产速度。
     */
    private static void testCallerRunsPolicy() {
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【策略 2：CallerRunsPolicy】-> 由提交任务的主线程亲自执行，提供反压降速");
        ThreadPoolExecutor executor = createTestPool(new ThreadPoolExecutor.CallerRunsPolicy());

        try {
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                System.out.printf("[提交任务 %d] 准备提交到线程池...\n", taskId);
                executor.execute(createTask(taskId, 300));
            }
        } finally {
            shutdownPool(executor);
        }
    }

    /**
     * 3. DiscardPolicy（静默丢弃）：直接丢弃无法处理的任务，不抛异常也不执行
     * 适用场景：允许数据丢失的监控指标上报、日志采集等。
     */
    private static void testDiscardPolicy() {
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【策略 3：DiscardPolicy】-> 静默丢弃多余任务，调用方无感知");
        ThreadPoolExecutor executor = createTestPool(new ThreadPoolExecutor.DiscardPolicy());

        try {
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                System.out.printf("[提交任务 %d] 准备提交到线程池...\n", taskId);
                executor.execute(createTask(taskId, 300));
            }
        } finally {
            shutdownPool(executor);
        }
    }

    /**
     * 4. DiscardOldestPolicy（丢弃最老任务）：丢弃队列首部等待时间最长的任务，并尝试重新提交当前任务
     * 适用场景：对最新数据更敏感，允许丢弃较老滞留数据的场景（如实时股票行情更新）。
     */
    private static void testDiscardOldestPolicy() {
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【策略 4：DiscardOldestPolicy】-> 丢弃队列首部等待最久的任务，执行最新任务");
        ThreadPoolExecutor executor = createTestPool(new ThreadPoolExecutor.DiscardOldestPolicy());

        try {
            // 提交 1、2：立即被 2 个核心线程拿去执行 (运行中)
            // 提交 3、4：进入队列 (3 是队列头部的老任务，4 在其后)
            // 提交 5：触发拒绝策略，弹出并丢弃队头任务 3，然后尝试执行任务 5
            for (int i = 1; i <= 5; i++) {
                final int taskId = i;
                System.out.printf("[提交任务 %d] 提交入池...\n", taskId);
                executor.execute(createTask(taskId, 300));
            }
        } finally {
            shutdownPool(executor);
        }
    }

    // ======================= 工具辅助方法 =======================

    private static final AtomicInteger threadIndex = new AtomicInteger(1);

    /**
     * 创建一个容量非常可控的线程池：
     * 核心线程数: 2，最大线程数: 2，有界队列容量: 2
     * 最大并发负荷承载量 = 2 + 2 = 4 个任务
     */
    private static ThreadPoolExecutor createTestPool(RejectedExecutionHandler handler) {
        return new ThreadPoolExecutor(
                2,
                2,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2),
                r -> new Thread(r, "pool-worker-" + threadIndex.getAndIncrement()),
                handler
        );
    }

    private static Runnable createTask(int taskId, long durationMs) {
        return () -> {
            String currentThread = Thread.currentThread().getName();
            System.out.printf("    ▶ [执行开始] 任务 %d 正在由线程 [%s] 执行...\n", taskId, currentThread);
            try {
                TimeUnit.MILLISECONDS.sleep(durationMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.printf("    ✔ [执行完成] 任务 %d 执行完毕 (线程: %s)\n", taskId, currentThread);
        };
    }

    private static void shutdownPool(ThreadPoolExecutor executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static void printSummaryTable() {
        System.out.println("\n===============================================================");
        System.out.println("                     四大拒绝策略架构选型一览表                 ");
        System.out.println("===============================================================");
        System.out.println("| 拒绝策略            | 处理机制                                 | 典型适用场景                 |");
        System.out.println("|--------------------|------------------------------------------|----------------------------|");
        System.out.println("| AbortPolicy        | 抛出 RejectedExecutionException           | 金融交易、核心强感知流程     |");
        System.out.println("| CallerRunsPolicy   | 调用者主线程执行，形成天然反压            | 允许降速吞吐、任务不可丢失   |");
        System.out.println("| DiscardPolicy      | 悄无声息丢弃新任务，不产生任何异常       | 监控心跳上报、非关键日志收集 |");
        System.out.println("| DiscardOldestPolicy| 丢弃队列首部等待最久的任务，尝试执行新任务 | 实时行情发布、快照最新状态   |");
        System.out.println("===============================================================\n");
    }
}
