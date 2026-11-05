package com.knowledge.pattern.behavioral;

/**
 * 行为型模式实战：状态模式 (State Pattern) 工业级电商订单状态机
 * 1. 业务痛点：
 *    - 订单生命周期长，随着状态增加，传统代码充斥着数十个嵌套 if-else 或庞大 switch-case，极其脆弱易出 Bug
 * 2. 状态模式优势：
 *    - 将每个业务状态封装为独立的状态对象，实现单一职责与开闭原则
 *    - 状态机严格管控合法状态跃迁流转，非法跃迁直接抛出 InvalidStateTransitionException
 * 3. 状态流转矩阵：
 *    - WAIT_PAY (待支付) -> 支付 -> PAID (已支付)
 *    - WAIT_PAY (待支付) -> 取消 -> CANCELLED (已取消)
 *    - PAID (已支付)     -> 发货 -> SHIPPED (已发货)
 *    - SHIPPED (已发货)  -> 确认收货 -> COMPLETED (已完成)
 *    - COMPLETED (已完成) -> 尝试取消 -> 拦截报错！
 */
public class OrderStateMachineDemo {

    public static void main(String[] args) {
        System.out.println("========== 场景 1: 正常订单履约全流程生命周期 ==========");
        OrderOrderContext order1 = new OrderOrderContext("ORDER_1001");
        order1.pay();      // 待支付 -> 已支付
        order1.ship();     // 已支付 -> 已发货
        order1.confirm();  // 已发货 -> 已完成

        System.out.println("\n========== 场景 2: 待支付超时主动取消 ==========");
        OrderOrderContext order2 = new OrderOrderContext("ORDER_1002");
        order2.cancel();   // 待支付 -> 已取消

        System.out.println("\n========== 场景 3: 状态机非法流转防御拦截 ==========");
        OrderOrderContext order3 = new OrderOrderContext("ORDER_1003");
        order3.pay();
        order3.ship();
        order3.confirm(); // 此时已是 COMPLETED 完成态

        try {
            System.out.println("用户尝试对已完成的订单发起取消请求...");
            order3.cancel(); // 预期触发状态机非法跃迁拦截
        } catch (IllegalStateException e) {
            System.err.println("【状态机安全拦截成功】: " + e.getMessage());
        }
    }

    // 状态接口定义
    public interface OrderState {
        void pay(OrderOrderContext ctx);
        void cancel(OrderOrderContext ctx);
        void ship(OrderOrderContext ctx);
        void confirm(OrderOrderContext ctx);
        String getStateName();
    }

    // 状态机上下文
    public static class OrderOrderContext {
        private final String orderId;
        private OrderState currentState;

        public OrderOrderContext(String orderId) {
            this.orderId = orderId;
            // 初始状态: 待支付
            this.currentState = new WaitPayState();
            System.out.printf(">>> [订单创建] %s 初始状态: %s%n", orderId, currentState.getStateName());
        }

        public void setState(OrderState state) {
            System.out.printf("  [状态流转] %s: %s -> %s%n", orderId, this.currentState.getStateName(), state.getStateName());
            this.currentState = state;
        }

        public void pay() { currentState.pay(this); }
        public void cancel() { currentState.cancel(this); }
        public void ship() { currentState.ship(this); }
        public void confirm() { currentState.confirm(this); }
    }

    // 1. 待支付状态
    public static class WaitPayState implements OrderState {
        @Override
        public void pay(OrderOrderContext ctx) {
            System.out.println("  ✓ 支付校验通过，扣减资金成功！");
            ctx.setState(new PaidState());
        }

        @Override
        public void cancel(OrderOrderContext ctx) {
            System.out.println("  ✓ 订单超时未支付，自动释放预占库存！");
            ctx.setState(new CancelledState());
        }

        @Override
        public void ship(OrderOrderContext ctx) {
            throw new IllegalStateException("待支付订单禁止发货！");
        }

        @Override
        public void confirm(OrderOrderContext ctx) {
            throw new IllegalStateException("待支付订单禁止确认收货！");
        }

        @Override
        public String getStateName() { return "WAIT_PAY (待支付)"; }
    }

    // 2. 已支付状态
    public static class PaidState implements OrderState {
        @Override
        public void pay(OrderOrderContext ctx) {
            throw new IllegalStateException("订单已支付，严禁重复扣款！");
        }

        @Override
        public void cancel(OrderOrderContext ctx) {
            System.out.println("  ✓ 用户申请退款，执行退款原路退回！");
            ctx.setState(new CancelledState());
        }

        @Override
        public void ship(OrderOrderContext ctx) {
            System.out.println("  ✓ 仓库打包完毕，交接顺丰快递发出！");
            ctx.setState(new ShippedState());
        }

        @Override
        public void confirm(OrderOrderContext ctx) {
            throw new IllegalStateException("未发货订单无法确认收货！");
        }

        @Override
        public String getStateName() { return "PAID (已支付)"; }
    }

    // 3. 已发货状态
    public static class ShippedState implements OrderState {
        @Override
        public void pay(OrderOrderContext ctx) {
            throw new IllegalStateException("已发货订单无需支付！");
        }

        @Override
        public void cancel(OrderOrderContext ctx) {
            throw new IllegalStateException("商品已在物流运输途中，无法直接取消，请申请售后退货！");
        }

        @Override
        public void ship(OrderOrderContext ctx) {
            throw new IllegalStateException("请勿重复发货！");
        }

        @Override
        public void confirm(OrderOrderContext ctx) {
            System.out.println("  ✓ 买家签收快递，款项结算给商家，累加用户会员积分！");
            ctx.setState(new CompletedState());
        }

        @Override
        public String getStateName() { return "SHIPPED (已发货)"; }
    }

    // 4. 已完成状态 (终态)
    public static class CompletedState implements OrderState {
        @Override
        public void pay(OrderOrderContext ctx) { throw new IllegalStateException("已完成订单不可支付！"); }
        @Override
        public void cancel(OrderOrderContext ctx) { throw new IllegalStateException("交易已圆满完成，严禁单方面取消！"); }
        @Override
        public void ship(OrderOrderContext ctx) { throw new IllegalStateException("已完成订单不可重复发货！"); }
        @Override
        public void confirm(OrderOrderContext ctx) { throw new IllegalStateException("已确认收货，不可重复确认！"); }
        @Override
        public String getStateName() { return "COMPLETED (已完成)"; }
    }

    // 5. 已取消状态 (终态)
    public static class CancelledState implements OrderState {
        @Override
        public void pay(OrderOrderContext ctx) { throw new IllegalStateException("已取消订单不可支付！"); }
        @Override
        public void cancel(OrderOrderContext ctx) { throw new IllegalStateException("已取消订单不可重复取消！"); }
        @Override
        public void ship(OrderOrderContext ctx) { throw new IllegalStateException("已取消订单不可发货！"); }
        @Override
        public void confirm(OrderOrderContext ctx) { throw new IllegalStateException("已取消订单不可确认！"); }
        @Override
        public String getStateName() { return "CANCELLED (已取消)"; }
    }
}
