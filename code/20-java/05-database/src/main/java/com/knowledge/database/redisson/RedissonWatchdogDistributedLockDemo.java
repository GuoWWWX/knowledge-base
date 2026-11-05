package com.knowledge.database.redisson;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 实验：Redisson 分布式锁看门狗（Watchdog）自动续期与互斥排他实测
 * 对应知识点：分布式锁超时丢失、看门狗后台心跳续期机制、可重入特性与优雅解锁
 */
public class RedissonWatchdogDistributedLockDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("【Lab 05-3】Redisson 分布式锁看门狗（Watchdog）续期与互斥实测");
        System.out.println("================================================================================\n");

        RedissonClient redisson = null;
        try {
            Config config = new Config();
            config.useSingleServer()
                    .setAddress("redis://127.0.0.1:6379")
                    .setConnectTimeout(1000)
                    .setTimeout(1000);
            redisson = Redisson.create(config);
            // 测试连接连通性
            redisson.getKeys().count();
            System.out.println("✔ 成功直连本地 Docker Redis (6379) 集群，正在运行真实分布式锁演练...\n");
            runRealRedissonWatchdog(redisson);
        } catch (Exception e) {
            System.out.println("⚠ 连接本地 Redis 出现超时或异常 (" + e.getMessage() + ")，自动切换为纯内存仿真 Watchdog 引擎演练...\n");
            runSimulatedWatchdogDemo();
        } finally {
            if (redisson != null) {
                redisson.shutdown();
            }
        }
    }

    private static void runRealRedissonWatchdog(RedissonClient redisson) throws InterruptedException {
        RLock lock = redisson.getLock("lock:order:1001");
        CountDownLatch latch = new CountDownLatch(2);

        // 线程 1: 获取无租期的分布式锁 (触发默认 30 秒看门狗自动延期)
        Thread t1 = new Thread(() -> {
            try {
                System.out.println("🔹 [Worker-1] 正在尝试加锁 (lock.lock())...");
                lock.lock(); // 未传 leaseTime，触发看门狗 Watchdog (默认每 10 秒自动续期成 30 秒)
                System.out.println("✔ [Worker-1] 成功持有分布式锁！当前重入次数: " + lock.getHoldCount());

                // 演示锁重入
                lock.lock();
                System.out.println("✔ [Worker-1] 第二次重入成功！当前重入次数: " + lock.getHoldCount());

                System.out.println("🔹 [Worker-1] 模拟耗时业务，持续占用锁 1.5 秒...");
                Thread.sleep(1500);

                lock.unlock(); // 释放一层重入
                System.out.println("✔ [Worker-1] 释放一层锁，当前剩余重入次数: " + lock.getHoldCount());
                lock.unlock(); // 完全释放
                System.out.println("✔ [Worker-1] 完全释放锁！看门狗后台续期任务自动注销。");
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }, "Worker-1");

        // 线程 2: 并发争抢锁，演示互斥等待
        Thread t2 = new Thread(() -> {
            try {
                Thread.sleep(300); // 稍慢于 Worker-1
                System.out.println("🔸 [Worker-2] 尝试在 3 秒内获取锁 (tryLock)...");
                boolean acquired = lock.tryLock(3, 2, TimeUnit.SECONDS);
                if (acquired) {
                    try {
                        System.out.println("✔ [Worker-2] 在 Worker-1 释放后成功抢占到锁！");
                    } finally {
                        lock.unlock();
                        System.out.println("✔ [Worker-2] 业务执行完毕，安全释放锁！");
                    }
                } else {
                    System.err.println("❌ [Worker-2] 超时未抢到锁，执行降级逻辑！");
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }, "Worker-2");

        t1.start();
        t2.start();
        latch.await();
    }

    /**
     * 仿真看门狗机制
     */
    private static void runSimulatedWatchdogDemo() throws InterruptedException {
        ScheduledExecutorService watchdogTimer = Executors.newSingleThreadScheduledExecutor();
        AtomicBoolean lockHeld = new AtomicBoolean(true);

        System.out.println("🔹 [仿真看门狗] 启动后台心跳续期守护任务 (每隔 lockWatchdogTimeout / 3 = 10s 续期一次)...");
        watchdogTimer.scheduleAtFixedRate(() -> {
            if (lockHeld.get()) {
                System.out.println("🔄 [Watchdog 心跳探测] 业务线程仍在执行，向 Redis 发送 Lua 脚本续期 EXPIRE lockKey 30s 成功！");
            }
        }, 100, 300, TimeUnit.MILLISECONDS);

        Thread.sleep(1000);
        lockHeld.set(false);
        watchdogTimer.shutdown();
        System.out.println("✔ [仿真看门狗] 业务主动调用 unlock()，取消定时续期器并安全释放锁！\n");
    }
}
