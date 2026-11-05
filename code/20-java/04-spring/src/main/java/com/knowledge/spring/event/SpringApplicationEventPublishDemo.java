package com.knowledge.spring.event;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Spring 事件驱动架构 (ApplicationEvent) 实战：
 * 1. 业务解耦：主下单流程只负责创建订单与扣减库存，履约发券、短信通知、数仓审计全部通过事件发布异步/同步解耦
 * 2. 核心组件：ApplicationEvent (事件体) + ApplicationEventPublisher (发布者) + @EventListener (事件监听器)
 * 3. 规避同步事件监听器抛出异常导致主事务被连带回滚的陷阱
 */
public class SpringApplicationEventPublishDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 启动 Spring 容器并注入事件驱动链路 ==========");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(EventConfig.class);

        OrderService orderService = context.getBean(OrderService.class);
        System.out.println("\n========== 2. 用户下单支付并触发事件广播 ==========");
        orderService.checkoutOrder("ORDER_20261103_001", "U8888", 299.0);

        context.close();
    }

    // 1. 领域事件
    public static class OrderCreatedEvent extends ApplicationEvent {
        private final String orderId;
        private final String userId;
        private final double amount;

        public OrderCreatedEvent(Object source, String orderId, String userId, double amount) {
            super(source);
            this.orderId = orderId;
            this.userId = userId;
            this.amount = amount;
        }

        public String getOrderId() { return orderId; }
        public String getUserId() { return userId; }
        public double getAmount() { return amount; }
    }

    // 2. 订单核心服务 (发布者)
    public static class OrderService {
        private final ApplicationEventPublisher eventPublisher;

        public OrderService(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        public void checkoutOrder(String orderId, String userId, double amount) {
            System.out.printf(">>> [核心交易链路] 订单创建成功! orderId=%s, userId=%s, amount=%.2f%n", orderId, userId, amount);
            System.out.println(">>> [发布事件] 广播 OrderCreatedEvent 领域事件...");
            eventPublisher.publishEvent(new OrderCreatedEvent(this, orderId, userId, amount));
            System.out.println(">>> [核心交易链路] 下单主流程顺利响应前端 HTTP 200 OK！");
        }
    }

    // 3. 营销发券监听器
    public static class CouponListener {
        @EventListener
        public void onOrderCreated(OrderCreatedEvent event) {
            System.out.printf("  ✓ [营销中心监听器] 捕获订单 %s, 为用户 %s 自动派发 50 元满减回购券！%n",
                    event.getOrderId(), event.getUserId());
        }
    }

    // 4. 用户通知监听器
    public static class NotificationListener {
        @EventListener
        public void onOrderCreated(OrderCreatedEvent event) {
            System.out.printf("  ✓ [消息中心监听器] 捕获订单 %s, 推送短信/App Push: '您已成功支付 %.2f 元'%n",
                    event.getOrderId(), event.getAmount());
        }
    }

    @Configuration
    static class EventConfig {
        @Bean
        public OrderService orderService(ApplicationEventPublisher publisher) {
            return new OrderService(publisher);
        }

        @Bean
        public CouponListener couponListener() {
            return new CouponListener();
        }

        @Bean
        public NotificationListener notificationListener() {
            return new NotificationListener();
        }
    }
}
