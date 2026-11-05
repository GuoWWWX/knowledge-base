package com.knowledge.spring.autoconfig;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

/**
 * Spring Boot 自动装配原理与手写 Starter：
 * 1. 核心注解体系：@Configuration + @ConditionalOnProperty + @ConditionalOnMissingBean
 * 2. 演练当配置项 enabled=true 时自动注入默认 Client；当用户自定义配置时自动退让（@ConditionalOnMissingBean）
 * 3. 揭秘 Spring Boot 3+ META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports 的 SPI 加载机制
 */
public class SpringBootAutoConfigurationInspectDemo {

    public static void main(String[] args) {
        System.out.println("========== 场景 1: 未开启配置 (mock.client.enabled=false)，自动装配跳过 ==========");
        testContextWithProperties("mock.client.enabled", "false", false);

        System.out.println("\n========== 场景 2: 开启配置 (mock.client.enabled=true)，自动装配生效默认 Bean ==========");
        testContextWithProperties("mock.client.enabled", "true", false);

        System.out.println("\n========== 场景 3: 用户自定义了 Bean，@ConditionalOnMissingBean 自动退让 ==========");
        testContextWithProperties("mock.client.enabled", "true", true);
    }

    private static void testContextWithProperties(String propKey, String propVal, boolean hasUserBean) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("testProps", Map.of(propKey, propVal)));
        context.setEnvironment(env);

        if (hasUserBean) {
            context.register(UserCustomConfig.class);
        }
        context.register(MockAutoConfiguration.class);
        context.refresh();

        boolean contains = context.containsBean("mockSmsClient");
        System.out.println("Spring 容器中是否存在 mockSmsClient: " + contains);
        if (contains) {
            MockSmsClient client = context.getBean(MockSmsClient.class);
            System.out.println("提取到的 Bean 类型与标识: " + client.getSignature());
        }
        context.close();
    }

    // 自动装配类
    @Configuration
    @ConditionalOnProperty(prefix = "mock.client", name = "enabled", havingValue = "true")
    static class MockAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean(MockSmsClient.class)
        public MockSmsClient mockSmsClient() {
            return new MockSmsClient("【官方默认 Starter 自动装配客户端】");
        }
    }

    // 用户自定义配置
    @Configuration
    static class UserCustomConfig {
        @Bean
        public MockSmsClient mockSmsClient() {
            return new MockSmsClient("【业务方深度定制版 SmsClient】");
        }
    }

    static class MockSmsClient {
        private final String signature;

        public MockSmsClient(String signature) {
            this.signature = signature;
        }

        public String getSignature() {
            return signature;
        }
    }
}
