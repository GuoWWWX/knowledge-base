package com.knowledge.pattern.creational;

/**
 * 创建型模式实战：建造者模式 (Builder) 与 抽象工厂模式 (Abstract Factory)
 * 1. 建造者模式 (Builder):
 *    - 解决多可选参数、重叠构造函数 (Telescoping Constructor) 与 JavaBean setter 导致的对象半初始化、状态不一致问题
 *    - 支持防御性参数校验、构建不可变 (Immutable) 客户端连接配置
 * 2. 抽象工厂模式 (Abstract Factory):
 *    - 解决产品族（Client + Authenticator）的整体解耦替换
 *    - 实战：多云架构下统一的存储服务工厂（Aliyun OSS 产品族 vs AWS S3 产品族）
 */
public class BuilderFactoryDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 建造者模式 (Builder Pattern) 构建高并发客户端配置 ==========");
        HttpClientConfig config = new HttpClientConfig.Builder()
                .baseUrl("https://api.knowledge.com")
                .connectTimeoutMs(3000)
                .readTimeoutMs(5000)
                .maxIdleConnections(200)
                .keepAliveDurationMinutes(5)
                .enableSsl(true)
                .addHeader("X-Client-Version", "v3.3.5")
                .addHeader("Authorization", "Bearer token-xyz")
                .build();

        System.out.println("成功构建不可变客户端配置: " + config);

        System.out.println("\n========== 2. 抽象工厂模式 (Abstract Factory) 统一多云接入 ==========");
        System.out.println("--- 场景 A: 生产环境配置切换为阿里云 OSS ---");
        CloudStorageFactory aliyunFactory = new AliyunCloudStorageFactory();
        CloudClient clientA = aliyunFactory.createClient();
        CloudAuthenticator authA = aliyunFactory.createAuthenticator();
        authA.authenticate("aliyun-key-1001");
        clientA.uploadFile("user_avatar.png", new byte[]{1, 2, 3});

        System.out.println("\n--- 场景 B: 海外业务一键平滑切换为 AWS S3 产品族 ---");
        CloudStorageFactory awsFactory = new AwsCloudStorageFactory();
        CloudClient clientB = awsFactory.createClient();
        CloudAuthenticator authB = awsFactory.createAuthenticator();
        authB.authenticate("aws-iam-role-arn");
        clientB.uploadFile("dataset.parquet", new byte[]{4, 5, 6});
    }

    // ================= 1. Builder 模式实现 =================
    public static class HttpClientConfig {
        private final String baseUrl;
        private final int connectTimeoutMs;
        private final int readTimeoutMs;
        private final int maxIdleConnections;
        private final int keepAliveDurationMinutes;
        private final boolean enableSsl;

        private HttpClientConfig(Builder builder) {
            this.baseUrl = builder.baseUrl;
            this.connectTimeoutMs = builder.connectTimeoutMs;
            this.readTimeoutMs = builder.readTimeoutMs;
            this.maxIdleConnections = builder.maxIdleConnections;
            this.keepAliveDurationMinutes = builder.keepAliveDurationMinutes;
            this.enableSsl = builder.enableSsl;
        }

        public static class Builder {
            private String baseUrl;
            private int connectTimeoutMs = 2000; // 默认值
            private int readTimeoutMs = 3000;
            private int maxIdleConnections = 50;
            private int keepAliveDurationMinutes = 1;
            private boolean enableSsl = true;

            public Builder baseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
                return this;
            }

            public Builder connectTimeoutMs(int timeout) {
                this.connectTimeoutMs = timeout;
                return this;
            }

            public Builder readTimeoutMs(int timeout) {
                this.readTimeoutMs = timeout;
                return this;
            }

            public Builder maxIdleConnections(int max) {
                this.maxIdleConnections = max;
                return this;
            }

            public Builder keepAliveDurationMinutes(int minutes) {
                this.keepAliveDurationMinutes = minutes;
                return this;
            }

            public Builder enableSsl(boolean enable) {
                this.enableSsl = enable;
                return this;
            }

            public Builder addHeader(String name, String value) {
                return this;
            }

            public HttpClientConfig build() {
                // 防御性校验
                if (baseUrl == null || baseUrl.isBlank()) {
                    throw new IllegalArgumentException("baseUrl 不能为空！");
                }
                if (connectTimeoutMs <= 0 || readTimeoutMs <= 0) {
                    throw new IllegalArgumentException("超时时间必须大于 0！");
                }
                return new HttpClientConfig(this);
            }
        }

        @Override
        public String toString() {
            return String.format("HttpClientConfig{url='%s', connect=%dms, read=%dms, maxConn=%d, ssl=%s}",
                    baseUrl, connectTimeoutMs, readTimeoutMs, maxIdleConnections, enableSsl);
        }
    }

    // ================= 2. 抽象工厂模式实现 =================
    public interface CloudClient {
        void uploadFile(String fileName, byte[] data);
    }

    public interface CloudAuthenticator {
        void authenticate(String credential);
    }

    // 抽象产品族工厂
    public interface CloudStorageFactory {
        CloudClient createClient();
        CloudAuthenticator createAuthenticator();
    }

    // 阿里云产品族
    public static class AliyunCloudStorageFactory implements CloudStorageFactory {
        @Override
        public CloudClient createClient() {
            return (fileName, data) -> System.out.printf("  ✓ [Aliyun OSS] 上传文件 [%s] 成功，字节大小: %d%n", fileName, data.length);
        }

        @Override
        public CloudAuthenticator createAuthenticator() {
            return cred -> System.out.printf("  ✓ [Aliyun RAM 鉴权] AccessKey 校验通过: %s%n", cred);
        }
    }

    // AWS 产品族
    public static class AwsCloudStorageFactory implements CloudStorageFactory {
        @Override
        public CloudClient createClient() {
            return (fileName, data) -> System.out.printf("  ✓ [AWS S3] 上传至 Bucket [%s] 成功，字节大小: %d%n", fileName, data.length);
        }

        @Override
        public CloudAuthenticator createAuthenticator() {
            return cred -> System.out.printf("  ✓ [AWS STS 鉴权] IAM AssumeRole 校验通过: %s%n", cred);
        }
    }
}
