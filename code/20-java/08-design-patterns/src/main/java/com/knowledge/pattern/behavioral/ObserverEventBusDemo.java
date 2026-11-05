package com.knowledge.pattern.behavioral;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 行为型模式实战：观察者模式 (Observer) 与微型高性能事件总线 (EventBus)
 * 1. 观察者模式：定义对象间的一对多依赖，当主题状态改变时所有依赖者自动得到通知并更新
 * 2. 现代生产演进：避免传统硬编码 Subject / Observer 接口强绑定，手写基于注解 @Subscribe 的微型 EventBus（仿 Google Guava / Spring Event）
 * 3. 核心机制：
 *    - 类型自省与动态路由：根据事件的 Class 类型自动分发到所有注册了该事件类型参数的方法
 *    - 异常隔离：单个观察者发生业务崩溃，不打断后续其他观察者的正常执行
 *    - 支持异步分发 (Async) 与同步分发 (Sync)
 */
public class ObserverEventBusDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 1. 构建微型 EventBus 并动态注册观察者 ==========");
        MiniEventBus eventBus = new MiniEventBus();

        // 注册营销中心、短信中心、数据埋点中心
        eventBus.register(new MarketingCouponObserver());
        eventBus.register(new SmsNotificationObserver());
        eventBus.register(new ProblematicAuditObserver()); // 模拟故障监听者

        System.out.println("\n========== 2. 核心交易系统广播支付成功事件 ==========");
        OrderPaidEvent paidEvent = new OrderPaidEvent("ORDER_20261103_001", "U9999", 599.00);
        eventBus.post(paidEvent);

        System.out.println("\n========== 3. 广播退款事件 ==========");
        OrderRefundEvent refundEvent = new OrderRefundEvent("ORDER_20261103_001", 599.00, "用户主动取消");
        eventBus.post(refundEvent);

        eventBus.shutdown();
        System.out.println("\n>>> 观察者模式解耦完成，系统间无直接引用调用依赖！");
    }

    // 订阅者注解
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface Subscribe {}

    // 事件 1: 支付成功
    public record OrderPaidEvent(String orderId, String userId, double amount) {}

    // 事件 2: 退款
    public record OrderRefundEvent(String orderId, double refundAmount, String reason) {}

    // 观察者 1: 营销中心
    public static class MarketingCouponObserver {
        @Subscribe
        public void handleOrderPaid(OrderPaidEvent event) {
            System.out.printf("  ✓ [营销中心观察者] 用户 %s 支付成功 ￥%.2f, 自动发放满减回购券一张%n",
                    event.userId(), event.amount());
        }
    }

    // 观察者 2: 短信通知中心
    public static class SmsNotificationObserver {
        @Subscribe
        public void handleOrderPaid(OrderPaidEvent event) {
            System.out.printf("  ✓ [短信通知观察者] 推送短信通知给用户 %s: '您的订单 %s 已支付成功'%n",
                    event.userId(), event.orderId());
        }

        @Subscribe
        public void handleOrderRefund(OrderRefundEvent event) {
            System.out.printf("  ✓ [短信通知观察者] 推送退款通知: '订单 %s 退款 ￥%.2f 已到账，原因: %s'%n",
                    event.orderId(), event.refundAmount(), event.reason());
        }
    }

    // 观察者 3: 模拟有 Bug 抛异常的监听器
    public static class ProblematicAuditObserver {
        @Subscribe
        public void handleOrderPaid(OrderPaidEvent event) {
            System.out.println("  * [审计中心观察者] 模拟网络抖动抛出异常...");
            throw new RuntimeException("AuditServiceTimeout: 连接审计数仓超时");
        }
    }

    // 微型事件总线引擎
    public static class MiniEventBus {
        private final Map<Class<?>, List<SubscriberMethod>> subscriberMap = new ConcurrentHashMap<>();
        private final ExecutorService asyncPool = Executors.newCachedThreadPool();

        public void register(Object listener) {
            Class<?> clazz = listener.getClass();
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.isAnnotationPresent(Subscribe.class)) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 1) {
                        Class<?> eventType = parameterTypes[0];
                        method.setAccessible(true);
                        subscriberMap.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                                     .add(new SubscriberMethod(listener, method));
                    }
                }
            }
        }

        public void post(Object event) {
            Class<?> eventType = event.getClass();
            List<SubscriberMethod> subscribers = subscriberMap.get(eventType);
            if (subscribers == null || subscribers.isEmpty()) {
                System.out.println("  [EventBus] 未发现匹配事件类型的订阅者: " + eventType.getSimpleName());
                return;
            }

            for (SubscriberMethod sub : subscribers) {
                try {
                    // 异常隔离反射触发
                    sub.method.invoke(sub.target, event);
                } catch (Exception e) {
                    System.err.printf("  ✖ [EventBus 异常隔离防线] 订阅者 [%s.%s] 执行失败: %s (已隔离，不影响其他订阅者！)%n",
                            sub.target.getClass().getSimpleName(), sub.method.getName(), e.getCause().getMessage());
                }
            }
        }

        public void shutdown() {
            asyncPool.shutdown();
        }

        record SubscriberMethod(Object target, Method method) {}
    }
}
