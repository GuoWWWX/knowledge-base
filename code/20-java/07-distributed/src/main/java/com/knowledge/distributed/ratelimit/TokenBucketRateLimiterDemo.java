package com.knowledge.distributed.ratelimit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 实验：高性能无锁令牌桶限流器（Token Bucket）手写与突发流量压测实战
 * 特点：基于惰性计算（Lazy Evaluation）消除定时线程开销，CAS 无锁保证并发吞吐
 */
public class TokenBucketRateLimiterDemo {

    public static class SimpleTokenBucket {
        private final long capacity;       // 桶容量 (允许应对的最大突发流量)
        private final double refillRate;    // 每毫秒生成的令牌速率 (tokens per ms)

        private final AtomicLong storedTokens; // 当前可用令牌数 (放大 1000 倍以支持整数原子操作)
        private final AtomicLong lastRefillTimestamp;

        public SimpleTokenBucket(long capacity, double permitsPerSecond) {
            this.capacity = capacity * 1000L;
            this.refillRate = permitsPerSecond / 1000.0;
            this.storedTokens = new AtomicLong(this.capacity);
            this.lastRefillTimestamp = new AtomicLong(System.currentTimeMillis());
        }

        /**
         * 尝试获取一个令牌 (非阻塞)
         */
        public boolean tryAcquire() {
            refill();
            while (true) {
                long current = storedTokens.get();
                if (current < 1000L) {
                    return false; // 令牌不足，直接限流拒绝
                }
                if (storedTokens.compareAndSet(current, current - 1000L)) {
                    return true;  // 成功消耗一个令牌
                }
            }
        }

        /**
         * 惰性填充令牌：根据时间差动态增加令牌
         */
        private void refill() {
            long now = System.currentTimeMillis();
            long last = lastRefillTimestamp.get();
            if (now > last) {
                if (lastRefillTimestamp.compareAndSet(last, now)) {
                    long durationMs = now - last;
                    long newTokens = (long) (durationMs * refillRate * 1000L);
                    if (newTokens > 0) {
                        storedTokens.updateAndGet(curr -> Math.min(capacity, curr + newTokens));
                    }
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("【Lab 07-2】高性能无锁令牌桶限流器（Token Bucket）并发压测实战");
        System.out.println("================================================================================\n");

        // 构造：桶容量 10（允许突发 10 个请求），每秒平滑恢复 20 个令牌 (20 QPS)
        SimpleTokenBucket limiter = new SimpleTokenBucket(10, 20);

        int totalRequests = 50;
        CountDownLatch latch = new CountDownLatch(totalRequests);
        AtomicInteger passedCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        System.out.println("测试场景：限制 20 QPS，桶容量 10。瞬时发射 " + totalRequests + " 个高并发请求冲击限流器...\n");

        for (int i = 1; i <= totalRequests; i++) {
            final int reqId = i;
            new Thread(() -> {
                boolean passed = limiter.tryAcquire();
                if (passed) {
                    passedCount.incrementAndGet();
                    System.out.println("✔ [请求放行] Request-" + reqId + " 成功获取到令牌，执行后端业务！");
                } else {
                    rejectedCount.incrementAndGet();
                    System.err.println("❌ [限流拦截] Request-" + reqId + " 无可用令牌，触发 HTTP 429 Too Many Requests！");
                }
                latch.countDown();
            }).start();
        }

        latch.await();

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("【压测统计结论】");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("总并发请求: " + totalRequests + " 次");
        System.out.println("成功放行数: " + passedCount.get() + " 次 (容量上限 10 个瞬间放行)");
        System.out.println("限流阻断数: " + rejectedCount.get() + " 次 (拦截率: " + ((double) rejectedCount.get() / totalRequests * 100) + "%)");
        System.out.println("令牌桶特性：支持一定程度的突发流量应对，相比漏桶（强制恒定流出速率）更适合弹性 Web 业务。\n");
    }
}
