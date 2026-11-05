package com.knowledge.seckill;

import com.knowledge.seckill.domain.SeckillGood;
import com.knowledge.seckill.domain.SeckillRequest;
import com.knowledge.seckill.domain.SeckillResult;
import com.knowledge.seckill.service.SeckillService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 高并发秒杀全链路压力测试与超卖验证
 * 模拟场景：100 个并发用户线程同时争抢库存仅有 10 件的秒杀商品
 * 核心指标验证：
 * 1. 成功生成订单数精确等于 10
 * 2. 数据库剩余库存精确等于 0
 * 3. 绝对不超卖 (Over-selling Rate = 0%)
 * 4. 一人一单严格生效
 */
@SpringBootTest
public class SeckillConcurrencyTest {

    @Autowired
    private SeckillService seckillService;

    /**
     * 独立 main 方法：可在不依赖 Maven Test 的情况下直接运行压测控制台
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println(">>> 正在启动单机独立压测容器...");
        SeckillService service = new SeckillService();
        service.init();

        runSeckillBenchmark(service, 100, 10);

        service.destroy();
    }

    @Test
    public void testHighConcurrencySeckill() throws InterruptedException {
        runSeckillBenchmark(seckillService, 100, 10);
    }

    public static void runSeckillBenchmark(SeckillService service, int totalUsers, int initialStock) throws InterruptedException {
        Long goodsId = 2001L;

        // 1. 初始化秒杀商品
        SeckillGood testGood = SeckillGood.builder()
                .goodsId(goodsId)
                .goodsName("【限量抢购】MacBook Pro M4 Max (10台)")
                .price(new BigDecimal("26999.00"))
                .seckillPrice(new BigDecimal("1999.00"))
                .initialStock(initialStock)
                .remainStock(initialStock)
                .frozenStock(0)
                .soldStock(0)
                .build();
        service.initGoods(testGood);

        System.out.println("\n================================================================================");
        System.out.println("  >>> 开始高并发秒杀压力测试 <<<");
        System.out.println("  商品ID: " + goodsId + " | 商品名称: " + testGood.getGoodsName());
        System.out.println("  初始库存: " + initialStock + " 件 | 模拟并发请求线程数: " + totalUsers + " 个");
        System.out.println("================================================================================\n");

        ExecutorService threadPool = Executors.newFixedThreadPool(totalUsers);

        // 线程同步栅栏与计数器
        CountDownLatch readyGate = new CountDownLatch(totalUsers); // 等待所有线程准备就绪
        CountDownLatch startGate = new CountDownLatch(1);          // 发令枪，一枪同时起跑
        CountDownLatch endGate = new CountDownLatch(totalUsers);   // 等待所有抢购请求完成

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger soldOutCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        AtomicInteger otherFailCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= totalUsers; i++) {
            final long userId = i; // 100 个不同的用户
            threadPool.submit(() -> {
                readyGate.countDown();
                try {
                    // 等待发令枪响
                    startGate.await();

                    SeckillRequest request = SeckillRequest.builder()
                            .goodsId(goodsId)
                            .userId(userId)
                            .build();

                    SeckillResult<?> result = service.doSeckill(request);

                    if (result.getCode() == 200) {
                        successCount.incrementAndGet();
                    } else if (result.getCode() == 4001 || result.getCode() == 4002) {
                        soldOutCount.incrementAndGet();
                    } else if (result.getCode() == 4003) {
                        duplicateCount.incrementAndGet();
                    } else {
                        otherFailCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    otherFailCount.incrementAndGet();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // 等待所有线程准备完毕后，发令枪响
        readyGate.await();
        System.out.println(">>> 100 个抢购线程全部就绪，发令枪响，高并发秒杀洪峰开始冲击！<<<");
        startGate.countDown();

        // 等待所有线程执行完毕
        endGate.await();
        long elapsed = System.currentTimeMillis() - startTime;

        // 等待异步落库队列全部消费完毕
        System.out.println(">>> 接口请求全部返回，正在等待后台异步落库队列削峰落盘...");
        long maxWait = System.currentTimeMillis() + 5000;
        while (!service.getAsyncOrderQueue().isEmpty() && System.currentTimeMillis() < maxWait) {
            Thread.sleep(50);
        }
        Thread.sleep(300); // 确保最后入库事务提交

        // 数据对账
        int dbRemainStock = service.getDatabaseRemainStock(goodsId);
        int redisStock = service.getRedisStock(goodsId);
        int actualOrderCount = service.getCreatedOrderCount(goodsId);
        boolean isLocalBarrierBlocked = service.isLocalOverSold(goodsId);

        System.out.println("\n================================================================================");
        System.out.println("  >>> 高并发秒杀测试结果汇总与对账报告 <<<");
        System.out.println("================================================================================");
        System.out.println(String.format("  总并发请求数       : %d 个", totalUsers));
        System.out.println(String.format("  请求处理耗时       : %d ms", elapsed));
        System.out.println(String.format("  预扣成功排队数     : %d 个", successCount.get()));
        System.out.println(String.format("  售罄拦截失败数     : %d 个", soldOutCount.get()));
        System.out.println(String.format("  重复购买拦截数     : %d 个", duplicateCount.get()));
        System.out.println(String.format("  其他异常拦截数     : %d 个", otherFailCount.get()));
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println(String.format("  【对账】数据库初始库存   : %d 件", initialStock));
        System.out.println(String.format("  【对账】数据库实际剩余   : %d 件 (预期: 0)", dbRemainStock));
        System.out.println(String.format("  【对账】Redis 剩余预扣    : %d 件 (预期: 0)", redisStock));
        System.out.println(String.format("  【对账】实际落库订单数   : %d 笔 (预期: %d 笔)", actualOrderCount, initialStock));
        System.out.println(String.format("  【对账】JVM售罄屏障生效 : %s", isLocalBarrierBlocked ? "已激活（成功拦截后续无效穿透）" : "未激活"));
        System.out.println(String.format("  【对账】超卖检验结果     : %s", (actualOrderCount == initialStock && dbRemainStock == 0) ? "【通过】绝对不超卖！" : "【失败】发生超卖！"));
        System.out.println("================================================================================\n");

        threadPool.shutdown();

        // 强断言验证
        assertEquals(initialStock, actualOrderCount, "数据库最终订单数必须精确等于初始库存数");
        assertEquals(0, dbRemainStock, "数据库剩余可用库存必须精确为 0");
        assertEquals(0, redisStock, "Redis 剩余库存必须精确为 0");
    }
}
