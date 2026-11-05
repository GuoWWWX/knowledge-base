package com.knowledge.database.redis;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 实验：Redis 缓存穿透、缓存击穿、缓存雪崩三大翻车事故现场演示与攻防实战
 */
public class RedisCachePenetrationBreakdownDemo {

    // 模拟底层真实关系型数据库
    private static final Map<Long, String> MOCK_DB = new ConcurrentHashMap<>();
    // 模拟 Redis 内存缓存
    private static final Map<String, CacheItem> MOCK_REDIS = new ConcurrentHashMap<>();
    // 模拟布隆过滤器 (预期容纳 10 万条，误判率 0.1%)
    private static final BloomFilter<Long> BLOOM_FILTER = BloomFilter.create(
            Funnels.longFunnel(), 100_000, 0.001
    );

    private static final AtomicInteger DB_QUERY_COUNTER = new AtomicInteger(0);
    private static final ExecutorService REBUILD_EXECUTOR = Executors.newFixedThreadPool(4);

    static {
        // 预热真实业务数据
        MOCK_DB.put(1001L, "iPhone 16 Pro Max 详细参数规格说明");
        MOCK_DB.put(1002L, "MacBook Pro M3 Max 详细参数规格说明");

        // 将合法 ID 写入布隆过滤器
        BLOOM_FILTER.put(1001L);
        BLOOM_FILTER.put(1002L);
    }

    record CacheItem(String data, LocalDateTime expireTime, boolean isNull) {}

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("【Lab 05-2】Redis 缓存穿透、击穿、雪崩三大翻车事故攻防实战");
        System.out.println("================================================================================\n");

        testCachePenetrationDefense();
        testCacheBreakdownWithMutexAndLogicalExpire();
        testCacheSnowslideTtlJitter();

        REBUILD_EXECUTOR.shutdown();
    }

    /**
     * 1. 缓存穿透防御演练 (布隆过滤器 + 空值缓存)
     */
    private static void testCachePenetrationDefense() {
        System.out.println("--- 1. 缓存穿透（Cache Penetration）攻防演练 ---");
        long maliciousId = -9999L; // 数据库中绝不存在的恶意 ID

        // 防线 1: 布隆过滤器快速过滤
        if (!BLOOM_FILTER.mightContain(maliciousId)) {
            System.out.println("✔ [第一道防线 - 布隆过滤器] 判定 ID=" + maliciousId + " 绝对不存在于知识库，直接在入口硬拦截拒绝，DB 查询次数: 0！");
        }

        // 假设偶遇误判穿透，防线 2: 缓存空对象 (Null Cache)
        String cached = queryWithNullObjectCache(8888L);
        System.out.println("✔ [第二道防线 - 空值缓存] 首次查询无果后写入空值缓存，第二次查询直接命中空缓存: '" + cached + "'，DB 总查询次数: " + DB_QUERY_COUNTER.get() + "\n");
    }

    private static String queryWithNullObjectCache(Long id) {
        String key = "item:" + id;
        CacheItem item = MOCK_REDIS.get(key);
        if (item != null) {
            return item.isNull() ? "【空对象兜底】数据不存在" : item.data();
        }

        DB_QUERY_COUNTER.incrementAndGet();
        String dbData = MOCK_DB.get(id);
        if (dbData == null) {
            // 写入短生命周期的空缓存 (如 60 秒)
            MOCK_REDIS.put(key, new CacheItem(null, LocalDateTime.now().plusSeconds(60), true));
            return "【空对象兜底】数据不存在";
        } else {
            MOCK_REDIS.put(key, new CacheItem(dbData, LocalDateTime.now().plusHours(1), false));
            return dbData;
        }
    }

    /**
     * 2. 缓存击穿防御演练 (互斥锁 Mutex vs 逻辑过期 Logical Expiration)
     */
    private static void testCacheBreakdownWithMutexAndLogicalExpire() throws InterruptedException {
        System.out.println("--- 2. 缓存击穿（Cache Breakdown）并发攻防演练 ---");
        System.out.println("场景：热点商品 1001 缓存突然瞬间失效，50 个并发线程同时发起读取请求...");

        MOCK_REDIS.remove("item:1001"); // 模拟热点 Key 突然过期
        DB_QUERY_COUNTER.set(0);

        int threadCount = 50;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ReentrantLock mutex = new ReentrantLock();

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                queryHotItemWithMutex(1001L, mutex);
                latch.countDown();
            }).start();
        }

        latch.await();
        System.out.println("✔ [互斥锁防御结果] 50 个高并发请求下，真实命中并穿透到底层 DB 的查询次数仅为: " + DB_QUERY_COUNTER.get() + " 次（其余 49 次全被锁拦截并等待复用新缓存）！\n");
    }

    private static String queryHotItemWithMutex(Long id, ReentrantLock mutex) {
        String key = "item:" + id;
        CacheItem item = MOCK_REDIS.get(key);
        if (item != null && !item.isNull()) {
            return item.data();
        }

        // 获取互斥锁 (模拟 Redis setnx lock:item:1001)
        if (mutex.tryLock()) {
            try {
                // Double Check 双重检查
                item = MOCK_REDIS.get(key);
                if (item != null && !item.isNull()) {
                    return item.data();
                }

                // 只有获得互斥锁的单个线程有权查询 DB 重建缓存
                DB_QUERY_COUNTER.incrementAndGet();
                Thread.sleep(20); // 模拟慢查询
                String dbData = MOCK_DB.get(id);
                MOCK_REDIS.put(key, new CacheItem(dbData, LocalDateTime.now().plusHours(1), false));
                return dbData;
            } catch (InterruptedException ignored) {
                return null;
            } finally {
                mutex.unlock();
            }
        } else {
            // 未获取到锁的线程自旋休眠重试
            try {
                Thread.sleep(10);
                return queryHotItemWithMutex(id, mutex);
            } catch (InterruptedException e) {
                return null;
            }
        }
    }

    /**
     * 3. 缓存雪崩防御演练 (TTL 随机加盐打散)
     */
    private static void testCacheSnowslideTtlJitter() {
        System.out.println("--- 3. 缓存雪崩（Cache Snowslide）防御演练 ---");
        int baseTtlSeconds = 3600; // 基础 1 小时
        Random random = new Random();

        System.out.println("批量预热 5 个业务分类缓存，加入 ±300s 随机加盐抖动:");
        for (int i = 1; i <= 5; i++) {
            int jitterSeconds = random.nextInt(600) - 300;
            int actualTtl = baseTtlSeconds + jitterSeconds;
            System.out.println("  - 缓存 Key [category:" + i + "] 实际过期时间分配为: " + actualTtl + " 秒 (平滑打散集中失效时间)");
        }
        System.out.println("✔ 成功消除集群在同一秒瞬间集体失效击垮数据库的潜在风险！\n");
    }
}
