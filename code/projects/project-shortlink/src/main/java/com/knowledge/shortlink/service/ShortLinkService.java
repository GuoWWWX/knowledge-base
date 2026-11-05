package com.knowledge.shortlink.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.knowledge.shortlink.util.Base62Converter;
import com.knowledge.shortlink.util.HashUtil;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * 高性能短链核心业务服务
 * 架构核心组件：
 * 1. MurmurHash3 + Base62: 极速离散短码生成
 * 2. Guava BloomFilter: 双向防线（生成阶段防哈希碰撞 + 访问阶段阻断穿透攻击）
 * 3. 本地多级 ConcurrentHashMap: 极速映射缓存与双向索引 (Short<->Long)
 * 4. LongAdder: 高并发无锁访问计数
 */
@Slf4j
@Service
public class ShortLinkService {

    /**
     * 短码到长链接的映射缓存 (shortCode -> originUrl)
     */
    private final ConcurrentHashMap<String, String> shortToLongMap = new ConcurrentHashMap<>();

    /**
     * 长链接到短码的反向索引缓存 (originUrl -> shortCode)，保障同一长链接生成幂等
     */
    private final ConcurrentHashMap<String, String> longToShortMap = new ConcurrentHashMap<>();

    /**
     * 短链访问统计计数器 (shortCode -> PV LongAdder)
     */
    private final ConcurrentHashMap<String, LongAdder> accessCountMap = new ConcurrentHashMap<>();

    /**
     * 哈希碰撞累计计数器
     */
    private final AtomicInteger collisionCounter = new AtomicInteger(0);

    /**
     * Guava 高性能布隆过滤器：
     * 预期容量：1,000,000 条短链
     * 允许误判率 (FPP): 0.1% (0.001)
     */
    private BloomFilter<String> bloomFilter;

    @PostConstruct
    public void init() {
        this.bloomFilter = BloomFilter.create(
                Funnels.stringFunnel(StandardCharsets.UTF_8),
                1_000_000,
                0.001
        );
        log.info("【短链服务】布隆过滤器已就绪，预期容量: 100万，目标误判率: 0.1%");
    }

    /**
     * 生成 6 位短链码
     * 流程：
     * 1. 幂等检查：若长链接已存在，直接返回已有短码；
     * 2. 计算 MurmurHash3 32位无符号整数并编码为 6 位 Base62；
     * 3. 利用布隆过滤器探测是否存在，若发生哈希碰撞，循环加盐重算直至唯一；
     * 4. 登记布隆过滤器并写入本地映射缓存。
     *
     * @param originUrl 原始长链接
     * @return 6 位短链唯一码
     */
    public String createShortLink(String originUrl) {
        if (originUrl == null || originUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("原始长链接不能为空");
        }

        String cleanedUrl = originUrl.trim();

        // 1. 幂等性检查：如果该长链接已经生成过，直接复用已有短码
        String existingShortCode = longToShortMap.get(cleanedUrl);
        if (existingShortCode != null) {
            log.debug("【长链幂等命中】长链接已存在对应短码: {} -> {}", cleanedUrl, existingShortCode);
            return existingShortCode;
        }

        // 2. 初始计算 MurmurHash3 + Base62
        long hash = HashUtil.murmur32AsUnsigned(cleanedUrl);
        String shortCode = Base62Converter.toBase62(hash, 6);

        // 3. 结合布隆过滤器与本地存储检测哈希碰撞
        int salt = 1;
        while (bloomFilter.mightContain(shortCode)) {
            // 布隆过滤器判定可能存在，进一步检查本地存储确认是否发生真碰撞
            String mappedOriginUrl = shortToLongMap.get(shortCode);
            if (mappedOriginUrl == null) {
                // 此时属于布隆过滤器 0.1% 的极低概率假阳性（误判），短码其实可用，直接跳出使用
                break;
            }

            if (mappedOriginUrl.equals(cleanedUrl)) {
                // 属于同一长链接的重复请求
                return shortCode;
            }

            // 确认发生真实哈希碰撞：不同的 URL 生成了相同的 shortCode
            int totalCollisions = collisionCounter.incrementAndGet();
            log.warn("⚠️【检测到哈希碰撞 #{}】长链 [{}] 与已有长链 [{}] 发生碰撞 shortCode: {}, 正在加盐重散列...",
                    totalCollisions, cleanedUrl, mappedOriginUrl, shortCode);

            // 加盐重新计算哈希与 Base62 短码
            long saltedHash = HashUtil.murmur32WithSalt(cleanedUrl, salt++);
            shortCode = Base62Converter.toBase62(saltedHash, 6);
        }

        // 4. 将确定唯一的 shortCode 注入布隆过滤器与双向缓存
        bloomFilter.put(shortCode);
        shortToLongMap.put(shortCode, cleanedUrl);
        longToShortMap.put(cleanedUrl, shortCode);
        accessCountMap.put(shortCode, new LongAdder());

        log.info("【短链生成成功】长链接: [{}] ---> 短码: [{}]", cleanedUrl, shortCode);
        return shortCode;
    }

    /**
     * 还原原始长链接（支持布隆过滤器快速阻断穿透）
     *
     * @param shortCode 6 位短链码
     * @return 原始长链接，若不存在则返回 null
     */
    public String getOriginUrl(String shortCode) {
        if (shortCode == null || shortCode.trim().isEmpty()) {
            return null;
        }

        String code = shortCode.trim();

        // ----------------------------------------------------
        // 第一道防线：布隆过滤器前置拦截非法/不存在的短码探测
        // 布隆过滤器核心性质：判定不存在，则 100% 绝对不存在！
        // ----------------------------------------------------
        if (!bloomFilter.mightContain(code)) {
            log.warn("🛡️【布隆过滤器精准拦截穿透】短码 [{}] 绝对不存在，直接拦截阻断！", code);
            return null;
        }

        // ----------------------------------------------------
        // 第二道防线：查询本地映射缓存
        // ----------------------------------------------------
        String originUrl = shortToLongMap.get(code);
        if (originUrl != null) {
            // 访问成功，累计访问热度 PV
            LongAdder counter = accessCountMap.computeIfAbsent(code, k -> new LongAdder());
            counter.increment();
            log.info("【短链跳转命中】短码 [{}] -> 长链接 [{}]，累计访问次数: {}", code, originUrl, counter.sum());
            return originUrl;
        }

        // 若布隆过滤器认为存在但本地没有，属于布隆过滤器的假阳性误判，安全返回 null
        log.warn("【布隆过滤器微小误判】短码 [{}] 未在本地找到真实映射", code);
        return null;
    }

    /**
     * 查询短链累计点击 PV
     */
    public long getAccessCount(String shortCode) {
        LongAdder counter = accessCountMap.get(shortCode);
        return counter != null ? counter.sum() : 0L;
    }

    /**
     * 查询当前系统中已生成的短链总数
     */
    public int getTotalShortLinks() {
        return shortToLongMap.size();
    }

    /**
     * 获取历史哈希碰撞次数统计
     */
    public int getCollisionCount() {
        return collisionCounter.get();
    }
}
