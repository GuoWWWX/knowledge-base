-- ============================================================
-- 知识库工程基础设施初始化数据库脚本
-- 数据库: knowledge_db
-- ============================================================

CREATE DATABASE IF NOT EXISTS `knowledge_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `knowledge_db`;

-- 1. 高并发秒杀商品表 (严格遵循实物守恒定律)
DROP TABLE IF EXISTS `seckill_good`;
CREATE TABLE `seckill_good` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品主键 ID',
    `title` VARCHAR(128) NOT NULL COMMENT '商品标题',
    `price` DECIMAL(10, 2) NOT NULL COMMENT '秒杀价格',
    `total_stock` INT NOT NULL DEFAULT 0 COMMENT '初始总库存',
    `remain_stock` INT NOT NULL DEFAULT 0 COMMENT '可售可用剩余库存',
    `frozen_stock` INT NOT NULL DEFAULT 0 COMMENT '锁定冻结库存',
    `sold_stock` INT NOT NULL DEFAULT 0 COMMENT '最终已售出库存',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀商品库存表';

-- 预置秒杀测试商品
INSERT INTO `seckill_good` (`id`, `title`, `price`, `total_stock`, `remain_stock`, `frozen_stock`, `sold_stock`, `version`)
VALUES 
(1001, 'iPhone 16 Pro Max 256GB 黑色钛金属', 9999.00, 10, 10, 0, 0, 0),
(1002, 'MacBook Pro 16 M3 Max 36GB', 22999.00, 5, 5, 0, 0, 0)
ON DUPLICATE KEY UPDATE `title`=VALUES(`title`);

-- 2. 秒杀订单流水表 (联合唯一索引防重刷)
DROP TABLE IF EXISTS `seckill_order`;
CREATE TABLE `seckill_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单自增 ID',
    `order_no` VARCHAR(64) NOT NULL COMMENT '全局唯一业务订单号',
    `request_id` VARCHAR(64) NOT NULL COMMENT '客户端防重请求幂等号',
    `user_id` BIGINT NOT NULL COMMENT '购买用户 ID',
    `good_id` BIGINT NOT NULL COMMENT '秒杀商品 ID',
    `pay_amount` DECIMAL(10, 2) NOT NULL COMMENT '实际支付金额',
    `status` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '订单状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_user_good` (`user_id`, `good_id`),
    KEY `idx_request_id` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秒杀订单表';

-- 3. 高性能短链映射表
DROP TABLE IF EXISTS `short_link`;
CREATE TABLE `short_link` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增 ID',
    `short_code` VARCHAR(16) NOT NULL COMMENT '62进制压缩短码 (如 aB12z9)',
    `original_url` VARCHAR(2048) NOT NULL COMMENT '原始长链接',
    `click_pv` BIGINT NOT NULL DEFAULT 0 COMMENT '累计访问点击量',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 1-启用, 0-禁用',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_short_code` (`short_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短链核心映射表';

-- 预置示例短链
INSERT INTO `short_link` (`short_code`, `original_url`, `click_pv`, `status`)
VALUES 
('github', 'https://github.com/spring-projects/spring-ai', 128, 1),
('docs', 'https://docs.spring.io/spring-boot/docs/current/reference/html/', 256, 1)
ON DUPLICATE KEY UPDATE `original_url`=VALUES(`original_url`);

-- 4. 消息队列消费幂等防重表
DROP TABLE IF EXISTS `mq_consumed_record`;
CREATE TABLE `mq_consumed_record` (
    `message_id` VARCHAR(64) NOT NULL COMMENT 'MQ 消息唯一标识',
    `consumer_group` VARCHAR(64) NOT NULL COMMENT '消费者组标识',
    `biz_key` VARCHAR(64) NOT NULL COMMENT '业务主键',
    `status` VARCHAR(32) NOT NULL DEFAULT 'SUCCESS' COMMENT '消费处理状态',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消费时间',
    PRIMARY KEY (`message_id`, `consumer_group`),
    KEY `idx_biz_key` (`biz_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='MQ消费幂等防重记录表';

-- 5. 分布式事务本地消息表 (可靠消息最终一致性)
DROP TABLE IF EXISTS `local_message_record`;
CREATE TABLE `local_message_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增 ID',
    `biz_id` VARCHAR(64) NOT NULL COMMENT '业务订单或主单 ID',
    `biz_type` VARCHAR(32) NOT NULL COMMENT '业务类型: ORDER_PAID, INVENTORY_DEDUCTED',
    `destination` VARCHAR(64) NOT NULL COMMENT '目标交换机或主题',
    `routing_key` VARCHAR(64) NOT NULL COMMENT '路由键',
    `payload` TEXT NOT NULL COMMENT '消息 JSON 体',
    `status` VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING, SENT, SUCCESS, DEAD',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '已重试投递次数',
    `next_retry_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下次允许重试时间',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_status_retry` (`status`, `next_retry_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本地消息表';

-- 6. 账户余额表 (用于行锁、间隙锁与死锁现场演示)
DROP TABLE IF EXISTS `account_balance`;
CREATE TABLE `account_balance` (
    `id` BIGINT NOT NULL COMMENT '主键 (演示间隙锁，预置不连续 ID)',
    `account_name` VARCHAR(64) NOT NULL COMMENT '账户名',
    `balance` DECIMAL(12, 2) NOT NULL DEFAULT 0.00 COMMENT '账户余额',
    `version` INT NOT NULL DEFAULT 0 COMMENT '版本号',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户资金表 (用于死锁与锁冲突实验)';

INSERT INTO `account_balance` (`id`, `account_name`, `balance`, `version`)
VALUES 
(1, 'Alice', 10000.00, 0),
(5, 'Bob', 20000.00, 0),
(10, 'Charlie', 30000.00, 0),
(15, 'David', 40000.00, 0)
ON DUPLICATE KEY UPDATE `account_name`=VALUES(`account_name`);
