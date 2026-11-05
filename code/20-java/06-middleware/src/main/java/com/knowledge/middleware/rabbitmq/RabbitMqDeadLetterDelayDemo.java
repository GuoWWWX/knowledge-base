package com.knowledge.middleware.rabbitmq;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 实验：RabbitMQ 死信交换机（DLX）与电商未支付超时关单延迟队列实战
 */
public class RabbitMqDeadLetterDelayDemo {

    private static final String DLX_EXCHANGE = "ex.deadletter";
    private static final String DLX_QUEUE = "q.deadletter.order.cancel";
    private static final String DLX_ROUTING_KEY = "order.cancel";

    private static final String DELAY_EXCHANGE = "ex.delay.order";
    private static final String DELAY_QUEUE = "q.delay.order.ttl";
    private static final String DELAY_ROUTING_KEY = "order.delay";

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("【Lab 06-2】RabbitMQ 死信交换机（DLX）与订单超时延迟关单实战");
        System.out.println("================================================================================\n");

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setConnectionTimeout(1500);

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            System.out.println("✔ 成功连接 RabbitMQ，正在搭建 DLX 死信与延迟关单拓扑架构...\n");
            runDeadLetterDelayExperiment(channel);

        } catch (Exception e) {
            System.out.println("⚠ 连接本地 RabbitMQ 异常 (" + e.getMessage() + ")，启动仿真延迟队列演练...\n");
            runSimulatedDelayQueue();
        }
    }

    private static void runDeadLetterDelayExperiment(Channel channel) throws IOException, InterruptedException {
        // 1. 声明死信交换机与死信消费者队列
        channel.exchangeDeclare(DLX_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        channel.queueDeclare(DLX_QUEUE, true, false, false, null);
        channel.queueBind(DLX_QUEUE, DLX_EXCHANGE, DLX_ROUTING_KEY);

        // 2. 声明带 TTL 与绑死信交换机的延迟缓冲队列 (无人消费)
        Map<String, Object> queueArgs = new HashMap<>();
        queueArgs.put("x-dead-letter-exchange", DLX_EXCHANGE);
        queueArgs.put("x-dead-letter-routing-key", DLX_ROUTING_KEY);
        queueArgs.put("x-message-ttl", 3000); // 实验设置 3000ms (3秒超时自动关单，生产中一般设置为 30分钟)

        channel.exchangeDeclare(DELAY_EXCHANGE, BuiltinExchangeType.DIRECT, true);
        channel.queueDeclare(DELAY_QUEUE, true, false, false, queueArgs);
        channel.queueBind(DELAY_QUEUE, DELAY_EXCHANGE, DELAY_ROUTING_KEY);

        // 3. 生产端投递一笔创建的订单
        String orderMsg = "{\"orderNo\":\"ORD-DELAY-8888\",\"createTime\":System.currentTimeMillis()}";
        channel.basicPublish(DELAY_EXCHANGE, DELAY_ROUTING_KEY, MessageProperties.PERSISTENT_TEXT_PLAIN, orderMsg.getBytes(StandardCharsets.UTF_8));
        long sendTime = System.currentTimeMillis();
        System.out.println("📤 [下单成功] 订单入缓冲队列: " + orderMsg + "，倒计时 3000ms 等待用户付款...");

        // 4. 监听死信关单队列 (DLX_QUEUE)
        CountDownLatch latch = new CountDownLatch(1);
        channel.basicConsume(DLX_QUEUE, false, new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                long receiveTime = System.currentTimeMillis();
                String content = new String(body, StandardCharsets.UTF_8);
                System.out.println("\n💀 [死信触发 - 延迟关单监听] 耗时: " + (receiveTime - sendTime) + "ms");
                System.out.println("🔔 [自动关单逻辑] 监听到超时未支付订单: " + content);
                System.out.println("⚙ 执行动作: 查询支付状态 -> 未支付 -> 回滚预扣库存 -> 将订单置为 CANCELLED 状态");
                channel.basicAck(envelope.getDeliveryTag(), false);
                latch.countDown();
            }
        });

        latch.await(6, TimeUnit.SECONDS);
    }

    private static void runSimulatedDelayQueue() {
        System.out.println("🔹 [死信三大诱发成因剖析]：");
        System.out.println("   1. 消费端 basicReject(requeue=false) 或 basicNack(requeue=false) 显式拒收；");
        System.out.println("   2. 消息在队列中等待超时 (TTL Expiration)；");
        System.out.println("   3. 队列达到 x-max-length 最大容量上限，队头溢出丢弃。");
        System.out.println("✔ 电商延迟关单设计：将消息发往无消费者的缓冲队列，绑定 DLX，TTL 到期后自动流转至死信队列执行关单解冻库存！\n");
    }
}
