package com.knowledge.spring.tx;

import java.lang.annotation.*;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.UUID;

/**
 * Spring 声明式事务四大典型失效陷阱仿真与防御实战
 *
 * 核心原理剖析：
 * Spring 声明式事务 (@Transactional) 本质是基于 Spring AOP 动态代理实现的。
 * 只有调用进入代理对象的拦截链 TransactionInterceptor，事务的开启、提交与回滚才会生效。
 *
 * 4 大致命失效场景：
 * 1. 同类内部方法自调用（this.method()）：绕过代理对象直接执行裸实例方法，切面不触发；
 * 2. 非 public 方法修饰：Spring 事务规则扫描默认仅对 public 方法生效，其它修饰符直接被忽略；
 * 3. 开发者在方法内部 try-catch 吞掉异常：切面无法捕获到抛出的异常，导致错误数据被作为“正常”提交；
 * 4. 异常类型不匹配：默认仅回滚 RuntimeException 与 Error，抛出 Checked Exception (如 Exception, SQLException) 时默认仍然提交！
 */
public class TransactionTrapSimDemo {

    // ======================= 模拟 Spring 事务注解与状态机 =======================

    @Target({ElementType.METHOD, ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SimTransactional {
        Class<? extends Throwable>[] rollbackFor() default {RuntimeException.class, Error.class};
    }

    public static class SimTransactionStatus {
        private final String txId = UUID.randomUUID().toString().substring(0, 8);
        private boolean rollbackOnly = false;
        private boolean completed = false;

        public String getTxId() { return txId; }
        public void setRollbackOnly() { this.rollbackOnly = true; }
        public boolean isRollbackOnly() { return rollbackOnly; }
        public boolean isCompleted() { return completed; }
        public void setCompleted() { this.completed = true; }
    }

    public static class SimTransactionManager {
        private static final ThreadLocal<SimTransactionStatus> currentTx = new ThreadLocal<>();

        public static SimTransactionStatus getStatus() {
            return currentTx.get();
        }

        public static SimTransactionStatus beginTransaction() {
            SimTransactionStatus status = new SimTransactionStatus();
            currentTx.set(status);
            System.out.printf("  [事务管理器] 🟢 BEGIN TRANSACTION [TX-%s] (开启事务连接)\n", status.getTxId());
            return status;
        }

        public static void commit(SimTransactionStatus status) {
            if (status.isRollbackOnly()) {
                System.out.printf("  [事务管理器] 🔴 ROLLBACK [TX-%s] (被标记为 rollbackOnly，强制回滚!)\n", status.getTxId());
            } else {
                System.out.printf("  [事务管理器] 🔵 COMMIT [TX-%s] (事务正常提交数据入库) ✅\n", status.getTxId());
            }
            status.setCompleted();
            currentTx.remove();
        }

        public static void rollback(SimTransactionStatus status, Throwable ex) {
            System.out.printf("  [事务管理器] 🔴 ROLLBACK [TX-%s] 捕获异常触发回滚: [%s] ❌\n",
                    status.getTxId(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
            status.setCompleted();
            currentTx.remove();
        }
    }

    // ======================= 模拟 Spring 事务 AOP 代理生成器 =======================

    @SuppressWarnings("unchecked")
    public static <T> T createTxProxy(T target, Class<T> interfaceClass) {
        InvocationHandler handler = (proxy, method, args) -> {
            // 获取目标类的具体方法声明
            Method targetMethod = target.getClass().getMethod(method.getName(), method.getParameterTypes());
            SimTransactional txAnno = targetMethod.getAnnotation(SimTransactional.class);

            // 陷阱 2 校验：Spring 事务默认只对 public 方法生效
            boolean isPublic = Modifier.isPublic(targetMethod.getModifiers());
            if (txAnno != null && !isPublic) {
                System.out.println("  [切面警告] ⚠ 方法非 public 修饰，Spring 事务拦截器直接跳过，不开启事务！");
                return targetMethod.invoke(target, args);
            }

            if (txAnno == null) {
                // 无事务注解直接执行
                return targetMethod.invoke(target, args);
            }

            // 开启事务
            SimTransactionStatus status = SimTransactionManager.beginTransaction();
            try {
                Object result = targetMethod.invoke(target, args);
                // 提交事务
                SimTransactionManager.commit(status);
                return result;
            } catch (Throwable e) {
                Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                // 陷阱 4 校验：判断是否匹配 rollbackFor 异常类型
                boolean shouldRollback = false;
                for (Class<? extends Throwable> rollbackType : txAnno.rollbackFor()) {
                    if (rollbackType.isAssignableFrom(cause.getClass())) {
                        shouldRollback = true;
                        break;
                    }
                }

                if (shouldRollback) {
                    SimTransactionManager.rollback(status, cause);
                } else {
                    System.err.printf("  [致命陷阱] 抛出受检异常 [%s]，不属于默认回滚范围 %s，事务被迫正常提交！\n",
                            cause.getClass().getSimpleName(), Arrays.toString(txAnno.rollbackFor()));
                    SimTransactionManager.commit(status);
                }
                throw cause;
            }
        };

        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                new Class<?>[]{interfaceClass},
                handler
        );
    }

    // ======================= 业务服务接口与实现 =======================

    public interface IAccountService {
        void transferWithSelfCall(String from, String to, double amount);
        void transferInternal(String from, String to, double amount);
        void transferWithTryCatch(String from, String to, double amount);
        void transferWithCheckedException(String from, String to, double amount) throws Exception;
        void transferWithRollbackFor(String from, String to, double amount) throws Exception;
        void setSelfProxy(IAccountService proxy);
    }

    public static class AccountServiceImpl implements IAccountService {

        private IAccountService selfProxy;

        @Override
        public void setSelfProxy(IAccountService proxy) {
            this.selfProxy = proxy;
        }

        /**
         * 陷阱 1：同类内部自调用
         * outerMethod 未加事务，内部通过 this.transferInternal() 调用带事务的方法
         */
        @Override
        public void transferWithSelfCall(String from, String to, double amount) {
            System.out.printf("  [业务] 进入 transferWithSelfCall (无事务注解)，准备通过 this 调用内部事务方法...\n");
            // 【失效触发】：this 是原始 target 实例，并非代理对象，直接跳过 AOP 切面！
            this.transferInternal(from, to, amount);
        }

        @Override
        @SimTransactional
        public void transferInternal(String from, String to, double amount) {
            System.out.printf("  [业务-核心转账] 执行扣款: from = %s, to = %s, amount = %.2f\n", from, to, amount);
            throw new RuntimeException("转账下游结算系统网络中断超时！");
        }

        /**
         * 陷阱 3：手动 try-catch 吞异常未向上抛出
         */
        @Override
        @SimTransactional
        public void transferWithTryCatch(String from, String to, double amount) {
            System.out.printf("  [业务-转账] 开始执行业务，内部包裹 try-catch...\n");
            try {
                System.out.printf("  [业务-扣款] %s 扣款成功 %.2f\n", from, amount);
                throw new RuntimeException("入库余额扣减异常");
            } catch (Exception e) {
                // 错误做法：仅打印日志而未 rethrow，且未设置 setRollbackOnly()
                System.err.printf("  [错误防范] catch 捕获到了异常: [%s]，但未重新抛出！\n", e.getMessage());
            }
        }

        /**
         * 陷阱 4：抛出受检异常 (Checked Exception)，默认不回滚
         */
        @Override
        @SimTransactional // 默认仅回滚 RuntimeException 和 Error
        public void transferWithCheckedException(String from, String to, double amount) throws Exception {
            System.out.printf("  [业务-转账] 模拟抛出 Checked Exception: SQLException...\n");
            throw new SQLException("SQL 连接已被数据库服务端重置关闭");
        }

        /**
         * 陷阱 4 正确防御解法：显式声明 rollbackFor = Exception.class
         */
        @Override
        @SimTransactional(rollbackFor = {Exception.class})
        public void transferWithRollbackFor(String from, String to, double amount) throws Exception {
            System.out.printf("  [业务-转账] 配置了 @SimTransactional(rollbackFor = Exception.class)...\n");
            throw new SQLException("SQL 连接超时，但配置了回滚策略！");
        }
    }

    // ======================= 主运行入口验证 =======================

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("          Spring 声明式事务四大典型失效陷阱深度复现与防御验证          ");
        System.out.println("===============================================================\n");

        AccountServiceImpl rawTarget = new AccountServiceImpl();
        IAccountService proxyService = createTxProxy(rawTarget, IAccountService.class);
        rawTarget.setSelfProxy(proxyService);

        // 陷阱 1：同类内部自调用
        System.out.println("---------------------------------------------------------------");
        System.out.println("【陷阱 1：同类内部自调用绕过代理】");
        try {
            proxyService.transferWithSelfCall("Alice", "Bob", 1000.0);
        } catch (Exception e) {
            System.out.printf("  >>> 外部捕获异常: %s (但是注意：上方未曾打印 BEGIN/ROLLBACK，事务完全未介入！)\n", e.getMessage());
        }

        // 陷阱 2：非 public 修饰（说明原理）
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【陷阱 2：非 public 方法修饰】");
        System.out.println("  Spring 官方规约：AbstractFallbackTransactionAttributeSource.computeTransactionAttribute");
        System.out.println("  默认在获取方法注解属性时会检查 Modifier.isPublic(method.getModifiers())，");
        System.out.println("  若不是 public 则直接 return null，导致事务切面失效！请务必确保方法为 public。");

        // 陷阱 3：手动 try-catch 吞异常
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【陷阱 3：方法内部手动 try-catch 吞掉异常】");
        proxyService.transferWithTryCatch("Alice", "Bob", 500.0);
        System.out.println("  >>> 观察上方：业务代码内部出错，但因 try-catch 吞异常，事务被错误地 COMMIT 了！");

        // 陷阱 4：默认异常类型不匹配
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【陷阱 4：抛出 Checked Exception 导致事务默认未回滚】");
        try {
            proxyService.transferWithCheckedException("Alice", "Bob", 200.0);
        } catch (Exception e) {
            System.out.printf("  >>> 外部捕获: %s，然而事务却被 COMMIT 提交入库！\n", e.getMessage());
        }

        // 陷阱 4 防御验证：配置 rollbackFor = Exception.class
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【正确防御 4：显式配置 rollbackFor = Exception.class】");
        try {
            proxyService.transferWithRollbackFor("Alice", "Bob", 200.0);
        } catch (Exception e) {
            System.out.printf("  >>> 外部捕获: %s，事务被成功 ROLLBACK 回滚！\n", e.getMessage());
        }

        System.out.println("\n===============================================================");
        System.out.println("                      Spring 事务避坑核心军规                   ");
        System.out.println("===============================================================");
        System.out.println("1. 内部调用走代理：使用 AopContext.currentProxy() 或注入自身代理；");
        System.out.println("2. 方法必须 public：确保事务方法具备公共访问级别；");
        System.out.println("3. 严禁盲目吞异常：业务 catch 必须 rethrow 或 TransactionAspectSupport 手动标记回滚；");
        System.out.println("4. 统一指定回滚范围：生产标配 @Transactional(rollbackFor = Exception.class)。");
        System.out.println("===============================================================\n");
    }
}
