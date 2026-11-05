package com.knowledge.pattern.structural;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 结构型模式实战：门面模式 (Facade) 与 享元模式 (Flyweight)
 * 1. 门面模式 (Facade):
 *    - 核心思想：子系统对外的统一高级接口，屏蔽底层子系统交互细节与复杂的并发编排
 *    - 业务实战：电商商品详情页 BFF (Backend For Frontend) 聚合门面，异步并行并发编排商品基本信息、库存、会员权益与营销折扣
 * 2. 享元模式 (Flyweight):
 *    - 核心思想：运用共享技术有效支持大量细粒度对象的复用（区分内部不变状态与外部易变状态）
 *    - 对标源码：Integer.valueOf(-128~127)、String 常量池、数据库连接池、字符图元排版
 *    - 业务实战：百万商品规格属性 (Color, Size, Material) 元数据享元池化复用
 */
public class FacadeAndFlyweightDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 门面模式 (Facade) 并行聚合商品详情页 ==========");
        ProductDetailFacade facade = new ProductDetailFacade();
        ProductDetailPageVo pageVo = facade.loadProductDetailPage(1001L, "U8888");
        System.out.println("门面服务快速返回前端页面聚合视图:\n" + pageVo);

        System.out.println("\n========== 2. 享元模式 (Flyweight) 百万级 SKU 规格元数据池化复用 ==========");
        testFlyweightMemoryReuse();
    }

    private static void testFlyweightMemoryReuse() {
        SkuSpecFlyweightFactory factory = new SkuSpecFlyweightFactory();

        // 模拟创建 5 个商品，虽然是不同商品，但颜色与规格元数据完全复用同一个内部不可变对象
        SkuSpec spec1 = factory.getSpec("深空黑", "1TB");
        SkuSpec spec2 = factory.getSpec("钛金属", "512GB");
        SkuSpec spec3 = factory.getSpec("深空黑", "1TB"); // 命中缓存池同一对象

        System.out.println("spec1 内存引用 Hash: " + System.identityHashCode(spec1));
        System.out.println("spec2 内存引用 Hash: " + System.identityHashCode(spec2));
        System.out.println("spec3 内存引用 Hash: " + System.identityHashCode(spec3));
        System.out.println("spec1 == spec3 享元同一性验证: " + (spec1 == spec3) + " (100% 内存单例复用！)");
        System.out.println("享元池内部实际维护的规格对象总数: " + factory.getPoolSize() + " (极大节约海量对象的 JVM 堆内存与 GC 压力)");
    }

    // ================= 1. 门面模式 =================
    public record ProductDetailPageVo(long spuId, String title, int stock, double finalPrice, String vipBenefit) {}

    public static class ProductDetailFacade {
        // 门面内部封装各复杂微服务客户端
        public ProductDetailPageVo loadProductDetailPage(long spuId, String userId) {
            System.out.println(">>> [BFF 门面接收请求] 启动 CompletableFuture 并发流水线聚合下游微服务...");

            CompletableFuture<String> titleFuture = CompletableFuture.supplyAsync(() -> "iPhone 16 Pro Max 钛金属版");
            CompletableFuture<Integer> stockFuture = CompletableFuture.supplyAsync(() -> 88);
            CompletableFuture<Double> priceFuture = CompletableFuture.supplyAsync(() -> 9999.00 - 400.00); // 营销券后价
            CompletableFuture<String> vipFuture = CompletableFuture.supplyAsync(() -> "尊贵黑金会员享优先发货");

            CompletableFuture.allOf(titleFuture, stockFuture, priceFuture, vipFuture).join();

            return new ProductDetailPageVo(
                    spuId,
                    titleFuture.join(),
                    stockFuture.join(),
                    priceFuture.join(),
                    vipFuture.join()
            );
        }
    }

    // ================= 2. 享元模式 =================
    // 享元对象：内部不变状态 (Intrinsic State)
    public record SkuSpec(String color, String capacity) {}

    // 享元工厂
    public static class SkuSpecFlyweightFactory {
        private final Map<String, SkuSpec> pool = new ConcurrentHashMap<>();

        public SkuSpec getSpec(String color, String capacity) {
            String key = color + "_" + capacity;
            return pool.computeIfAbsent(key, k -> {
                System.out.println("  * [享元池新对象实例化] key=" + key);
                return new SkuSpec(color, capacity);
            });
        }

        public int getPoolSize() {
            return pool.size();
        }
    }
}
