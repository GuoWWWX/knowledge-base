package com.knowledge.core.proxy;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 代理与高性能调用体系深度对比：
 * 1. 手写 JDK 动态代理原理与 InvocationHandler 拦截切面
 * 2. 现代高性能方法句柄 MethodHandles.lookup() 与直接编译字节码调用
 * 3. 1,000,000 次高频调用下：直接调用 vs MethodHandle vs 传统反射 Method.invoke 性能天梯榜
 */
public class DynamicProxyDeepDiveDemo {

    public interface OrderService {
        String createOrder(String userId, double amount);
    }

    public static class OrderServiceImpl implements OrderService {
        @Override
        public String createOrder(String userId, double amount) {
            return "ORDER_" + userId + "_" + (long) amount;
        }
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("========== 1. JDK 动态代理运行期代理与拦截切面实战 ==========");
        OrderService target = new OrderServiceImpl();
        OrderService proxy = (OrderService) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                new MetricsInvocationHandler(target)
        );

        String result = proxy.createOrder("U1001", 999.0);
        System.out.println("代理执行结果: " + result);
        System.out.println("生成的代理类 Class: " + proxy.getClass().getName());

        System.out.println("\n========== 2. 1,000,000 次方法调用性能压测基准 ==========");
        int count = 1_000_000;
        OrderServiceImpl service = new OrderServiceImpl();

        // 预热 JVM
        for (int i = 0; i < 20_000; i++) {
            service.createOrder("test", 1.0);
        }

        // 基准 1: 原生直接调用
        long t1 = System.nanoTime();
        for (int i = 0; i < count; i++) {
            service.createOrder("U1", 10.0);
        }
        long t2 = System.nanoTime();

        // 基准 2: MethodHandles.lookup() 方法句柄调用
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(String.class, String.class, double.class);
        MethodHandle mh = lookup.findVirtual(OrderServiceImpl.class, "createOrder", mt);
        long t3 = System.nanoTime();
        for (int i = 0; i < count; i++) {
            String s = (String) mh.invokeExact(service, "U1", 10.0);
        }
        long t4 = System.nanoTime();

        // 基准 3: 传统反射 Method.invoke()
        Method method = OrderServiceImpl.class.getMethod("createOrder", String.class, double.class);
        method.setAccessible(true);
        long t5 = System.nanoTime();
        for (int i = 0; i < count; i++) {
            method.invoke(service, "U1", 10.0);
        }
        long t6 = System.nanoTime();

        System.out.printf("1. 原生直接调用耗时       : %8.2f ms (基准)%n", (t2 - t1) / 1_000_000.0);
        System.out.printf("2. MethodHandle invokeExact: %8.2f ms (JVM 字节码直接链接，支持 JIT 内联)%n", (t4 - t3) / 1_000_000.0);
        System.out.printf("3. 传统反射 Method.invoke  : %8.2f ms (参数包装 Object[] 数组与权限安全检查开销)%n", (t6 - t5) / 1_000_000.0);
        System.out.println(">>> 总结: JDK 7+ 引入的 MethodHandle 是 JVM 底层支撑 invokedynamic 指令的基石，性能远超传统反射！");
    }

    static class MetricsInvocationHandler implements InvocationHandler {
        private final Object target;

        public MetricsInvocationHandler(Object target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            long start = System.nanoTime();
            try {
                System.out.println(">>> [AOP 前置通知] 准备执行目标方法: " + method.getName());
                return method.invoke(target, args);
            } finally {
                long duration = System.nanoTime() - start;
                System.out.printf("<<< [AOP 后置通知] 方法 %s 执行结束，耗时: %.3f ms%n",
                        method.getName(), duration / 1_000_000.0);
            }
        }
    }
}
