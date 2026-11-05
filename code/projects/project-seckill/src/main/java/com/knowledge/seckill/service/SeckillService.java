package com.knowledge.seckill.service;

import com.knowledge.seckill.domain.SeckillGood;
import com.knowledge.seckill.domain.SeckillOrder;
import com.knowledge.seckill.domain.SeckillRequest;
import com.knowledge.seckill.domain.SeckillResult;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 高并发秒杀核心业务服务
 * 核心架构三层漏斗设计：
 * 1. JVM 本地售罄标记内存屏障 (Local Over Sold Barrier) -> 阻断 99% 无效请求
 * 2. 模拟 Redis Lua 原子脚本预扣库存 (Mock Redis Lua Script Atomic Deduct) -> 保障一人一单与资格原子预扣
 * 3. 异步消息排队削峰落库 (Async Order Queueing & Peak Shaving) -> 保护数据库匀速写入
 */
@Slf4j
@Service
public class SeckillService {

    // ==========================================
    // 1. JVM 本地内存屏障
    // ==========================================
    /**
     * 商品本地售罄标记：goodsId -> isSoldOut
     * 一旦标记为 true，请求直接在 JVM 内存中快速失败返回，不再穿透到 Redis 或 MQ
     */
    private final ConcurrentHashMap<Long, Boolean> localOverSoldMap = new ConcurrentHashMap<>();

    // ==========================================
    // 2. 模拟 Redis 集群层数据结构与 Lua 脚本原子执行
    // ==========================================
    /**
     * 模拟 Redis 中的商品库存键：seckill:{goodsId}:stock
     */
    private final ConcurrentHashMap<Long, AtomicInteger> mockRedisStockMap = new ConcurrentHashMap<>();

    /**
     * 模拟 Redis 中的已购用户集合键：seckill:{goodsId}:users (用于一人一单校验)
     */
    private final ConcurrentHashMap<Long, Set<Long>> mockRedisPurchasedUsersMap = new ConcurrentHashMap<>();

    /**
     * 针对每个 goodsId 的细粒度排他锁，用于精确模拟 Redis 单线程执行 Lua 脚本的原子语义
     */
    private final ConcurrentHashMap<Long, ReentrantLock> luaLocks = new ConcurrentHashMap<>();

    // ==========================================
    // 3. 模拟 MySQL 数据库持久化存储与事务落库
    // ==========================================
    /**
     * 模拟数据库中的商品库存表
     */
    private final ConcurrentHashMap<Long, SeckillGood> mockDatabaseGoods = new ConcurrentHashMap<>();

    /**
     * 模拟数据库中的秒杀订单表：orderId -> SeckillOrder
     */
    private final ConcurrentHashMap<Long, SeckillOrder> mockDatabaseOrders = new ConcurrentHashMap<>();

    /**
     * 模拟数据库联合唯一索引 uk_activity_user(goodsId, userId) 强一致保障
     */
    private final ConcurrentHashMap<String, Long> mockDatabaseUniqueIndex = new ConcurrentHashMap<>();

    /**
     * 分布式唯一订单号发号器（模拟雪花算法生成唯一 ID）
     */
    private final AtomicLong orderIdGenerator = new AtomicLong(100000000000L);

    // ==========================================
    // 4. 异步削峰消息队列与落库消费者线程池
    // ==========================================
    /**
     * 模拟 RocketMQ / 消息中间件阻塞任务队列
     */
    private final BlockingQueue<SeckillTask> asyncOrderQueue = new LinkedBlockingQueue<>(50000);

    /**
     * 异步落库消费工作线程池
     */
    private final ExecutorService orderConsumerExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "seckill-order-consumer");
        thread.setDaemon(true);
        return thread;
    });

    private volatile boolean isRunning = true;

    @Data
    @AllArgsConstructor
    public static class SeckillTask {
        private String requestId;
        private Long goodsId;
        private Long userId;
        private BigDecimal payAmount;
        private CompletableFuture<SeckillResult<SeckillOrder>> future;
    }

    @PostConstruct
    public void init() {
        // 启动后台队列异步消费线程，模拟 MQ 消费者平稳将订单落库
        for (int i = 0; i < 4; i++) {
            orderConsumerExecutor.submit(this::processOrderQueue);
        }
        log.info("【秒杀核心服务】初始化完成，异步订单消费者线程组已启动！");
    }

    @PreDestroy
    public void destroy() {
        isRunning = false;
        orderConsumerExecutor.shutdown();
    }

    /**
     * 初始化秒杀商品与活动预热
     */
    public void initGoods(SeckillGood good) {
        Long goodsId = good.getGoodsId();
        // 1. 初始化数据库商品行记录
        mockDatabaseGoods.put(goodsId, good);

        // 2. 预热库存至模拟 Redis 缓存
        mockRedisStockMap.put(goodsId, new AtomicInteger(good.getRemainStock()));
        mockRedisPurchasedUsersMap.put(goodsId, ConcurrentHashMap.newKeySet());
        luaLocks.putIfAbsent(goodsId, new ReentrantLock());

        // 3. 重置 JVM 本地售罄标记屏障
        localOverSoldMap.put(goodsId, false);

        log.info("【秒杀预热完成】商品ID: {}, 名称: {}, 初始库存: {}, 秒杀价格: {}",
                goodsId, good.getGoodsName(), good.getRemainStock(), good.getSeckillPrice());
    }

    /**
     * 核心秒杀下单入口方法
     * 经历三层拦截过滤：
     * 步骤 1: JVM 本地售罄标记内存屏障校验
     * 步骤 2: 模拟 Redis Lua 脚本原子预扣资格与库存
     * 步骤 3: 发送异步消息排队削峰，保护数据库落库
     */
    public SeckillResult<?> doSeckill(SeckillRequest request) {
        Long goodsId = request.getGoodsId();
        Long userId = request.getUserId();
        String requestId = request.getRequestId();
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString().replace("-", "");
            request.setRequestId(requestId);
        }

        // ----------------------------------------------------
        // 第一道防线：JVM 本地内存标记售罄屏障拦截
        // ----------------------------------------------------
        Boolean isSoldOut = localOverSoldMap.getOrDefault(goodsId, false);
        if (Boolean.TRUE.equals(isSoldOut)) {
            return SeckillResult.fail(4001, "商品已售罄 (JVM 本地内存屏障拦截)");
        }

        // ----------------------------------------------------
        // 第二道防线：模拟 Redis Lua 脚本执行原子扣减与防重校验
        // ----------------------------------------------------
        int luaResult = executeMockRedisLuaScript(goodsId, userId, requestId);
        if (luaResult == -1) {
            // 库存不足
            localOverSoldMap.put(goodsId, true);
            return SeckillResult.fail(4002, "商品已售罄 (Redis 预扣失败)");
        } else if (luaResult == -2) {
            // 一人一单限制，重复购买
            return SeckillResult.fail(4003, "每人限购一件，请勿重复抢购 (一人一单拦截)");
        } else if (luaResult != 1) {
            return SeckillResult.fail(5000, "系统繁忙，请稍后再试");
        }

        // ----------------------------------------------------
        // 第三道防线：预扣成功，投递异步消息队列削峰排队
        // ----------------------------------------------------
        SeckillGood good = mockDatabaseGoods.get(goodsId);
        BigDecimal payAmount = good != null ? good.getSeckillPrice() : BigDecimal.ZERO;

        CompletableFuture<SeckillResult<SeckillOrder>> orderFuture = new CompletableFuture<>();
        SeckillTask task = new SeckillTask(requestId, goodsId, userId, payAmount, orderFuture);

        boolean offered = asyncOrderQueue.offer(task);
        if (!offered) {
            // 队列已满，反向回滚 Redis 库存（模拟分布式事务补偿）
            rollbackRedisPreDeduct(goodsId, userId);
            return SeckillResult.fail(5001, "当前抢购排队人数过多，请重试");
        }

        Map<String, Object> resultData = new HashMap<>();
        resultData.put("requestId", requestId);
        resultData.put("goodsId", goodsId);
        resultData.put("userId", userId);
        resultData.put("status", "QUEUED");
        resultData.put("message", "抢购资格预扣成功，订单正在排队创建中");

        return SeckillResult.success(resultData);
    }

    /**
     * 同步等待订单落库完成（测试与调试辅助）
     */
    public SeckillResult<SeckillOrder> doSeckillSync(SeckillRequest request, long timeoutMs) {
        SeckillResult<?> asyncResult = doSeckill(request);
        if (asyncResult.getCode() != 200) {
            return SeckillResult.fail(asyncResult.getCode(), asyncResult.getMessage());
        }

        // 轮询查询数据库落库结果
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            SeckillOrder order = queryOrderByUser(request.getGoodsId(), request.getUserId());
            if (order != null) {
                return SeckillResult.success("秒杀成功并已落库", order);
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return SeckillResult.fail(5002, "排队处理超时");
    }

    /**
     * 模拟 Redis Lua 脚本原子执行逻辑：
     *
     * 对应的标准 Redis Lua 脚本逻辑：
     * -----------------------------------------------------------
     * local stockKey = KEYS[1]
     * local usersKey = KEYS[2]
     * local userId = ARGV[1]
     *
     * -- 1. 一人一单防重检查
     * if redis.call('SISMEMBER', usersKey, userId) == 1 then
     *     return -2  -- 重复抢购
     * end
     *
     * -- 2. 库存可用性检查
     * local stock = tonumber(redis.call('GET', stockKey) or '0')
     * if stock <= 0 then
     *     return -1  -- 库存已罄
     * end
     *
     * -- 3. 原子扣减与记录用户
     * redis.call('DECRBY', stockKey, 1)
     * redis.call('SADD', usersKey, userId)
     * return 1      -- 扣减成功
     * -----------------------------------------------------------
     */
    private int executeMockRedisLuaScript(Long goodsId, Long userId, String requestId) {
        ReentrantLock lock = luaLocks.computeIfAbsent(goodsId, id -> new ReentrantLock());
        lock.lock();
        try {
            Set<Long> userSet = mockRedisPurchasedUsersMap.computeIfAbsent(goodsId, id -> ConcurrentHashMap.newKeySet());
            // 1. 一人一单防重检查
            if (userSet.contains(userId)) {
                return -2; // 重复购买
            }

            // 2. 检查可用库存
            AtomicInteger stock = mockRedisStockMap.get(goodsId);
            if (stock == null || stock.get() <= 0) {
                return -1; // 库存不足
            }

            // 3. 原子扣减并登记用户
            int remain = stock.decrementAndGet();
            userSet.add(userId);

            // 若扣减后刚好售罄，立即更新 JVM 本地屏障，后续请求无需再竞争锁
            if (remain <= 0) {
                localOverSoldMap.put(goodsId, true);
                log.info(">>> [Redis Lua] 商品 {} 预扣后库存清零，触发标记本地售罄屏障 <<<", goodsId);
            }

            return 1; // 成功
        } finally {
            lock.unlock();
        }
    }

    /**
     * 异常或队列满时的回滚操作
     */
    private void rollbackRedisPreDeduct(Long goodsId, Long userId) {
        ReentrantLock lock = luaLocks.computeIfAbsent(goodsId, id -> new ReentrantLock());
        lock.lock();
        try {
            AtomicInteger stock = mockRedisStockMap.get(goodsId);
            if (stock != null) {
                stock.incrementAndGet();
            }
            Set<Long> userSet = mockRedisPurchasedUsersMap.get(goodsId);
            if (userSet != null) {
                userSet.remove(userId);
            }
            // 恢复本地标记
            localOverSoldMap.put(goodsId, false);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 异步落库队列工作消费者任务
     */
    private void processOrderQueue() {
        while (isRunning) {
            try {
                SeckillTask task = asyncOrderQueue.poll(500, TimeUnit.MILLISECONDS);
                if (task != null) {
                    processTaskInDatabaseTransaction(task);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("消费秒杀订单异常: ", e);
            }
        }
    }

    /**
     * 模拟关系型数据库事务中的条件更新与订单写入（强一致性兜底）
     */
    private void processTaskInDatabaseTransaction(SeckillTask task) {
        Long goodsId = task.getGoodsId();
        Long userId = task.getUserId();
        String requestId = task.getRequestId();

        // 1. 唯一索引防护：防止重复落库
        String uniqueKey = goodsId + ":" + userId;
        if (mockDatabaseUniqueIndex.putIfAbsent(uniqueKey, 0L) != null) {
            log.warn("【数据库唯一索引冲突拦截】用户 {} 对商品 {} 重复生成订单，已被物理拦截！", userId, goodsId);
            task.getFuture().complete(SeckillResult.fail(4003, "重复下单"));
            return;
        }

        // 2. 模拟数据库原子条件更新：UPDATE seckill_goods SET remain_stock = remain_stock - 1, frozen_stock = frozen_stock + 1 WHERE goods_id = ? AND remain_stock >= 1
        SeckillGood good = mockDatabaseGoods.get(goodsId);
        if (good == null) {
            task.getFuture().complete(SeckillResult.fail(4004, "商品不存在"));
            return;
        }

        synchronized (good) {
            if (good.getRemainStock() < 1) {
                log.error("【数据库行级锁兜底拦截】商品 {} 实际数据库库存不足，拒绝落库！", goodsId);
                task.getFuture().complete(SeckillResult.fail(4002, "商品库存不足"));
                return;
            }

            // 更新库存
            good.setRemainStock(good.getRemainStock() - 1);
            good.setFrozenStock(good.getFrozenStock() + 1);

            // 3. 生成持久化订单记录
            long orderId = orderIdGenerator.incrementAndGet();
            SeckillOrder order = SeckillOrder.builder()
                    .orderId(orderId)
                    .requestId(requestId)
                    .goodsId(goodsId)
                    .userId(userId)
                    .payAmount(task.getPayAmount())
                    .status("WAIT_PAY")
                    .createTime(LocalDateTime.now())
                    .build();

            mockDatabaseOrders.put(orderId, order);
            mockDatabaseUniqueIndex.put(uniqueKey, orderId);

            log.info("【数据库订单落库成功】订单号: {}, 用户ID: {}, 商品: {}, 实付: {}",
                    orderId, userId, goodsId, task.getPayAmount());

            task.getFuture().complete(SeckillResult.success(order));
        }
    }

    // ==========================================
    // 状态查询与对账辅助接口
    // ==========================================
    public SeckillOrder queryOrderByUser(Long goodsId, Long userId) {
        String uniqueKey = goodsId + ":" + userId;
        Long orderId = mockDatabaseUniqueIndex.get(uniqueKey);
        if (orderId == null || orderId == 0L) {
            return null;
        }
        return mockDatabaseOrders.get(orderId);
    }

    public SeckillOrder queryOrderById(Long orderId) {
        return mockDatabaseOrders.get(orderId);
    }

    public int getDatabaseRemainStock(Long goodsId) {
        SeckillGood good = mockDatabaseGoods.get(goodsId);
        return good != null ? good.getRemainStock() : 0;
    }

    public int getRedisStock(Long goodsId) {
        AtomicInteger stock = mockRedisStockMap.get(goodsId);
        return stock != null ? stock.get() : 0;
    }

    public int getCreatedOrderCount(Long goodsId) {
        return (int) mockDatabaseOrders.values().stream()
                .filter(o -> o.getGoodsId().equals(goodsId))
                .count();
    }

    public boolean isLocalOverSold(Long goodsId) {
        return localOverSoldMap.getOrDefault(goodsId, false);
    }

    public BlockingQueue<SeckillTask> getAsyncOrderQueue() {
        return asyncOrderQueue;
    }
}
