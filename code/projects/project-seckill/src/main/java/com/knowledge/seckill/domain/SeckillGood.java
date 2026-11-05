package com.knowledge.seckill.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 秒杀商品模型
 * 恒等平衡公式：totalStock = remainStock + frozenStock + soldStock
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillGood {

    /**
     * 商品/活动 ID
     */
    private Long goodsId;

    /**
     * 商品名称
     */
    private String goodsName;

    /**
     * 原始价格
     */
    private BigDecimal price;

    /**
     * 秒杀特惠价
     */
    private BigDecimal seckillPrice;

    /**
     * 初始总库存
     */
    private Integer initialStock;

    /**
     * 数据库当前可用库存
     */
    private Integer remainStock;

    /**
     * 冻结中库存（下单未支付）
     */
    private Integer frozenStock;

    /**
     * 已支付/已完成出库库存
     */
    private Integer soldStock;
}
