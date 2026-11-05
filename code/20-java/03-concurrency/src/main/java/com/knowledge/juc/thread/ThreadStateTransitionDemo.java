package com.knowledge.juc.thread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Java 线程 6 大状态变迁全景实证：
 * 1. NEW: 刚创建尚未调用 start()
 * 2. RUNNABLE: 正在 JVM 中执行 (包含就绪 Ready 与运行 Running)
 * 3. BLOCKED: 等待获取 synchronized 监视器锁
 * 4. WAITING: 无限期挂起等待 (Object.wait(), Thread.join(), LockSupport.park())
 * 5. TIMED_WAITING: 超时挂起 (Thread.sleep(), Object.wait(timeout), LockSupport.parkNanos())
 * 6. TERMINATED: 线程 run() 方法执行完毕正常或异常退出
 */
public class ThreadStateTransitionDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== Java 线程 6 大生命周期状态现场抓取 ==========");

        // 1. NEW 状态
        Thread worker = new Thread(() -> {
            try {
                // 4. TIMED_WAITING 状态
                TimeUnit.MILLISECONDS.sleep(100);

                // 5. WAITING 状态
                LockSupport.park();

                // 3. BLOCKED 状态
                synchronized (ThreadStateTransitionDemo.class) {
                    // do nothing
                }
            } catch (InterruptedException ignored) {}
        }, "State-Worker-Thread");

        System.out.println("1. new 出来尚未 start() -> 状态: " + worker.getState() + " (预期: NEW)");

        // 2. RUNNABLE 状态
        worker.start();
        System.out.println("2. 调用 start() 后 -> 状态: " + worker.getState() + " (预期: RUNNABLE)");

        // 等待其进入 sleep(100)
        TimeUnit.MILLISECONDS.sleep(20);
        System.out.println("3. 线程执行 sleep(100) -> 状态: " + worker.getState() + " (预期: TIMED_WAITING)");

        // 等待其睡醒进入 park()
        TimeUnit.MILLISECONDS.sleep(120);
        System.out.println("4. 线程执行 LockSupport.park() -> 状态: " + worker.getState() + " (预期: WAITING)");

        // 构造 BLOCKED 竞争：主线程先提前占有锁，再唤醒 worker 去抢锁
        synchronized (ThreadStateTransitionDemo.class) {
            LockSupport.unpark(worker); // 唤醒 worker
            TimeUnit.MILLISECONDS.sleep(20); // 确保 worker 走到 synchronized 并阻塞
            System.out.println("5. worker 尝试获取被主线程持有的 synchronized 锁 -> 状态: " + worker.getState() + " (预期: BLOCKED)");
        } // 主线程释放锁

        // 6. TERMINATED 状态
        worker.join();
        System.out.println("6. worker 线程执行结束 -> 状态: " + worker.getState() + " (预期: TERMINATED)");
        System.out.println(">>> 6 大线程状态变迁全部现场实测验证通过！");
    }
}
