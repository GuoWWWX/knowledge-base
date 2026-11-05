package com.knowledge.seckill.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 秒杀订单实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillOrder {

    /**
     * 全局唯一分布式订单号
     */
    private Long orderId;

    /**
     * 单次抢购幂等请求号
     */
    private String requestId;

    /**
     * 秒杀商品 ID
     */
    private Long goodsId;

    /**
     * 抢购用户 ID
     */
    private Long userId;

    /**
     * 实付金额
     */
    private BigDecimal payAmount;

    /**
     * 订单状态：WAIT_PAY(待支付), PAID(已支付), CLOSED(已取消)
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
