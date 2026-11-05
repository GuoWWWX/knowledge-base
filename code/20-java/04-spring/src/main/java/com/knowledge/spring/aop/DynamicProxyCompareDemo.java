package com.knowledge.spring.aop;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * 手写 JDK 动态代理与 Spring AOP 责任链拦截器原理深度剖析
 *
 * 核心对比维度：
 * 1. 原生直接调用：性能极致，但横切逻辑（日志、监控、事务）与业务代码深度侵入耦合；
 * 2. 基础反射调用：解耦了具体类与方法名，但依然缺乏切面编排与无感织入能力；
 * 3. JDK 动态代理 + AOP 拦截器链：
 *    - 运行时动态生成代理字节码 $Proxy0；
 *    - 基于 InvocationHandler + MethodInvocation 责任链递归推进；
 *    - 业务代码零侵入，优雅实现前置、环绕、后置、异常等切面能力。
 */
public class DynamicProxyCompareDemo {

    // ======================= 业务接口与实现 =======================

    public interface IUserService {
        String createUser(String username, String role);
        String queryUser(String username);
    }

    public static class UserServiceImpl implements IUserService {
        @Override
        public String createUser(String username, String role) {
            System.out.printf("    [核心业务] 创建用户成功: name = %s, role = %s\n", username, role);
            return "UID-" + System.currentTimeMillis();
        }

        @Override
        public String queryUser(String username) {
            System.out.printf("    [核心业务] 查询用户数据库: name = %s\n", username);
            return "用户信息: [" + username + ", 状态: 激活]";
        }
    }

    // ======================= 模拟 Spring AOP 拦截器链机制 =======================

    /**
     * 环绕通知拦截器接口 (类似 org.aopalliance.intercept.MethodInterceptor)
     */
    @FunctionalInterface
    public interface MethodInterceptor {
        Object invoke(MethodInvocation invocation) throws Throwable;
    }

    /**
     * 方法调用上下文与责任链驱动器
     */
    public static class MethodInvocation {
        private final Object target;
        private final Method method;
        private final Object[] args;
        private final List<MethodInterceptor> interceptors;
        private int currentInterceptorIndex = -1;

        public MethodInvocation(Object target, Method method, Object[] args, List<MethodInterceptor> interceptors) {
            this.target = target;
            this.method = method;
            this.args = args;
            this.interceptors = interceptors;
        }

        public Object getTarget() { return target; }
        public Method getMethod() { return method; }
        public Object[] getArgs() { return args; }

        /**
         * 责任链递归推进：依次执行拦截器，链尾执行目标反射调用
         */
        public Object proceed() throws Throwable {
            if (this.currentInterceptorIndex == this.interceptors.size() - 1) {
                // 已经到达拦截器链末尾，反射调用目标真实方法
                return method.invoke(target, args);
            }
            // 推进到下一个拦截器
            MethodInterceptor nextInterceptor = this.interceptors.get(++this.currentInterceptorIndex);
            return nextInterceptor.invoke(this);
        }
    }

    /**
     * 切面 1：日志打印拦截器
     */
    public static class LoggingInterceptor implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            System.out.printf("  [切面-日志] >> 进入方法: %s, 入参: %s\n",
                    invocation.getMethod().getName(), java.util.Arrays.toString(invocation.getArgs()));
            Object result = invocation.proceed();
            System.out.printf("  [切面-日志] << 离开方法: %s, 返回值: %s\n",
                    invocation.getMethod().getName(), result);
            return result;
        }
    }

    /**
     * 切面 2：耗时监控统计拦截器
     */
    public static class PerformanceMetricsInterceptor implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            long start = System.nanoTime();
            try {
                return invocation.proceed();
            } finally {
                long duration = System.nanoTime() - start;
                System.out.printf("  [切面-监控] ⏱ 方法 [%s] 耗时统计: %d ns\n",
                        invocation.getMethod().getName(), duration);
            }
        }
    }

    /**
     * 切面 3：安全与权限校验拦截器
     */
    public static class SecurityAuthInterceptor implements MethodInterceptor {
        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            System.out.printf("  [切面-鉴权] 🔒 开始鉴权校验: 方法 = %s\n", invocation.getMethod().getName());
            Object[] args = invocation.getArgs();
            if (args != null && args.length >= 2 && "admin".equalsIgnoreCase(String.valueOf(args[1]))) {
                System.out.println("  [切面-鉴权] ⚠ 检测到 admin 高危角色创建，记录风控审计！");
            }
            return invocation.proceed();
        }
    }

    /**
     * AOP 代理工厂
     */
    @SuppressWarnings("unchecked")
    public static <T> T createProxy(T target, List<MethodInterceptor> interceptors, Class<T> interfaceClass) {
        InvocationHandler handler = (proxy, method, args) -> {
            // 将每次代理方法调用封装为 MethodInvocation 责任链推进
            MethodInvocation invocation = new MethodInvocation(target, method, args, interceptors);
            return invocation.proceed();
        };

        return (T) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                new Class<?>[]{interfaceClass},
                handler
        );
    }

    // ======================= 主运行入口验证 =======================

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================");
        System.out.println("        原生调用 vs 反射调用 vs JDK 动态代理 AOP 切面增强深度对比     ");
        System.out.println("===============================================================\n");

        IUserService targetService = new UserServiceImpl();

        // 1. 原生直接调用
        System.out.println("--- 模式 1：原生直接调用（无切面支持）---");
        targetService.createUser("张三", "guest");

        System.out.println("\n---------------------------------------------------------------");
        // 2. 原生反射调用
        System.out.println("--- 模式 2：JDK 原生反射调用（硬编码调用）---");
        Method queryMethod = IUserService.class.getMethod("queryUser", String.class);
        Object reflectResult = queryMethod.invoke(targetService, "张三");
        System.out.printf("  反射直接调用返回值: %s\n", reflectResult);

        System.out.println("\n---------------------------------------------------------------");
        // 3. JDK 动态代理 + AOP 责任链切面增强
        System.out.println("--- 模式 3：JDK 动态代理 + AOP 拦截器链（无侵入织入）---");

        List<MethodInterceptor> interceptorChain = new ArrayList<>();
        interceptorChain.add(new PerformanceMetricsInterceptor()); // 外层：统计全链路耗时
        interceptorChain.add(new LoggingInterceptor());            // 中层：打印出入参日志
        interceptorChain.add(new SecurityAuthInterceptor());       // 内层：权限与风控审计

        IUserService proxyService = createProxy(targetService, interceptorChain, IUserService.class);

        System.out.printf("代理对象类型: %s (JDK 动态代理类: %b)\n\n",
                proxyService.getClass().getName(), Proxy.isProxyClass(proxyService.getClass()));

        System.out.println(">>> 触发执行 proxyService.createUser(\"李四-管理员\", \"admin\"):");
        String uid = proxyService.createUser("李四-管理员", "admin");
        System.out.printf(">>> 调用完成，生成 UID: %s\n\n", uid);

        System.out.println(">>> 触发执行 proxyService.queryUser(\"王五\"):");
        proxyService.queryUser("王五");

        System.out.println("\n>>> JDK 动态代理与 AOP 责任链拦截机制演练成功完成！");
    }
}
