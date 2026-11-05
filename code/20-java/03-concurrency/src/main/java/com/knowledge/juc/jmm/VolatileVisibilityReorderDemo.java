package com.knowledge.juc.jmm;

import java.util.concurrent.TimeUnit;

/**
 * JMM (Java 内存模型) 与 volatile 关键字深度解密：
 * 1. 内存可见性：工作内存（CPU 高速缓存 L1/L2）未写回主内存，导致另一个线程陷入无限死循环
 * 2. JIT 优化激进化：JIT 编译器发现循环体内没有变量修改，将 while(running) 激进优化为 while(true) 永不停机！
 * 3. volatile 读写屏障 (Memory Barrier): 强制刷新主存并禁止指令重排序
 */
public class VolatileVisibilityReorderDemo {

    // 状态标志位：如果缺少 volatile，子线程将因 JIT 优化和缓存不一致永远无法感知主线程的修改
    private static volatile boolean running = true;
    private static int counter = 0;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 1. JMM 内存可见性实测 ==========");
        Thread worker = new Thread(() -> {
            System.out.println("工作线程启动，开始监听 running 标志位...");
            while (running) {
                // 循环体内不调用 System.out.println() 或 Thread.sleep()，
                // 因为它们内部有 synchronized 同步代码块，会强制触发缓存行失效，掩盖可见性 Bug！
                counter++;
            }
            System.out.println("工作线程成功感知 running 变为 false，退出循环！累计计数: " + counter);
        }, "Worker-Thread");

        worker.start();

        // 主线程睡眠 500 毫秒
        TimeUnit.MILLISECONDS.sleep(500);

        System.out.println("主线程将 running 设置为 false...");
        running = false;

        // 等待工作线程结束
        worker.join(2000);

        if (worker.isAlive()) {
            System.err.println("【可见性灾难】工作线程未能在 2 秒内停止！依然深陷在工作内存死循环中！");
            worker.interrupt();
        } else {
            System.out.println("【验证通过】volatile 内存屏障生效，工作线程准时终止！");
        }

        System.out.println("\n========== 2. volatile 禁止指令重排序底层原理解析 ==========");
        System.out.println("底层硬件机制 (x86 架构):");
        System.out.println("1. volatile 写操作前插入 StoreStore 屏障，写操作后插入 StoreLoad 屏障 (lock 前缀指令，锁总线/缓存一致性 MESI 协议)");
        System.out.println("2. volatile 读操作后插入 LoadLoad 和 LoadStore 屏障");
        System.out.println("3. 彻底防止双重检查锁定 (DCL) 单例中 `instance = new Singleton()` 由于 '分配内存->赋值引用->初始化对象' 重排序导致的半初始化对象逸出灾难！");
    }
}
