package com.knowledge.seckill.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillRequest {

    /**
     * 商品/活动 ID
     */
    private Long goodsId;

    /**
     * 用户 ID
     */
    private Long userId;

    /**
     * 客户端请求幂等 ID（可选，若未传则由服务端生成）
     */
    private String requestId;
}
