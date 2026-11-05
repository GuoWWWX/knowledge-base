package com.knowledge.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 注解体系与运行时处理器实战：
 * 1. 元注解深度解析：@Target (作用域)、@Retention (生命周期: SOURCE, CLASS, RUNTIME)、@Inherited (可继承性)、@Documented
 * 2. 运行时自定义业务注解 @RateLimit (限流注解)
 * 3. 基于反射构建微型注解处理器 (Annotation Processor / AOP 拦截器前置校验)
 */
public class CustomAnnotationProcessorDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. 自定义注解元数据反射扫描与提取 ==========");
        inspectAnnotations(OrderApiController.class);

        System.out.println("\n========== 2. 模拟微型注解限流拦截器 (AOP 前置拦截) ==========");
        OrderApiController controller = new OrderApiController();
        RateLimitInterceptor interceptor = new RateLimitInterceptor();

        Method createOrderMethod = OrderApiController.class.getMethod("createOrder", String.class, double.class);

        // 连续发起 5 次请求（注解限流配置为每秒最多 3 次）
        for (int i = 1; i <= 5; i++) {
            boolean allowed = interceptor.preHandle(controller, createOrderMethod);
            if (allowed) {
                controller.createOrder("ORDER_" + i, 99.0);
            } else {
                System.err.printf("  ✖ 请求 #%d 被 @RateLimit 注解拦截器直接拒绝并返回 HTTP 429 Too Many Requests!%n", i);
            }
        }
    }

    private static void inspectAnnotations(Class<?> clazz) {
        System.out.println("扫描类: " + clazz.getSimpleName());
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(RateLimit.class)) {
                RateLimit limit = method.getAnnotation(RateLimit.class);
                System.out.printf("  发现方法 [%s] 标记了 @RateLimit: key='%s', maxQps=%d, timeout=%d%s%n",
                        method.getName(), limit.key(), limit.maxQps(), limit.timeout(), limit.timeUnit().name());
            }
        }
    }

    // 1. 定义业务限流注解
    @Documented
    @Inherited
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME) // 运行时保留，允许反射读取
    public @interface RateLimit {
        String key() default "default";
        int maxQps() default 10;
        long timeout() default 1;
        TimeUnit timeUnit() default TimeUnit.SECONDS;
    }

    // 2. 业务使用注解
    public static class OrderApiController {
        @RateLimit(key = "create_order_api", maxQps = 3)
        public void createOrder(String orderId, double amount) {
            System.out.printf("  ✓ [业务执行] 订单 %s 创建成功，金额: ￥%.2f%n", orderId, amount);
        }
    }

    // 3. 注解拦截处理器
    public static class RateLimitInterceptor {
        private final ConcurrentHashMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();

        public boolean preHandle(Object target, Method method) {
            if (!method.isAnnotationPresent(RateLimit.class)) {
                return true;
            }

            RateLimit rateLimit = method.getAnnotation(RateLimit.class);
            String limitKey = rateLimit.key().equals("default")
                    ? target.getClass().getSimpleName() + "." + method.getName()
                    : rateLimit.key();

            AtomicInteger counter = counterMap.computeIfAbsent(limitKey, k -> new AtomicInteger(0));
            int current = counter.incrementAndGet();
            return current <= rateLimit.maxQps();
        }
    }
}
