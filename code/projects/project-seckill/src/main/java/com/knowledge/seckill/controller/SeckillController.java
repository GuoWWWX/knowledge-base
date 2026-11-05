package com.knowledge.seckill.controller;

import com.knowledge.seckill.domain.SeckillGood;
import com.knowledge.seckill.domain.SeckillOrder;
import com.knowledge.seckill.domain.SeckillRequest;
import com.knowledge.seckill.domain.SeckillResult;
import com.knowledge.seckill.service.SeckillService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 秒杀业务对外 REST 接口
 */
@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillController {

    private final SeckillService seckillService;

    /**
     * 高并发秒杀下单入口
     * POST /api/seckill/doSeckill
     */
    @PostMapping("/doSeckill")
    public SeckillResult<?> doSeckill(@RequestBody SeckillRequest request) {
        if (request.getGoodsId() == null || request.getUserId() == null) {
            return SeckillResult.fail(400, "参数错误：goodsId 与 userId 不能为空");
        }
        return seckillService.doSeckill(request);
    }

    /**
     * 查询订单创建状态
     * GET /api/seckill/order?goodsId=1001&userId=10
     */
    @GetMapping("/order")
    public SeckillResult<?> queryOrder(@RequestParam Long goodsId, @RequestParam Long userId) {
        SeckillOrder order = seckillService.queryOrderByUser(goodsId, userId);
        if (order == null) {
            return SeckillResult.fail(404, "订单尚未生成或排队中");
        }
        return SeckillResult.success(order);
    }

    /**
     * 查询秒杀活动库存及状态对账
     * GET /api/seckill/stock?goodsId=1001
     */
    @GetMapping("/stock")
    public SeckillResult<?> queryStock(@RequestParam Long goodsId) {
        Map<String, Object> data = new HashMap<>();
        data.put("goodsId", goodsId);
        data.put("redisStock", seckillService.getRedisStock(goodsId));
        data.put("databaseRemainStock", seckillService.getDatabaseRemainStock(goodsId));
        data.put("createdOrderCount", seckillService.getCreatedOrderCount(goodsId));
        data.put("isLocalOverSold", seckillService.isLocalOverSold(goodsId));
        return SeckillResult.success(data);
    }

    /**
     * 活动初始化/预热测试接口
     * POST /api/seckill/init
     */
    @PostMapping("/init")
    public SeckillResult<?> initGoods(@RequestBody(required = false) SeckillGood good) {
        if (good == null) {
            good = SeckillGood.builder()
                    .goodsId(1001L)
                    .goodsName("iPhone 16 Pro Max 限量抢购")
                    .price(new BigDecimal("9999.00"))
                    .seckillPrice(new BigDecimal("999.00"))
                    .initialStock(10)
                    .remainStock(10)
                    .frozenStock(0)
                    .soldStock(0)
                    .build();
        }
        seckillService.initGoods(good);
        return SeckillResult.success("秒杀活动初始化成功", good);
    }
}
