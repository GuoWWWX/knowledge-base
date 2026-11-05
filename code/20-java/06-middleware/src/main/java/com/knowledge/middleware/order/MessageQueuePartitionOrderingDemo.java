package com.knowledge.middleware.order;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 消息队列单分区严格顺序消费模型：
 * 1. 业务痛点：多并发消费者消费订单状态变更（创建 -> 支付 -> 发货 -> 完成），由于网络延迟或处理速度差异，极易导致“发货”先于“创建”处理，引发状态机错乱！
 * 2. 解决方案：
 *    - 生产者端：依据业务唯一 Key (如 orderId) 计算哈希，严格路由到固定的单个分区 (Partition / Queue)
 *    - 消费端：单分区严格单线程 (或内存单 Worker 队列) 串行消费，确保相同 Key 的消息绝对保序！
 */
public class MessageQueuePartitionOrderingDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 分布式消息队列分区保序消费实战 ==========");
        PartitionedMessageBroker broker = new PartitionedMessageBroker(4); // 4 个分区
        broker.startConsumers();

        // 构造两个订单的交错并发事件
        String order1 = "ORDER_100001";
        String order2 = "ORDER_100002";

        System.out.println("--- 投递两个订单的生命周期事件 ---");
        broker.send(new OrderEvent(order1, "1. 创建订单"));
        broker.send(new OrderEvent(order2, "1. 创建订单"));
        broker.send(new OrderEvent(order1, "2. 支付成功"));
        broker.send(new OrderEvent(order2, "2. 支付成功"));
        broker.send(new OrderEvent(order1, "3. 仓储发货"));
        broker.send(new OrderEvent(order2, "3. 仓储发货"));
        broker.send(new OrderEvent(order1, "4. 确认收货"));
        broker.send(new OrderEvent(order2, "4. 确认收货"));

        TimeUnit.SECONDS.sleep(1);
        broker.shutdown();

        System.out.println(">>> 结论: 相同 orderId 的所有事件严格哈希绑定在同一分区内串行消费，彻底杜绝状态乱序！");
    }

    public record OrderEvent(String orderId, String action) {}

    public static class PartitionedMessageBroker {
        private final int partitionCount;
        private final List<BlockingQueue<OrderEvent>> partitions = new ArrayList<>();
        private final ExecutorService executor;

        public PartitionedMessageBroker(int partitionCount) {
            this.partitionCount = partitionCount;
            this.executor = Executors.newFixedThreadPool(partitionCount);
            for (int i = 0; i < partitionCount; i++) {
                partitions.add(new LinkedBlockingQueue<>());
            }
        }

        public void send(OrderEvent event) {
            // 根据 orderId 计算哈希分片
            int partitionIndex = Math.abs(event.orderId().hashCode()) % partitionCount;
            partitions.get(partitionIndex).offer(event);
            System.out.printf("[Producer] 消息 [%s - %s] -> 路由至 Partition-%d%n",
                    event.orderId(), event.action(), partitionIndex);
        }

        public void startConsumers() {
            for (int i = 0; i < partitionCount; i++) {
                final int partitionId = i;
                final BlockingQueue<OrderEvent> queue = partitions.get(i);
                executor.submit(() -> {
                    try {
                        while (!Thread.currentThread().isInterrupted()) {
                            OrderEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                            if (event != null) {
                                System.out.printf("  ✓ [Consumer-Worker-%d] 严格顺序处理: %s -> %s%n",
                                        partitionId, event.orderId(), event.action());
                                Thread.sleep(20); // 模拟业务处理
                            }
                        }
                    } catch (InterruptedException ignored) {}
                });
            }
        }

        public void shutdown() {
            executor.shutdownNow();
        }
    }
}
