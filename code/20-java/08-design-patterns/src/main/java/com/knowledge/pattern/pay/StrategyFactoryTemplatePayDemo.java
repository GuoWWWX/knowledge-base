package com.knowledge.pattern.pay;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生产级架构实战：策略模式 + 工厂模式 + 模板方法模式三合一支付结算引擎
 *
 * 架构设计全景：
 * 1. 策略模式 (Strategy)：
 *    - 消除冗长丑陋的 if-else / switch 渠道分支；
 *    - 将各支付渠道的异构行为抽象为统一接口 PaymentStrategy，方便随时横向扩展新渠道（如 ApplePay、数字人民币）。
 *
 * 2. 工厂模式 (Factory)：
 *    - PaymentStrategyFactory 维护渠道枚举与策略实例的注册映射表；
 *    - 统一提供根据渠道标识获取策略实例的路由能力，实现策略的解耦创建与定位。
 *
 * 3. 模板方法模式 (Template Method)：
 *    - AbstractPaymentTemplate 固化支付通用算法骨架：
 *      入参基础校验 -> 风控与防重校验 -> 渠道特定优惠算价 -> 核心扣款执行 -> 后置审计与对账通知；
 *    - 核心业务骨架用 final 锁定，渠道特性（算价策略、扣款逻辑、特定风控）开放给具体策略子类扩展。
 */
public class StrategyFactoryTemplatePayDemo {

    // ======================= 1. 领域模型定义 =======================

    public enum PayChannelEnum {
        WECHAT_PAY("WECHAT", "微信支付"),
        ALIPAY("ALIPAY", "支付宝"),
        UNION_PAY("UNION", "银联快捷支付");

        private final String code;
        private final String desc;

        PayChannelEnum(String code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public String getCode() { return code; }
        public String getDesc() { return desc; }

        public static PayChannelEnum fromCode(String code) {
            for (PayChannelEnum c : values()) {
                if (c.code.equalsIgnoreCase(code)) {
                    return c;
                }
            }
            throw new IllegalArgumentException("未知的支付渠道编码: " + code);
        }
    }

    public record PaymentRequest(
            String orderId,
            String userId,
            BigDecimal amount,
            PayChannelEnum channel,
            Map<String, String> extraParams
    ) {}

    public record PaymentResult(
            boolean success,
            String orderId,
            String tradeNo,
            BigDecimal actualAmount,
            String code,
            String message
    ) {
        public static PaymentResult success(String orderId, String tradeNo, BigDecimal actualAmount) {
            return new PaymentResult(true, orderId, tradeNo, actualAmount, "SUCCESS", "支付成功");
        }

        public static PaymentResult fail(String orderId, String code, String message) {
            return new PaymentResult(false, orderId, null, BigDecimal.ZERO, code, message);
        }
    }

    // ======================= 2. 策略模式接口定义 =======================

    public interface PaymentStrategy {
        PayChannelEnum getChannel();
        PaymentResult pay(PaymentRequest request);
    }

    // ======================= 3. 模板方法模式核心骨架 =======================

    public abstract static class AbstractPaymentTemplate implements PaymentStrategy {

        /**
         * 模板方法：final 声明，固定支付全生命周期的执行顺序
         */
        @Override
        public final PaymentResult pay(PaymentRequest request) {
            long startTime = System.currentTimeMillis();
            System.out.println("----------------------------------------------------------------------");
            System.out.printf("▶ [支付流程启动] 渠道: [%s], 订单号: [%s], 请求金额: ￥%s\n",
                    getChannel().getDesc(), request.orderId(), request.amount());

            try {
                // 步骤 1：入参基础合法性校验（通用逻辑）
                validateRequest(request);

                // 步骤 2：风控安全与限额检查（钩子方法，子类可覆盖定制）
                checkRiskControl(request);

                // 步骤 3：计算实付金额（钩子方法，支持渠道立减/优惠券叠加）
                BigDecimal actualAmount = calculateActualAmount(request);
                System.out.printf("  [Step 3 算价中心] 原始金额: ￥%s, 优惠后实付金额: ￥%s\n",
                        request.amount(), actualAmount);

                // 步骤 4：调用渠道网关执行真实扣款（抽象方法，强制各渠道子类实现）
                String tradeNo = executeChannelDeduction(request, actualAmount);
                System.out.printf("  [Step 4 渠道扣款] 扣款成功，生成外部交易流水号: %s\n", tradeNo);

                // 组装成功结果
                PaymentResult result = PaymentResult.success(request.orderId(), tradeNo, actualAmount);

                // 步骤 5：后置处理（流水落库、履约通知、MQ消息）
                postPaySuccess(request, result);

                long cost = System.currentTimeMillis() - startTime;
                System.out.printf("✔ [支付成功归档] 订单: %s 处理完毕，耗时: %d ms\n", request.orderId(), cost);
                return result;

            } catch (RiskException e) {
                System.err.printf("  ✖ [风控拦截] %s (订单: %s)\n", e.getMessage(), request.orderId());
                PaymentResult failResult = PaymentResult.fail(request.orderId(), "RISK_REJECT", e.getMessage());
                postPayFailure(request, failResult);
                return failResult;
            } catch (Exception e) {
                System.err.printf("  ✖ [系统异常] 支付失败: %s (订单: %s)\n", e.getMessage(), request.orderId());
                PaymentResult failResult = PaymentResult.fail(request.orderId(), "SYSTEM_ERROR", e.getMessage());
                postPayFailure(request, failResult);
                return failResult;
            }
        }

        // --- 内部通用实现与钩子方法 ---

        protected void validateRequest(PaymentRequest request) {
            System.out.println("  [Step 1 基础校验] 验证订单号、商户号与正向金额合法性...");
            if (request.orderId() == null || request.orderId().isBlank()) {
                throw new IllegalArgumentException("订单号不能为空！");
            }
            if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("支付金额必须大于 0！");
            }
        }

        /**
         * 钩子方法：风控校验，默认提供通用限额检查，子类可扩展
         */
        protected void checkRiskControl(PaymentRequest request) {
            System.out.println("  [Step 2 风控校验] 执行全局通用反欺诈与黑名单扫描通过。");
        }

        /**
         * 钩子方法：计算实付金额，默认无优惠
         */
        protected BigDecimal calculateActualAmount(PaymentRequest request) {
            return request.amount();
        }

        /**
         * 抽象方法：具体渠道的扣款指令下发
         */
        protected abstract String executeChannelDeduction(PaymentRequest request, BigDecimal actualAmount);

        /**
         * 钩子方法：支付成功后置处理
         */
        protected void postPaySuccess(PaymentRequest request, PaymentResult result) {
            System.out.printf("  [Step 5 成功后置] 写入支付流水表，投递 RocketMQ 支付成功事件通知交易履约中台！\n");
        }

        /**
         * 钩子方法：支付失败后置处理
         */
        protected void postPayFailure(PaymentRequest request, PaymentResult result) {
            System.out.printf("  [Step 5 失败后置] 记录失败审计日志，释放库存预占锁定。\n");
        }
    }

    public static class RiskException extends RuntimeException {
        public RiskException(String message) {
            super(message);
        }
    }

    // ======================= 4. 具体策略子类实现 =======================

    /**
     * 策略实现 1：微信支付
     */
    public static class WeChatPaymentStrategy extends AbstractPaymentTemplate {
        @Override
        public PayChannelEnum getChannel() {
            return PayChannelEnum.WECHAT_PAY;
        }

        @Override
        protected String executeChannelDeduction(PaymentRequest request, BigDecimal actualAmount) {
            System.out.printf("    -> [微信支付专区] 调用微信统一统一下单 API (V3接口)，统一下单 PrepayId 生成中...\n");
            return "WX_PREPAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }

    /**
     * 策略实现 2：支付宝支付（享受专属立减优惠）
     */
    public static class AliPaymentStrategy extends AbstractPaymentTemplate {
        @Override
        public PayChannelEnum getChannel() {
            return PayChannelEnum.ALIPAY;
        }

        @Override
        protected BigDecimal calculateActualAmount(PaymentRequest request) {
            // 模拟支付宝渠道专享活动：满 100 立减 15 元
            BigDecimal discount = new BigDecimal("15.00");
            if (request.amount().compareTo(new BigDecimal("100.00")) >= 0) {
                System.out.println("    -> [支付宝专区优惠] 命中“支付宝立减金活动”，立减 ￥15.00！");
                return request.amount().subtract(discount).setScale(2, RoundingMode.HALF_UP);
            }
            return request.amount();
        }

        @Override
        protected String executeChannelDeduction(PaymentRequest request, BigDecimal actualAmount) {
            System.out.printf("    -> [支付宝专区] 发送 alipay.trade.create 收单请求，实收: ￥%s\n", actualAmount);
            return "ALI_TRADE_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }

    /**
     * 策略实现 3：银联快捷支付（大额卡BIN及单笔限额风控拦截）
     */
    public static class UnionPaymentStrategy extends AbstractPaymentTemplate {
        private static final BigDecimal SINGLE_LIMIT = new BigDecimal("50000.00");

        @Override
        public PayChannelEnum getChannel() {
            return PayChannelEnum.UNION_PAY;
        }

        @Override
        protected void checkRiskControl(PaymentRequest request) {
            super.checkRiskControl(request);
            // 银联专属风控规则：单笔金额不能超过 5 万元
            System.out.printf("    -> [银联专区风控] 校验快捷支付单笔限额: 当前金额 ￥%s vs 限额 ￥%s\n",
                    request.amount(), SINGLE_LIMIT);
            if (request.amount().compareTo(SINGLE_LIMIT) > 0) {
                throw new RiskException("银联单笔快捷支付超限！单笔上限: ￥" + SINGLE_LIMIT);
            }
        }

        @Override
        protected String executeChannelDeduction(PaymentRequest request, BigDecimal actualAmount) {
            System.out.printf("    -> [银联专区] 组装银联 ISO-8583 报文，向发卡行发起扣款请求...\n");
            return "UNION_PAY_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }

    // ======================= 5. 工厂模式：策略路由中心 =======================

    public static class PaymentStrategyFactory {
        private static final Map<PayChannelEnum, PaymentStrategy> STRATEGY_POOL = new ConcurrentHashMap<>();

        /**
         * 静态注册或依赖注入收集
         */
        public static void registerStrategy(PaymentStrategy strategy) {
            STRATEGY_POOL.put(strategy.getChannel(), strategy);
        }

        /**
         * 根据渠道枚举动态路由查找策略
         */
        public static PaymentStrategy getStrategy(PayChannelEnum channel) {
            PaymentStrategy strategy = STRATEGY_POOL.get(channel);
            if (strategy == null) {
                throw new IllegalArgumentException("未找到支持该支付渠道的策略实现: " + channel);
            }
            return strategy;
        }

        public static int getRegisteredCount() {
            return STRATEGY_POOL.size();
        }
    }

    // ======================= 6. 主程序场景运行演练 =======================

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("       策略模式 + 工厂模式 + 模板方法模式 电商全渠道支付结算架构演练      ");
        System.out.println("======================================================================\n");

        // 模拟 Spring 容器启动自动注册各策略 Bean
        PaymentStrategyFactory.registerStrategy(new WeChatPaymentStrategy());
        PaymentStrategyFactory.registerStrategy(new AliPaymentStrategy());
        PaymentStrategyFactory.registerStrategy(new UnionPaymentStrategy());

        System.out.printf(">>> 支付工厂路由表初始化完毕，当前支持渠道数: %d\n\n",
                PaymentStrategyFactory.getRegisteredCount());

        // 场景 1：微信支付正常结算
        System.out.println("================== 场景 1：微信支付标准扣款流程 ==================");
        PaymentRequest wxReq = new PaymentRequest(
                "ORD-20261103-001", "USER-1001",
                new BigDecimal("88.00"), PayChannelEnum.WECHAT_PAY, Map.of()
        );
        PaymentResult wxRes = PaymentStrategyFactory.getStrategy(wxReq.channel()).pay(wxReq);
        System.out.println("结算结果: " + wxRes);

        // 场景 2：支付宝结算，享受渠道满减立减金
        System.out.println("\n================== 场景 2：支付宝结算享受满减优惠 ==================");
        PaymentRequest aliReq = new PaymentRequest(
                "ORD-20261103-002", "USER-1002",
                new BigDecimal("299.00"), PayChannelEnum.ALIPAY, Map.of()
        );
        PaymentResult aliRes = PaymentStrategyFactory.getStrategy(aliReq.channel()).pay(aliReq);
        System.out.println("结算结果: " + aliRes);

        // 场景 3：银联大额支付触发风控拦截
        System.out.println("\n================== 场景 3：银联单笔超限风控拦截 ==================");
        PaymentRequest unionReq = new PaymentRequest(
                "ORD-20261103-003", "USER-1003",
                new BigDecimal("88000.00"), PayChannelEnum.UNION_PAY, Map.of()
        );
        PaymentResult unionRes = PaymentStrategyFactory.getStrategy(unionReq.channel()).pay(unionReq);
        System.out.println("结算结果: " + unionRes);

        System.out.println("\n======================================================================");
        System.out.println(">>> 支付中台三大设计模式架构验证完毕：开闭原则(OCP)、单一职责(SRP)完美落地！");
        System.out.println("======================================================================");
    }
}
