package com.knowledge.core.exception;

/**
 * 异常处理深度实战：
 * 1. 传统 try-catch-finally 中 finally 异常覆盖吞掉业务主异常的线上灾难
 * 2. try-with-resources 语法糖底层机制与 getSuppressed() 压制异常无损提取
 * 3. 证明 AutoCloseable 资源逆序关闭（声明顺序与释放顺序相反）
 */
public class ExceptionSuppressedDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 传统 finally 异常吃掉业务主异常的经典 Bug ==========");
        simulateTraditionalFinallyTrap();

        System.out.println("\n========== 2. try-with-resources 自动压制 (Suppressed) 机制证明 ==========");
        simulateTryWithResourcesSuppression();

        System.out.println("\n========== 3. AutoCloseable 资源逆序关闭顺序证明 ==========");
        verifyCloseOrder();
    }

    private static void simulateTraditionalFinallyTrap() {
        try {
            badPracticeLegacyClose();
        } catch (Exception e) {
            System.err.println("【传统写法捕获到的异常】: " + e.getMessage());
            System.out.println(">>> 观察：核心业务异常 [BusinessException: 支付扣款超时失败] 被 finally 中的关闭流异常彻底抹杀，排错无门！");
        }
    }

    private static void badPracticeLegacyClose() throws Exception {
        LegacyResource res = null;
        try {
            res = new LegacyResource();
            throw new RuntimeException("BusinessException: 支付扣款超时失败");
        } finally {
            if (res != null) {
                // finally 内部抛出异常，直接覆盖栈帧并吃掉 try 内抛出的业务异常
                res.close();
            }
        }
    }

    private static void simulateTryWithResourcesSuppression() {
        try (AutoResource res1 = new AutoResource("DBConnection", true);
             AutoResource res2 = new AutoResource("RedisPool", true)) {

            System.out.println("正在执行业务核心操作...");
            throw new IllegalStateException("PrimaryException: 核心下单事务回滚");

        } catch (Exception e) {
            System.err.println("【优雅捕获的主业务异常】: " + e.getMessage());
            Throwable[] suppressed = e.getSuppressed();
            System.out.println("【挂载被压制(Suppressed)的异常个数】: " + suppressed.length);
            for (int i = 0; i < suppressed.length; i++) {
                System.out.printf("  [压制异常 #%d]: %s%n", i + 1, suppressed[i].getMessage());
            }
            System.out.println(">>> 结论：try-with-resources 既抛出了最重要的主异常，又完整保留了资源关闭链的所有附属异常！");
        }
    }

    private static void verifyCloseOrder() {
        System.out.println("按顺序声明：Resource-A -> Resource-B -> Resource-C");
        try (AutoResource a = new AutoResource("Resource-A", false);
             AutoResource b = new AutoResource("Resource-B", false);
             AutoResource c = new AutoResource("Resource-C", false)) {
            System.out.println(">>> try 块代码执行中...");
        } catch (Exception e) {
            // ignore
        }
        System.out.println(">>> 结论：声明顺序为 A -> B -> C，关闭释放顺序严格逆序为 C -> B -> A！保障依赖关系的正确解构。");
    }

    static class LegacyResource {
        public void close() throws Exception {
            throw new RuntimeException("FinallyException: Socket 关闭时 IO 故障");
        }
    }

    static class AutoResource implements AutoCloseable {
        private final String name;
        private final boolean failOnClose;

        public AutoResource(String name, boolean failOnClose) {
            this.name = name;
            this.failOnClose = failOnClose;
            System.out.println("-> [创建资源] " + name);
        }

        @Override
        public void close() throws Exception {
            System.out.println("<- [释放关闭资源] " + name);
            if (failOnClose) {
                throw new RuntimeException("ResourceCloseFailedException: [" + name + "] 释放资源抛出异常");
            }
        }
    }
}
