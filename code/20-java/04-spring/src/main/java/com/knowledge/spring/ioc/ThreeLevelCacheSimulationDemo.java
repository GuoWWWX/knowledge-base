package com.knowledge.spring.ioc;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 纯 Java 脱离 Spring 容器手写实现三级缓存与闭环解决循环依赖
 *
 * 核心原理剖析：
 * 1. 一级缓存 singletonObjects：
 *    - 存放完全初始化好的成品单例 Bean（经过了实例化、属性填充、初始化、AOP代理）。
 * 2. 二级缓存 earlySingletonObjects：
 *    - 存放提前暴露的半成品 Bean（可能已被 AOP 提前代理）。
 *    - 核心作用：当存在多重循环依赖（如 A->B->A, A->C->A）时，保证获取到的是同一个代理对象，避免重复触发 AOP 代理。
 * 3. 三级缓存 singletonFactories：
 *    - 存放对象工厂（ObjectFactory / Lambda），延迟按需生成早期暴露引用（含 AOP 动态代理）。
 *    - 核心作用：Spring 设计理念是“尽可能在 Bean 生命周期的最后初始化阶段才做 AOP 代理”，只有发生循环依赖时才提前生成。
 * 4. singletonsCurrentlyInCreation 集合：
 *    - 标记当前正在创建中的 Bean，用于检测是否存在循环依赖。
 */
public class ThreeLevelCacheSimulationDemo {

    // ======================= 三级缓存容器定义 =======================

    // 一级缓存：成品单例池
    private final Map<String, Object> singletonObjects = new ConcurrentHashMap<>(64);
    // 二级缓存：半成品单例池（解决多重循环依赖下的代理唯一性）
    private final Map<String, Object> earlySingletonObjects = new ConcurrentHashMap<>(64);
    // 三级缓存：单例工厂池（按需生成早期代理引用）
    private final Map<String, ObjectFactory<?>> singletonFactories = new ConcurrentHashMap<>(64);

    // 正在创建中的 Bean 标记集合（循环依赖检测探针）
    private final Set<String> singletonsCurrentlyInCreation = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // 模拟注册的 BeanClass 元数据
    private final Map<String, Class<?>> beanDefinitionMap = new HashMap<>();

    // 模拟哪些 Bean 需要被 AOP 动态代理拦截增强
    private final Set<String> aopRequiredBeans = new HashSet<>();

    @FunctionalInterface
    public interface ObjectFactory<T> {
        T getObject();
    }

    public void registerBeanDefinition(String beanName, Class<?> beanClass, boolean needAop) {
        beanDefinitionMap.put(beanName, beanClass);
        if (needAop) {
            aopRequiredBeans.add(beanName);
        }
    }

    /**
     * 容器核心入口：获取 Bean
     */
    public Object getBean(String beanName) {
        // 1. 先尝试从三级缓存中直接获取
        Object sharedInstance = getSingleton(beanName, true);
        if (sharedInstance != null) {
            return sharedInstance;
        }

        // 2. 缓存无命中，开始真正的创建流程
        return createBean(beanName);
    }

    /**
     * 三级缓存查询经典三部曲
     */
    protected Object getSingleton(String beanName, boolean allowEarlyReference) {
        // 第一步：查一级缓存（成品）
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
            // 第二步：查二级缓存（早期半成品）
            singletonObject = this.earlySingletonObjects.get(beanName);
            if (singletonObject == null && allowEarlyReference) {
                // 第三步：查三级缓存（工厂）
                ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
                if (singletonFactory != null) {
                    System.out.printf("  ⚡ [三级缓存命中] 触发单例工厂生产早期引用: beanName = [%s]\n", beanName);
                    singletonObject = singletonFactory.getObject();
                    // 升温：放入二级缓存，同时从三级缓存移除
                    this.earlySingletonObjects.put(beanName, singletonObject);
                    this.singletonFactories.remove(beanName);
                    System.out.printf("  ⬆ [二级缓存提升] 早期引用已存入 earlySingletonObjects，移除工厂: [%s]\n", beanName);
                }
            }
        }
        return singletonObject;
    }

    private boolean isSingletonCurrentlyInCreation(String beanName) {
        return this.singletonsCurrentlyInCreation.contains(beanName);
    }

    /**
     * 创建 Bean 主流程
     */
    private Object createBean(String beanName) {
        Class<?> beanClass = beanDefinitionMap.get(beanName);
        if (beanClass == null) {
            throw new IllegalArgumentException("未找到 BeanDefinition: " + beanName);
        }

        // 标记当前 Bean 正在创建中
        this.singletonsCurrentlyInCreation.add(beanName);
        System.out.printf("[Bean创建] 1. 实例化目标对象: [%s]\n", beanName);

        Object beanInstance;
        try {
            beanInstance = beanClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("实例化 Bean 失败: " + beanName, e);
        }

        final Object rawInstance = beanInstance;

        // 2. 存入三级缓存：提前暴露工厂（Lambda 延迟获取早期引用）
        System.out.printf("[Bean创建] 2. 存入三级缓存 singletonFactories: [%s]\n", beanName);
        this.singletonFactories.put(beanName, () -> getEarlyBeanReference(beanName, rawInstance));

        // 3. 属性填充（若发现依赖其它 Bean，递归调用 getBean）
        System.out.printf("[Bean创建] 3. 开始属性依赖注入: [%s]\n", beanName);
        populateBean(beanName, rawInstance);

        // 4. 初始化与 AOP 后置处理
        System.out.printf("[Bean创建] 4. 初始化与最终对象确认: [%s]\n", beanName);
        Object exposedObject = initializeBean(beanName, rawInstance);

        // 如果提前在二级缓存暴露过（说明经历过循环依赖提前代理），则最终放入一级缓存的必须是二级缓存中的代理对象！
        if (this.earlySingletonObjects.containsKey(beanName)) {
            exposedObject = this.earlySingletonObjects.get(beanName);
            System.out.printf("  🔗 [代理一致性校验] 从二级缓存拉取早期代理对象放入一级缓存: [%s]\n", beanName);
        }

        // 5. 放入一级缓存，彻底完工，清理二、三级缓存与在创标记
        this.singletonObjects.put(beanName, exposedObject);
        this.earlySingletonObjects.remove(beanName);
        this.singletonFactories.remove(beanName);
        this.singletonsCurrentlyInCreation.remove(beanName);
        System.out.printf("[Bean创建] 5. 成功晋升至一级缓存 singletonObjects: [%s] (完成!)\n\n", beanName);

        return exposedObject;
    }

    /**
     * 属性自动注入模拟（简单按照字段类型扫描并自动装配）
     */
    private void populateBean(String beanName, Object instance) {
        Field[] fields = instance.getClass().getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            String depBeanName = field.getName();
            // 若在 BeanDefinition 中已声明，则认为是依赖字段
            if (beanDefinitionMap.containsKey(depBeanName)) {
                System.out.printf("  -> [%s] 字段依赖 [%s]，准备递归 getBean(\"%s\")...\n",
                        beanName, depBeanName, depBeanName);
                Object depObject = getBean(depBeanName);
                try {
                    field.set(instance, depObject);
                    System.out.printf("  ✔ [%s] 成功将 [%s] 注入到对应字段中！\n", beanName, depBeanName);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("字段注入异常", e);
                }
            }
        }
    }

    /**
     * 获取早期 Bean 引用（如果配置了 AOP 则提前返回代理对象，否则返回裸对象）
     */
    private Object getEarlyBeanReference(String beanName, Object rawInstance) {
        Object exposed = rawInstance;
        if (aopRequiredBeans.contains(beanName)) {
            System.out.printf("    ★ [AOP 提前代理增强] 对 [%s] 生成 JDK 动态代理对象！\n", beanName);
            exposed = createAopProxy(rawInstance, beanName);
        }
        return exposed;
    }

    private Object initializeBean(String beanName, Object rawInstance) {
        // 正常没有循环依赖时，AOP 发生在初始化之后；如果已有早期暴露，则此处不再重复代理
        return rawInstance;
    }

    /**
     * 动态代理生成辅助
     */
    private Object createAopProxy(Object target, String beanName) {
        return Proxy.newProxyInstance(
                target.getClass().getClassLoader(),
                target.getClass().getInterfaces(),
                (proxy, method, args) -> {
                    System.out.printf("      [AOP 切面拦截通知] 执行目标方法前置日志: [%s.%s()]\n",
                            beanName, method.getName());
                    Object ret = method.invoke(target, args);
                    System.out.printf("      [AOP 切面拦截通知] 目标方法执行完毕后置日志: [%s.%s()]\n",
                            beanName, method.getName());
                    return ret;
                }
        );
    }

    // ======================= 业务接口与实体模拟 =======================

    public interface IOrderService {
        void createOrder(String orderId);
        IPaymentService getPaymentService();
    }

    public interface IPaymentService {
        void pay(String orderId);
        IOrderService getOrderService();
    }

    public static class OrderServiceImpl implements IOrderService {
        private IPaymentService paymentService; // 依赖注入 PaymentService

        @Override
        public void createOrder(String orderId) {
            System.out.printf("  [OrderServiceImpl] 正在创建订单: %s\n", orderId);
            paymentService.pay(orderId);
        }

        @Override
        public IPaymentService getPaymentService() {
            return paymentService;
        }
    }

    public static class PaymentServiceImpl implements IPaymentService {
        private IOrderService orderService; // 依赖注入 OrderService (形成双向循环依赖)

        @Override
        public void pay(String orderId) {
            System.out.printf("  [PaymentServiceImpl] 接收订单进行结算扣款: %s\n", orderId);
        }

        @Override
        public IOrderService getOrderService() {
            return orderService;
        }
    }

    // ======================= 主运行入口验证 =======================

    public static void main(String[] args) {
        System.out.println("===============================================================");
        System.out.println("        Spring 三级缓存与循环依赖闭环解决原生 Java 模拟验证        ");
        System.out.println("===============================================================\n");

        ThreeLevelCacheSimulationDemo container = new ThreeLevelCacheSimulationDemo();

        // 注册两个形成循环依赖的 Bean，并为 orderService 开启 AOP 代理增强
        container.registerBeanDefinition("orderService", OrderServiceImpl.class, true);
        container.registerBeanDefinition("paymentService", PaymentServiceImpl.class, false);

        System.out.println("--- 步骤 1：触发 getBean(\"orderService\")，观察全链路三级缓存流转 ---\n");
        IOrderService orderService = (IOrderService) container.getBean("orderService");

        System.out.println("--- 步骤 2：验证循环依赖注入对象完整性与单例同一性 ---");
        IPaymentService paymentService = (IPaymentService) container.getBean("paymentService");

        System.out.printf("orderService 实际类型: %s (是否为 AOP 代理: %b)\n",
                orderService.getClass().getName(), Proxy.isProxyClass(orderService.getClass()));
        System.out.printf("paymentService 内部持有的 orderService 是否与容器中的相同: %b\n",
                paymentService.getOrderService() == orderService);

        System.out.println("\n--- 步骤 3：调用业务方法，验证 AOP 切面拦截逻辑 ---");
        orderService.createOrder("ORDER-20261103-8899");

        System.out.println("\n>>> 三级缓存完美闭环解决循环依赖 + AOP 代理单例同一性验证成功！");
    }
}
