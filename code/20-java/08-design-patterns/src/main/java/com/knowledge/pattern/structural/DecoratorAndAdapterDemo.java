package com.knowledge.pattern.structural;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 结构型模式实战：装饰器模式 (Decorator) 与 适配器模式 (Adapter)
 * 1. 装饰器模式 (Decorator):
 *    - 核心思想：动态组合（继承同源接口，持有内部引用），在不改变原对象的基础上动态添加行为与职责
 *    - 经典对标：Java I/O (InputStream -> BufferedInputStream -> GZIPInputStream)
 *    - 业务实战：消息传输流的层层增强（基础传输 -> Gzip 压缩装饰器 -> Base64 加密装饰器 -> 耗时审计装饰器）
 * 2. 适配器模式 (Adapter):
 *    - 核心思想：结构转换，将不兼容接口转换为客户端期望的目标接口
 *    - 业务实战：第三方老旧银企直连 XML 协议报文适配为内部现代统一 JSON 数据模型
 */
public class DecoratorAndAdapterDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 装饰器模式 (Decorator) 动态增强数据处理管道 ==========");
        String rawPayload = "{\"orderId\":\"ORDER_20261103_888\",\"amount\":1299.00}";
        System.out.println("原始报文: " + rawPayload);

        // 像叠罗汉一样动态组装装饰器：原始传输 -> 加密装饰器 -> 压缩装饰器 -> 耗时度量装饰器
        MessageSender pipeline = new MetricsSenderDecorator(
                new Base64EncryptSenderDecorator(
                        new MockGzipCompressSenderDecorator(
                                new BasicHttpSender()
                        )
                )
        );

        pipeline.send(rawPayload);

        System.out.println("\n========== 2. 适配器模式 (Adapter) 统一遗留银企 XML 接口 ==========");
        LegacyBankXmlService legacyService = new LegacyBankXmlService();
        ModernPaymentGateway adapter = new BankXmlToModernJsonAdapter(legacyService);

        PaymentRequest request = new PaymentRequest("ACC_62220202", 5000.0, "CNY");
        PaymentResponse response = adapter.processPayment(request);
        System.out.println("适配器对外统一输出现代对象: " + response);
    }

    // ================= 1. 装饰器模式 =================
    public interface MessageSender {
        void send(String payload);
    }

    // 基础组件
    public static class BasicHttpSender implements MessageSender {
        @Override
        public void send(String payload) {
            System.out.printf("  -> [HTTP 底层发包] 最终向对端 Socket 写入字节流 (字节长度: %d)%n",
                    payload.getBytes(StandardCharsets.UTF_8).length);
        }
    }

    // 抽象装饰器 (继承并聚合)
    public static abstract class MessageSenderDecorator implements MessageSender {
        protected final MessageSender wrapped;

        public MessageSenderDecorator(MessageSender wrapped) {
            this.wrapped = wrapped;
        }

        @Override
        public void send(String payload) {
            wrapped.send(payload);
        }
    }

    // 压缩装饰器
    public static class MockGzipCompressSenderDecorator extends MessageSenderDecorator {
        public MockGzipCompressSenderDecorator(MessageSender wrapped) {
            super(wrapped);
        }

        @Override
        public void send(String payload) {
            System.out.println("  [Decorator - Gzip] 正在对报文执行 GZIP 压缩，体积减少 65%...");
            String compressed = "[GZIP]" + payload;
            super.send(compressed);
        }
    }

    // 加密装饰器
    public static class Base64EncryptSenderDecorator extends MessageSenderDecorator {
        public Base64EncryptSenderDecorator(MessageSender wrapped) {
            super(wrapped);
        }

        @Override
        public void send(String payload) {
            System.out.println("  [Decorator - Base64] 正在对压缩报文进行防篡改安全编码...");
            String encoded = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            super.send(encoded);
        }
    }

    // 耗时审计装饰器
    public static class MetricsSenderDecorator extends MessageSenderDecorator {
        public MetricsSenderDecorator(MessageSender wrapped) {
            super(wrapped);
        }

        @Override
        public void send(String payload) {
            long start = System.nanoTime();
            try {
                System.out.println("  [Decorator - Metrics] 开启全链路耗时与吞吐监控打点...");
                super.send(payload);
            } finally {
                long duration = System.nanoTime() - start;
                System.out.printf("  [Decorator - Metrics] 管道处理完毕，总耗时: %.2f ms%n", duration / 1_000_000.0);
            }
        }
    }

    // ================= 2. 适配器模式 =================
    public record PaymentRequest(String accountNo, double amount, String currency) {}
    public record PaymentResponse(String status, String transactionId, String message) {}

    // 遗留老旧系统的 XML 服务接口
    public static class LegacyBankXmlService {
        public String executeXmlTransfer(String xmlString) {
            System.out.println("  [老旧银行核心系统] 接收到原始 XML 报文:\n  " + xmlString);
            return "<response><status>SUCCESS</status><txId>TX_BANK_999</txId><msg>OK</msg></response>";
        }
    }

    // 现代统一微服务标准接口
    public interface ModernPaymentGateway {
        PaymentResponse processPayment(PaymentRequest request);
    }

    // 对象适配器
    public static class BankXmlToModernJsonAdapter implements ModernPaymentGateway {
        private final LegacyBankXmlService legacyService;

        public BankXmlToModernJsonAdapter(LegacyBankXmlService legacyService) {
            this.legacyService = legacyService;
        }

        @Override
        public PaymentResponse processPayment(PaymentRequest request) {
            // 将现代对象适配转换为老旧系统的 XML 字符串
            String xmlPayload = String.format(
                    "<transfer><account>%s</account><amt>%.2f</amt><cur>%s</cur></transfer>",
                    request.accountNo(), request.amount(), request.currency());

            String xmlResponse = legacyService.executeXmlTransfer(xmlPayload);

            // 将老旧系统的 XML 返回值适配还原为现代微服务对象
            return new PaymentResponse("SUCCESS", "TX_BANK_999", "银行接口调用成功");
        }
    }
}
