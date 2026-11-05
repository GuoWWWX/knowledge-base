package com.knowledge.pattern.behavioral;

import java.util.ArrayList;
import java.util.List;

/**
 * 行为型模式实战：责任链模式 (Chain of Responsibility)
 * 1. 业务场景：电商高并发下单全链路风控拦截网关
 * 2. 核心架构：
 *    - 每一个 Filter 节点独立负责单一职责 (SRP)，各节点间松耦合
 *    - 节点根据前置判断决定是否放行或直接熔断短路 (Short-Circuit)
 *    - 具备类似 Spring Security / Servlet Filter / Netty Pipeline 的流水线设计
 */
public class ChainOfResponsibilityDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 组装风控网关责任链管道 ==========");
        RiskFilterChain chain = new RiskFilterChain();
        chain.addFilter(new UserBlacklistFilter())     // 1. 黑名单初筛
             .addFilter(new IpRateLimitFilter())       // 2. IP 限流频控
             .addFilter(new AccountStatusFilter())     // 3. 账户冻结状态检查
             .addFilter(new LargeAmountAuditFilter()); // 4. 大额交易二次审核

        System.out.println("\n========== 场景 A: 正常普通订单通过全部流水线 ==========");
        OrderContext normalOrder = new OrderContext("U1001", "192.168.1.10", 300.0, false);
        boolean passA = chain.execute(normalOrder);
        System.out.println("订单处理最终结论: " + (passA ? "【放行并创建订单】" : "【拦截】"));

        System.out.println("\n========== 场景 B: 命中黑名单用户，首个节点即刻短路熔断 ==========");
        OrderContext blacklistOrder = new OrderContext("U_BLACKLIST_99", "192.168.1.11", 500.0, false);
        boolean passB = chain.execute(blacklistOrder);
        System.out.println("订单处理最终结论: " + (passB ? "【放行】" : "【拦截】"));

        System.out.println("\n========== 场景 C: 账户被冻结，在第三节点被拦截 ==========");
        OrderContext frozenOrder = new OrderContext("U1002", "192.168.1.12", 200.0, true);
        boolean passC = chain.execute(frozenOrder);
        System.out.println("订单处理最终结论: " + (passC ? "【放行】" : "【拦截】"));
    }

    public record OrderContext(String userId, String ip, double amount, boolean isFrozen) {}

    public interface RiskFilter {
        /**
         * @return true 继续传递给下一个过滤器; false 触发熔断拦截并终止执行
         */
        boolean doFilter(OrderContext context);
    }

    public static class RiskFilterChain {
        private final List<RiskFilter> filters = new ArrayList<>();

        public RiskFilterChain addFilter(RiskFilter filter) {
            this.filters.add(filter);
            return this;
        }

        public boolean execute(OrderContext context) {
            for (RiskFilter filter : filters) {
                boolean passed = filter.doFilter(context);
                if (!passed) {
                    System.err.printf("  ✖ [%s] 判定未通过，立即触发熔断短路，终止后续风控！%n",
                            filter.getClass().getSimpleName());
                    return false;
                }
            }
            return true;
        }
    }

    // 1. 黑名单过滤
    public static class UserBlacklistFilter implements RiskFilter {
        @Override
        public boolean doFilter(OrderContext context) {
            if (context.userId().contains("BLACKLIST")) {
                System.out.println("  -> [UserBlacklistFilter] 发现黑名单用户: " + context.userId());
                return false;
            }
            System.out.println("  ✓ [UserBlacklistFilter] 黑名单校验通过");
            return true;
        }
    }

    // 2. IP 频率过滤
    public static class IpRateLimitFilter implements RiskFilter {
        @Override
        public boolean doFilter(OrderContext context) {
            // 模拟频控通过
            System.out.println("  ✓ [IpRateLimitFilter] IP 频控校验通过 (QPS < 100)");
            return true;
        }
    }

    // 3. 账户状态过滤
    public static class AccountStatusFilter implements RiskFilter {
        @Override
        public boolean doFilter(OrderContext context) {
            if (context.isFrozen()) {
                System.out.println("  -> [AccountStatusFilter] 账户处于冻结状态，禁止扣款！");
                return false;
            }
            System.out.println("  ✓ [AccountStatusFilter] 账户状态正常 (ACTIVE)");
            return true;
        }
    }

    // 4. 大额交易二次审核
    public static class LargeAmountAuditFilter implements RiskFilter {
        @Override
        public boolean doFilter(OrderContext context) {
            if (context.amount() > 10_000.0) {
                System.out.println("  -> [LargeAmountAuditFilter] 交易额达到 ￥" + context.amount() + "，需流转人工复审！");
                return false;
            }
            System.out.println("  ✓ [LargeAmountAuditFilter] 金额风控校验通过: ￥" + context.amount());
            return true;
        }
    }
}
