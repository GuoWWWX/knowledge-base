package com.knowledge.spring.lifecycle;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Bean 完整生命周期打桩透视：
 * 1. 实例化 (Instantiation) -> 构造函数
 * 2. 属性填充 (Populate Properties)
 * 3. Aware 容器感知接口回调 (BeanNameAware, ApplicationContextAware)
 * 4. BeanPostProcessor 前置处理 (postProcessBeforeInitialization)
 * 5. @PostConstruct 初始化回调
 * 6. InitializingBean.afterPropertiesSet()
 * 7. 自定义 init-method 初始化
 * 8. BeanPostProcessor 后置处理 (postProcessAfterInitialization，此处发生 AOP 动态代理包装！)
 * 9. Bean 就绪供业务使用
 * 10. @PreDestroy / DisposableBean.destroy() / 自定义 destroy-method
 */
public class BeanLifeCycleExtensionDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 启动 Spring 容器并触发 Bean 生命周期 ==========");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);

        System.out.println("\n========== 2. 从容器提取 Bean 并执行业务 ==========");
        LifecycleWorker worker = context.getBean(LifecycleWorker.class);
        worker.doWork();

        System.out.println("\n========== 3. 关闭 Spring 容器触发 Bean 销毁流程 ==========");
        context.close();
    }

    @Configuration
    static class AppConfig {
        @Bean(initMethod = "customInit", destroyMethod = "customDestroy")
        public LifecycleWorker lifecycleWorker() {
            return new LifecycleWorker();
        }

        @Bean
        public CustomBeanPostProcessor customBeanPostProcessor() {
            return new CustomBeanPostProcessor();
        }
    }

    public static class LifecycleWorker implements BeanNameAware, InitializingBean, DisposableBean {
        private String beanName;

        public LifecycleWorker() {
            System.out.println("  [第 1 步: 实例化] 调用 LifecycleWorker 构造函数");
        }

        @Override
        public void setBeanName(String name) {
            this.beanName = name;
            System.out.println("  [第 2 步: Aware 回调] BeanNameAware.setBeanName: " + name);
        }

        @PostConstruct
        public void postConstruct() {
            System.out.println("  [第 4 步: 初始化] @PostConstruct 标注的方法执行");
        }

        @Override
        public void afterPropertiesSet() {
            System.out.println("  [第 5 步: 初始化] InitializingBean.afterPropertiesSet() 执行");
        }

        public void customInit() {
            System.out.println("  [第 6 步: 初始化] @Bean(initMethod) 自定义初始化方法执行");
        }

        public void doWork() {
            System.out.println("  >>> [第 8 步: 正常业务执行] LifecycleWorker 处理核心业务！");
        }

        @PreDestroy
        public void preDestroy() {
            System.out.println("  [第 9 步: 销毁] @PreDestroy 标注的方法执行");
        }

        @Override
        public void destroy() {
            System.out.println("  [第 10 步: 销毁] DisposableBean.destroy() 执行");
        }

        public void customDestroy() {
            System.out.println("  [第 11 步: 销毁] @Bean(destroyMethod) 自定义销毁方法执行");
        }
    }

    public static class CustomBeanPostProcessor implements BeanPostProcessor {
        @Override
        public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof LifecycleWorker) {
                System.out.println("  [第 3 步: BPP 前置] BeanPostProcessor.postProcessBeforeInitialization");
            }
            return bean;
        }

        @Override
        public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
            if (bean instanceof LifecycleWorker) {
                System.out.println("  [第 7 步: BPP 后置] BeanPostProcessor.postProcessAfterInitialization (在此处完成 AOP 动态代理生成)");
            }
            return bean;
        }
    }
}
