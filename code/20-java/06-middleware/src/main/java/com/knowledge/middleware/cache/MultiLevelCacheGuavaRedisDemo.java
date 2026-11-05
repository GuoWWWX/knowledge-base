package com.knowledge.middleware.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 工业级多级缓存 (Multi-Level Cache) 一致性架构：
 * 1. 一级缓存 (L1): JVM 进程内本地缓存 (Guava/Caffeine)，读取微秒级 (零网络开销)
 * 2. 二级缓存 (L2): 分布式缓存 (Docker Redis 7)，读取毫秒级 (跨节点共享)
 * 3. 三级存储 (L3): 数据库持久化存储 (MySQL)
 * 4. 读链路: L1 -> L2 -> L3 (层层渗透与回填)
 * 5. 写链路 & 缓存一致性: 更新 DB -> 淘汰 Redis -> 通过 Redis Pub/Sub 发布缓存失效广播，所有集群节点的 L1 自动同步失效！
 */
public class MultiLevelCacheGuavaRedisDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 1. 构建多级缓存实例与 Redis 广播监听 ==========");
        Config config = new Config();
        config.useSingleServer().setAddress("redis://127.0.0.1:6379");
        RedissonClient redisson = Redisson.create(config);

        MultiLevelCacheManager cacheNodeA = new MultiLevelCacheManager("Node-A", redisson);
        MultiLevelCacheManager cacheNodeB = new MultiLevelCacheManager("Node-B", redisson);

        String skuId = "SKU_8888";

        System.out.println("\n========== 2. 读穿透链路演练 ==========");
        System.out.println("--- 节点 A 首次读取: L1 未命中 -> L2 未命中 -> 查询 DB 并层层回填 ---");
        String val1 = cacheNodeA.get(skuId);
        System.out.println("节点 A 获取结果: " + val1);

        System.out.println("\n--- 节点 A 第二次读取: 瞬间命中本地 L1 缓存 (0 网络开销) ---");
        String val2 = cacheNodeA.get(skuId);
        System.out.println("节点 A 获取结果: " + val2);

        System.out.println("\n--- 节点 B 首次读取: L1 未命中 -> 命中 Redis L2 缓存并回填节点 B 本地 L1 ---");
        String val3 = cacheNodeB.get(skuId);
        System.out.println("节点 B 获取结果: " + val3);

        System.out.println("\n========== 3. 写链路与分布式缓存失效广播演练 ==========");
        System.out.println("--- 节点 A 更新商品价格: 更新 DB -> 淘汰 Redis L2 -> 发布广播通知全网节点清理本地 L1 ---");
        cacheNodeA.update(skuId, "iPhone 16 Pro - ￥7999 (降价大促)");

        // 等待 Redis 订阅发布消息传递 (50ms)
        TimeUnit.MILLISECONDS.sleep(100);

        System.out.println("\n--- 检验节点 B 的本地 L1 是否已被广播强制清除 ---");
        System.out.println("节点 B 此时读取，应当重新从 Redis/DB 加载最新数据:");
        String latestVal = cacheNodeB.get(skuId);
        System.out.println("节点 B 读到的最新数据: " + latestVal);

        redisson.shutdown();
        System.out.println(">>> 多级缓存一致性演示圆满闭环！");
    }

    public static class MultiLevelCacheManager {
        private final String nodeName;
        private final RedissonClient redisson;
        private final Cache<String, String> localCache;
        private final RTopic invalidationTopic;

        public MultiLevelCacheManager(String nodeName, RedissonClient redisson) {
            this.nodeName = nodeName;
            this.redisson = redisson;
            this.localCache = CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .build();

            this.invalidationTopic = redisson.getTopic("CACHE_INVALIDATION_TOPIC");
            // 订阅全网缓存失效通知
            this.invalidationTopic.addListener(String.class, (channel, key) -> {
                System.out.printf("  [%s 收到失效广播] 立即从本地 L1 淘汰 key=%s%n", this.nodeName, key);
                localCache.invalidate(key);
            });
        }

        public String get(String key) {
            // 1. 查 L1
            String value = localCache.getIfPresent(key);
            if (value != null) {
                System.out.printf("  ✓ [%s L1 命中] key=%s%n", nodeName, key);
                return value;
            }

            // 2. 查 L2 (Redis)
            RBucket<String> bucket = redisson.getBucket("cache:" + key);
            value = bucket.get();
            if (value != null) {
                System.out.printf("  ✓ [%s L2 Redis 命中] 回填 L1, key=%s%n", nodeName, key);
                localCache.put(key, value);
                return value;
            }

            // 3. 查 L3 (DB 模拟)
            System.out.printf("  * [%s L1/L2 均未命中] 回源查询数据库... key=%s%n", nodeName, key);
            value = "iPhone 16 Pro - ￥8999 (原价)";
            bucket.set(value, Duration.ofMinutes(10));
            localCache.put(key, value);
            return value;
        }

        public void update(String key, String newValue) {
            // 1. 更新数据库 (模拟)
            System.out.printf(">>> [%s] 更新数据库成功: %s%n", nodeName, newValue);

            // 2. 淘汰 Redis L2
            redisson.getBucket("cache:" + key).delete();
            System.out.printf(">>> [%s] 淘汰 Redis L2 成功: %s%n", nodeName, key);

            // 3. 广播失效消息
            invalidationTopic.publish(key);
        }
    }
}
