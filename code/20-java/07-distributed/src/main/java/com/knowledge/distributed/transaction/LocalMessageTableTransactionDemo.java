package com.knowledge.distributed.transaction;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

/**
 * 实验：分布式事务之本地消息表（Local Message Table）可靠最终一致性实战
 * 核心机制：本地业务与待发送消息同一本地事务提交，后台异步重试投递，下游幂等消费回执闭环
 */
public class LocalMessageTableTransactionDemo {

    public enum MessageStatus {
        PENDING, // 待投递
        SENT,    // 已投递未回执
        SUCCESS, // 消费成功已闭环
        DEAD     // 达到最大重试次数，转人工告警
    }

    public record LocalMessage(
            String id,
            String bizId,
            String payload,
            MessageStatus status,
            int retryCount,
            long nextRetryTime
    ) {}

    // 模拟本地数据库存储
    private static final List<String> ORDER_DB = Collections.synchronizedList(new ArrayList<>());
    private static final List<LocalMessage> MESSAGE_TABLE = new CopyOnWriteArrayList<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("【Lab 07-1】分布式事务 - 本地消息表最终一致性实战演练");
        System.out.println("================================================================================\n");

        // 1. 模拟核心下单业务 (同事务落库订单与本地消息)
        String orderId = "ORD-20261103-9999";
        createOrderWithLocalMessage(orderId, "{\"orderId\":\"" + orderId + "\",\"amount\":5999.00,\"points\":600}");

        // 2. 模拟后台调度扫描引擎 (定时拉取 PENDING 消息重试投递)
        ScheduledExecutorService dispatcher = Executors.newSingleThreadScheduledExecutor();
        CountDownLatch finishLatch = new CountDownLatch(1);

        System.out.println("\n--- 2. 后台轮询调度器启动，开始尝试将待发送消息投递至下游积分系统 ---");
        dispatcher.scheduleAtFixedRate(() -> {
            try {
                dispatchPendingMessages(finishLatch);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 100, 500, TimeUnit.MILLISECONDS);

        finishLatch.await(5, TimeUnit.SECONDS);
        dispatcher.shutdown();

        System.out.println("\n✔ 本地消息表全生命周期流转完毕，分布式最终一致性成功达成！\n");
    }

    /**
     * 模拟 @Transactional 本地数据库事务
     */
    private static void createOrderWithLocalMessage(String orderId, String payload) {
        System.out.println("--- 1. 执行本地数据库事务 (BEGIN TRANSACTION) ---");
        // 步骤 1: 写入订单表
        ORDER_DB.add(orderId);
        System.out.println("  ✔ [1/2] 写入订单表成功: orderId=" + orderId);

        // 步骤 2: 写入本地消息表 (同事务保证 100% 要么同时成功要么同时回滚)
        LocalMessage msg = new LocalMessage(
                "MSG-" + System.currentTimeMillis(),
                orderId,
                payload,
                MessageStatus.PENDING,
                0,
                System.currentTimeMillis()
        );
        MESSAGE_TABLE.add(msg);
        System.out.println("  ✔ [2/2] 写入本地消息表成功: msgId=" + msg.id() + ", status=PENDING");
        System.out.println("✔ 本地事务提交成功 (COMMIT)，哪怕此时系统断电崩溃，待发消息绝不丢失！");
    }

    private static void dispatchPendingMessages(CountDownLatch finishLatch) {
        long now = System.currentTimeMillis();
        for (int i = 0; i < MESSAGE_TABLE.size(); i++) {
            LocalMessage msg = MESSAGE_TABLE.get(i);
            if ((msg.status() == MessageStatus.PENDING || msg.status() == MessageStatus.SENT) && now >= msg.nextRetryTime()) {
                System.out.println("🚀 [调度扫描] 探测到待投递消息: id=" + msg.id() + ", 当前重试次数=" + msg.retryCount());

                // 模拟网络与下游消费 (前 1 次模拟网络超时，第 2 次成功)
                if (msg.retryCount() < 1) {
                    System.err.println("  ❌ [网络异常] 投递下游超时失败！执行指数退避 (Backoff): retryCount=" + (msg.retryCount() + 1));
                    MESSAGE_TABLE.set(i, new LocalMessage(
                            msg.id(), msg.bizId(), msg.payload(), MessageStatus.SENT,
                            msg.retryCount() + 1, now + 1000 // 1 秒后重试
                    ));
                } else {
                    System.out.println("  ✔ [下游回执] 下游积分系统成功处理并返回 ACK！");
                    MESSAGE_TABLE.set(i, new LocalMessage(
                            msg.id(), msg.bizId(), msg.payload(), MessageStatus.SUCCESS,
                            msg.retryCount() + 1, now
                    ));
                    finishLatch.countDown();
                }
            }
        }
    }
}
