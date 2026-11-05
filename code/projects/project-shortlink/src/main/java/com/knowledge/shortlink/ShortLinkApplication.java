package com.knowledge.shortlink;

import com.knowledge.shortlink.service.ShortLinkService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * 高性能短链系统 Spring Boot 启动类
 */
@SpringBootApplication
public class ShortLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShortLinkApplication.class, args);
    }

    /**
     * 容器启动后自动预热并打印访问信息
     */
    @Bean
    public CommandLineRunner initDemoShortLinks(ShortLinkService shortLinkService) {
        return args -> {
            String sampleLongUrl = "https://github.com/spring-projects/spring-boot/releases/tag/v3.3.5";
            String code = shortLinkService.createShortLink(sampleLongUrl);
            System.out.println("================================================================================");
            System.out.println("【高性能短链服务】启动成功！已预热示范短链：");
            System.out.println("原始长链接: " + sampleLongUrl);
            System.out.println("生成短链码: " + code);
            System.out.println("访问重定向: http://localhost:8082/" + code);
            System.out.println("短链创建接口: POST http://localhost:8082/api/shortlink/create");
            System.out.println("================================================================================");
        };
    }
}
