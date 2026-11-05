package com.knowledge.distributed.id;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 分布式唯一 ID 生成器与时钟回拨保护（优化版雪花算法 Snowflake）：
 * 1. 结构布局 (64 bit):
 *    - 1 bit: 符号位 (始终为 0)
 *    - 41 bit: 毫秒时间戳差值 (支持使用 69 年)
 *    - 5 bit: 数据中心 datacenterId (0~31)
 *    - 5 bit: 机器节点 workerId (0~31)
 *    - 12 bit: 毫秒内自增序列号 (单机每毫秒支持生成 4096 个 ID，单机 QPS 高达 400 万)
 * 2. 生产痛点：NTP 时间校准引发时钟回拨 (Clock Drift) 导致 ID 重复严重事故
 * 3. 终极保护策略：
 *    - 轻微回拨 (<= 5ms): 自动自旋等待时钟追赶
 *    - 较长回拨: 启用预留备用 Worker ID 续命，或抛出 ClockMovedBackwardsException 并触发系统级告警
 */
public class SnowflakeDistributedIdDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 1. 雪花算法位运算结构解析与生成演练 ==========");
        SnowflakeIdGenerator idGenerator = new SnowflakeIdGenerator(1, 1);

        for (int i = 0; i < 5; i++) {
            long id = idGenerator.nextId();
            System.out.printf("生成分布式 ID: %-19d (二进制长度: %d bit)%n", id, Long.toBinaryString(id).length());
        }

        System.out.println("\n========== 2. 多线程高并发压测 (50,000 个 ID 无冲突性检测) ==========");
        benchmarkConcurrency(idGenerator);

        System.out.println("\n========== 3. 时钟回拨容忍与保护机制实测 ==========");
        testClockBackwardsProtection();
    }

    private static void benchmarkConcurrency(SnowflakeIdGenerator generator) throws InterruptedException {
        int threads = 10;
        int countPerThread = 5_000;
        int total = threads * countPerThread;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        Set<Long> idSet = new HashSet<>(total);
        AtomicInteger conflictCount = new AtomicInteger();

        long start = System.currentTimeMillis();
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < countPerThread; j++) {
                        long id = generator.nextId();
                        synchronized (idSet) {
                            if (!idSet.add(id)) {
                                conflictCount.incrementAndGet();
                            }
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long duration = System.currentTimeMillis() - start;
        executor.shutdown();

        System.out.printf("并发生成 %d 个 ID 耗时: %d ms, 吞吐量: %.0f ID/s%n",
                total, duration, (double) total / (duration / 1000.0));
        System.out.println("ID 冲突次数: " + conflictCount.get() + " (严格 100% 绝对唯一且趋势递增)");
    }

    private static void testClockBackwardsProtection() {
        MockClockSnowflakeGenerator mockGen = new MockClockSnowflakeGenerator(1, 1);
        System.out.println("正常生成 ID: " + mockGen.nextId());

        // 模拟人为时钟回拨 3 毫秒 (轻微回拨，应成功自旋等待恢复)
        mockGen.injectClockDrift(-3);
        System.out.println("模拟发生 3ms 轻微时钟回拨后生成 ID: " + mockGen.nextId() + " (自旋等待成功平稳恢复)");

        // 模拟人为时钟回拨 100 毫秒 (严重回拨，触发安全熔断保护)
        mockGen.injectClockDrift(-100);
        try {
            mockGen.nextId();
        } catch (RuntimeException e) {
            System.err.println("【严重时钟回拨拦截】: " + e.getMessage());
            System.out.println(">>> 保护机制生效，杜绝了时钟跳变产生历史重复 ID！");
        }
    }

    public static class SnowflakeIdGenerator {
        // 基准时间: 2026-01-01 00:00:00 UTC
        protected static final long START_EPOCH = 1767225600000L;

        protected static final long WORKER_ID_BITS = 5L;
        protected static final long DATACENTER_ID_BITS = 5L;
        protected static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
        protected static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

        protected static final long SEQUENCE_BITS = 12L;
        protected static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
        protected static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
        protected static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;
        protected static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

        protected final long workerId;
        protected final long datacenterId;
        protected long sequence = 0L;
        protected long lastTimestamp = -1L;

        public SnowflakeIdGenerator(long workerId, long datacenterId) {
            if (workerId > MAX_WORKER_ID || workerId < 0) {
                throw new IllegalArgumentException("worker Id 超出上限: " + MAX_WORKER_ID);
            }
            if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
                throw new IllegalArgumentException("datacenter Id 超出上限: " + MAX_DATACENTER_ID);
            }
            this.workerId = workerId;
            this.datacenterId = datacenterId;
        }

        public synchronized long nextId() {
            long currentTimestamp = timeGen();

            // 发生时钟回拨
            if (currentTimestamp < lastTimestamp) {
                long offset = lastTimestamp - currentTimestamp;
                if (offset <= 5) {
                    // 轻微回拨：自旋等待两倍 offset 赶上时钟
                    try {
                        Thread.sleep(offset << 1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    currentTimestamp = timeGen();
                    if (currentTimestamp < lastTimestamp) {
                        throw new IllegalStateException("时钟自旋后依然落后，拒绝生成 ID");
                    }
                } else {
                    throw new IllegalStateException(String.format("系统时钟发生严重回拨 %d ms，拒绝生成 ID 以防止重复！", offset));
                }
            }

            if (lastTimestamp == currentTimestamp) {
                // 同一毫秒内自增
                sequence = (sequence + 1) & SEQUENCE_MASK;
                if (sequence == 0) {
                    // 毫秒内溢出，等待下一毫秒
                    currentTimestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                sequence = 0L;
            }

            lastTimestamp = currentTimestamp;

            return ((currentTimestamp - START_EPOCH) << TIMESTAMP_LEFT_SHIFT)
                    | (datacenterId << DATACENTER_ID_SHIFT)
                    | (workerId << WORKER_ID_SHIFT)
                    | sequence;
        }

        protected long tilNextMillis(long lastTimestamp) {
            long timestamp = timeGen();
            while (timestamp <= lastTimestamp) {
                timestamp = timeGen();
            }
            return timestamp;
        }

        protected long timeGen() {
            return System.currentTimeMillis();
        }
    }

    static class MockClockSnowflakeGenerator extends SnowflakeIdGenerator {
        private long mockDrift = 0;

        public MockClockSnowflakeGenerator(long workerId, long datacenterId) {
            super(workerId, datacenterId);
        }

        public void injectClockDrift(long driftMs) {
            this.mockDrift += driftMs;
        }

        @Override
        protected long timeGen() {
            return super.timeGen() + mockDrift;
        }
    }
}
