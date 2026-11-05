package com.knowledge.seckill;

import com.knowledge.seckill.domain.SeckillGood;
import com.knowledge.seckill.service.SeckillService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

/**
 * 高并发秒杀系统启动类
 */
@SpringBootApplication
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }

    /**
     * 容器启动后自动预热一件库存为 10 的秒杀测试商品
     */
    @Bean
    public CommandLineRunner initDefaultGoods(SeckillService seckillService) {
        return args -> {
            SeckillGood testGood = SeckillGood.builder()
                    .goodsId(1001L)
                    .goodsName("Apple iPhone 16 Pro Max 1TB 钛金属黑")
                    .price(new BigDecimal("13999.00"))
                    .seckillPrice(new BigDecimal("999.00"))
                    .initialStock(10)
                    .remainStock(10)
                    .frozenStock(0)
                    .soldStock(0)
                    .build();
            seckillService.initGoods(testGood);
            System.out.println("==========================================================");
            System.out.println("【秒杀系统】开箱就绪！测试商品 1001 预热完成（库存: 10 件）");
            System.out.println("秒杀下单接口：POST http://localhost:8081/api/seckill/doSeckill");
            System.out.println("==========================================================");
        };
    }
}
