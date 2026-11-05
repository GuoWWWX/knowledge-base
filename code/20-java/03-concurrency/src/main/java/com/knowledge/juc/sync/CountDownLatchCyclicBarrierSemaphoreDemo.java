package com.knowledge.juc.sync;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * JUC 三大核心同步辅助工具全景实战：
 * 1. CountDownLatch (倒计时门闩): 线程计数递减，主线程阻塞等待多个并行任务全部就绪（一次性不可重置）
 * 2. CyclicBarrier (循环栅栏): 多线程互相等待，达到屏障点时统一冲破并触发回调，支持循环复用 (分阶段作战)
 * 3. Semaphore (信号量): 基于 AQS 共享锁的并发流量控制与令牌租借（高并发保护有限资源）
 */
public class CountDownLatchCyclicBarrierSemaphoreDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. CountDownLatch: 分布式健康巡检等待全部就绪 ==========");
        demoCountDownLatch();

        System.out.println("\n========== 2. CyclicBarrier: 多线程分阶段同步集结 ==========");
        demoCyclicBarrier();

        System.out.println("\n========== 3. Semaphore: 核心有限数据库连接池并发流控 ==========");
        demoSemaphore();
    }

    private static void demoCountDownLatch() throws InterruptedException {
        int serviceCount = 3;
        CountDownLatch latch = new CountDownLatch(serviceCount);
        ExecutorService executor = Executors.newFixedThreadPool(serviceCount);

        String[] services = {"MySQL 数据源", "Redis 缓存集群", "RabbitMQ 消息代理"};
        for (String service : services) {
            executor.submit(() -> {
                try {
                    System.out.println("  -> 正在初始化子系统: " + service);
                    TimeUnit.MILLISECONDS.sleep(100);
                    System.out.println("  ✓ 子系统就绪: " + service);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        // 主线程阻塞等待全部完成
        boolean ready = latch.await(2, TimeUnit.SECONDS);
        executor.shutdown();
        System.out.println(">>> 主线程收拢: 所有子系统健康检查通过=" + ready + "，系统对外开放服务！");
    }

    private static void demoCyclicBarrier() throws Exception {
        int players = 3;
        // 3 个玩家都到达关卡后，自动触发屏障通关动作
        CyclicBarrier barrier = new CyclicBarrier(players, () -> {
            System.out.println("  ★ [全员集结完毕] 开启副本第一阶段首领 BOSS 战！");
        });

        ExecutorService executor = Executors.newFixedThreadPool(players);
        for (int i = 1; i <= players; i++) {
            final int id = i;
            executor.submit(() -> {
                try {
                    System.out.println("玩家 " + id + " 正在加载地图资源...");
                    TimeUnit.MILLISECONDS.sleep(id * 50L);
                    System.out.println("玩家 " + id + " 到达集结点，等待队友...");
                    barrier.await(); // 阻塞等待其他所有玩家到达
                    System.out.println("玩家 " + id + " 冲入 BOSS 战场战斗！");
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }

    private static void demoSemaphore() throws InterruptedException {
        // 允许最多 2 个并发访问
        Semaphore semaphore = new Semaphore(2);
        int clients = 5;
        ExecutorService executor = Executors.newFixedThreadPool(clients);

        for (int i = 1; i <= clients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    System.out.printf("请求者 %d 尝试租借数据库连接 (当前剩余许可: %d)...%n",
                            clientId, semaphore.availablePermits());
                    semaphore.acquire();
                    System.out.printf("  ✓ [成功持有连接] 请求者 %d 正在执行查询事务...%n", clientId);
                    TimeUnit.MILLISECONDS.sleep(150);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    System.out.printf("  - [释放连接] 请求者 %d 归还许可%n", clientId);
                    semaphore.release();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(2, TimeUnit.SECONDS);
    }
}
