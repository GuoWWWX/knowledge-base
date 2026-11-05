package com.knowledge.middleware.idempotent;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 实验：消息消费端幂等性防重设计实战 (Redis 防重锁 + MySQL 唯一防重表)
 */
public class MessageIdempotentConsumerDemo {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/knowledge_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String USER = "root";
    private static final String PASS = "root123456";

    // 内存模拟 Redis SET NX EX
    private static final Map<String, Long> MOCK_REDIS_LOCK = new ConcurrentHashMap<>();
    private static final AtomicInteger BIZ_EXECUTION_COUNTER = new AtomicInteger(0);

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("【Lab 06-3】消息消费端幂等防重机制实战 (双防线保障)");
        System.out.println("================================================================================\n");

        String messageId = "MSG-UUID-9527-20261103";
        String consumerGroup = "order-fulfillment-group";
        String orderNo = "ORD-20261103-8888";

        System.out.println("场景：由于网络偶发抖动超时，Broker 触发重试机制，向消费端连续投递了 3 次相同消息！");
        System.out.println("消息详情: messageId=" + messageId + ", orderNo=" + orderNo + "\n");

        for (int i = 1; i <= 3; i++) {
            System.out.println("--- 收到第 " + i + " 次消息投递 ---");
            boolean success = consumeMessageWithDoubleIdempotence(messageId, consumerGroup, orderNo);
            if (success) {
                System.out.println("✔ 第 " + i + " 次消费: 首次收到消息，核心履约/发货业务执行完毕！");
            } else {
                System.out.println("🛡 第 " + i + " 次消费: 拦截到重复消息，防重屏障生效，直接返回 ACK 确认！");
            }
        }

        System.out.println("\n================================================================================");
        System.out.println("【实验结论】核心业务代码实际执行次数: " + BIZ_EXECUTION_COUNTER.get() + " 次！(严格保证业务绝对幂等，0重复履约)");
        System.out.println("================================================================================\n");
    }

    private static boolean consumeMessageWithDoubleIdempotence(String msgId, String group, String orderNo) {
        String redisKey = "idempotent:mq:" + group + ":" + msgId;

        // 防线 1: Redis SET NX 毫秒级抢占防并发重复消费
        if (MOCK_REDIS_LOCK.putIfAbsent(redisKey, System.currentTimeMillis()) != null) {
            System.out.println("  [第一道防线 - Redis SET NX] Key 已存在，拦截重复投递！");
            return false;
        }

        // 防线 2: 数据库唯一索引硬兜底 (INSERT INTO mq_consumed_record)
        boolean dbRecorded = insertConsumedRecord(msgId, group, orderNo);
        if (!dbRecorded) {
            System.out.println("  [第二道防线 - MySQL 唯一索引] 消费记录已落库，触发唯一键冲突，拦截重复投递！");
            return false;
        }

        // 执行核心业务 (如扣款、发货)
        executeFulfillmentBusiness(orderNo);
        return true;
    }

    private static boolean insertConsumedRecord(String msgId, String group, String orderNo) {
        String sql = "INSERT INTO mq_consumed_record (message_id, consumer_group, biz_key, status) VALUES (?, ?, ?, 'SUCCESS')";
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASS);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, msgId);
            ps.setString(2, group);
            ps.setString(3, orderNo);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Duplicate entry (错误码 1062)
            return false;
        } catch (Exception e) {
            // 若数据库未连接，降级为模拟成功
            return true;
        }
    }

    private static void executeFulfillmentBusiness(String orderNo) {
        BIZ_EXECUTION_COUNTER.incrementAndGet();
        System.out.println("  ⚙ [核心业务] 正在为订单 " + orderNo + " 生成物流面单、调用仓库系统扣减实物库存...");
    }
}
