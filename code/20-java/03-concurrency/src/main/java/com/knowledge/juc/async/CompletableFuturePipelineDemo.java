package com.knowledge.juc.async;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * CompletableFuture 生产级电商详情页异步编排实战
 *
 * 核心设计要点：
 * 1. 独立线程池隔离：拒绝使用默认 ForkJoinPool.commonPool()，避免因慢调用耗尽全局线程；
 * 2. 多源并发调用：并行请求用户服务、商品基础服务、优惠券服务；
 * 3. 依赖链路串联：依赖商品基础信息的子服务（如关联推荐服务）采用 thenComposeAsync 流水线编排；
 * 4. 故障隔离与优雅降级：通过 exceptionally() / handle() 对非核心服务（如优惠券）兜底容错；
 * 5. 结果聚合收口：CompletableFuture.allOf() 等待全部就绪后无缝组装 VO。
 */
public class CompletableFuturePipelineDemo {

    // 生产推荐：为不同业务域声明独立的线程池
    private static final ExecutorService bizExecutor = new ThreadPoolExecutor(
            4, 8, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadFactory() {
                private int count = 1;
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "detail-pipeline-pool-" + count++);
                    t.setDaemon(true);
                    return t;
                }
            },
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("     CompletableFuture 电商详情页高并发异步编排与容错实战      ");
        System.out.println("===============================================================\n");

        long userId = 10086L;
        long productId = 998877L;

        // 1. 串行基准对照测试
        System.out.println("--- 步骤 1：传统串行顺序调用耗时测试 ---");
        long serialStart = System.currentTimeMillis();
        UserInfo serialUser = mockGetUserInfo(userId);
        ProductInfo serialProduct = mockGetProductInfo(productId);
        List<String> serialCoupons = mockGetAvailableCoupons(userId, productId, false);
        List<String> serialRecommends = mockGetRecommendProducts(serialProduct.category());
        long serialCost = System.currentTimeMillis() - serialStart;
        System.out.printf(">>> 串行顺序调用完成，总耗时: %d ms\n\n", serialCost);

        // 2. 并行异步流水线编排测试 (含故障降级)
        System.out.println("--- 步骤 2：CompletableFuture 并发编排与故障降级测试 ---");
        long asyncStart = System.currentTimeMillis();

        // 异步任务 1：获取用户信息
        CompletableFuture<UserInfo> userFuture = CompletableFuture.supplyAsync(
                () -> mockGetUserInfo(userId), bizExecutor);

        // 异步任务 2：获取商品基础信息
        CompletableFuture<ProductInfo> productFuture = CompletableFuture.supplyAsync(
                () -> mockGetProductInfo(productId), bizExecutor);

        // 异步任务 3：获取优惠券（模拟网络故障并自动容错降级）
        CompletableFuture<List<String>> couponFuture = CompletableFuture.supplyAsync(
                () -> mockGetAvailableCoupons(userId, productId, true), bizExecutor)
                .exceptionally(throwable -> {
                    System.err.printf("  [容错告警] 优惠券服务调用异常: [%s]，触发默认兜底降级策略！\n",
                            throwable.getMessage());
                    return Collections.singletonList("兜底新人立减券 ￥5 (Fallback)");
                });

        // 异步任务 4：依赖商品分类查询关联推荐（thenCompose 串联）
        CompletableFuture<List<String>> recommendFuture = productFuture.thenComposeAsync(
                product -> CompletableFuture.supplyAsync(
                        () -> mockGetRecommendProducts(product.category()), bizExecutor),
                bizExecutor
        );

        // 聚合收口：等待所有任务并行完成
        CompletableFuture<Void> allOfFuture = CompletableFuture.allOf(
                userFuture, productFuture, couponFuture, recommendFuture);

        // 当所有任务完成后组装成最终的详情页 VO
        CompletableFuture<ProductDetailVO> resultFuture = allOfFuture.thenApply(v -> {
            UserInfo u = userFuture.join();
            ProductInfo p = productFuture.join();
            List<String> c = couponFuture.join();
            List<String> r = recommendFuture.join();
            return new ProductDetailVO(u, p, c, r);
        });

        ProductDetailVO detailVO = resultFuture.join();
        long asyncCost = System.currentTimeMillis() - asyncStart;

        System.out.printf(">>> 并发流水线聚合完成，总耗时: %d ms (性能显著提升 ~%d%%)!\n\n",
                asyncCost, (serialCost - asyncCost) * 100 / serialCost);

        // 打印详情页组装结果
        System.out.println("---------------- 最终组装完成的电商商品详情页 VO ----------------");
        System.out.println(detailVO);
        System.out.println("-----------------------------------------------------------------");

        bizExecutor.shutdown();
    }

    // ======================= 模拟微服务调用 =======================

    private static UserInfo mockGetUserInfo(long userId) {
        sleep(200);
        return new UserInfo(userId, "架构师小刘", "SVIP-黄金会员");
    }

    private static ProductInfo mockGetProductInfo(long productId) {
        sleep(250);
        return new ProductInfo(productId, "MacBook Pro M4 Max 64G", 26999.00, "数码电子");
    }

    private static List<String> mockGetAvailableCoupons(long userId, long productId, boolean simulateError) {
        sleep(150);
        if (simulateError) {
            throw new RuntimeException("CouponRpcException: 优惠券集群响应超时 504");
        }
        return List.of("数码满 5000 减 400", "金牌会员专属券 9 折");
    }

    private static List<String> mockGetRecommendProducts(String category) {
        sleep(180);
        return List.of("妙控键盘 黑色款", "Type-C 扩展坞", "原装保护内胆包");
    }

    private static void sleep(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ======================= 实体定义 =======================

    public record UserInfo(long userId, String username, String vipLevel) {}
    public record ProductInfo(long productId, String title, double price, String category) {}
    public record ProductDetailVO(
            UserInfo user,
            ProductInfo product,
            List<String> coupons,
            List<String> recommends
    ) {
        @Override
        public String toString() {
            return String.format(
                    "用户状态: [%s (%s)]\n" +
                    "商品信息: [%s | 原价: ￥%.2f | 类目: %s]\n" +
                    "可用优惠: %s\n" +
                    "搭配推荐: %s",
                    user.username(), user.vipLevel(),
                    product.title(), product.price(), product.category(),
                    coupons,
                    recommends
            );
        }
    }
}
