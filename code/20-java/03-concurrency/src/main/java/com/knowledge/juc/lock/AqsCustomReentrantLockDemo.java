package com.knowledge.juc.lock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * 基于 AbstractQueuedSynchronizer (AQS) 手写可重入独占锁实战演练
 *
 * 核心原理剖析：
 * 1. 状态位 state：
 *    - state = 0 表示当前锁空闲无线程持有；
 *    - state > 0 表示已被某个线程独占，数值代表当前持有线程的重入次数。
 * 2. 独占持有者 ExclusiveOwnerThread：
 *    - 记录持有锁的线程实例，重入时校验 Thread.currentThread() == getExclusiveOwnerThread()。
 * 3. AQS 内部同步队列（CLH 变体双向链表）：
 *    - 竞争失败的线程被封装为 Node 节点入队自旋或挂起（LockSupport.park()），由释放锁的线程唤醒（unparkSuccessor）。
 */
public class AqsCustomReentrantLockDemo {

    /**
     * 自定义基于 AQS 的可重入独占互斥锁
     */
    public static class SimpleReentrantLock implements Lock {

        /**
         * 内部同步器，继承自 AQS
         */
        private static final class Sync extends AbstractQueuedSynchronizer {

            @Override
            protected boolean tryAcquire(int acquires) {
                final Thread current = Thread.currentThread();
                int c = getState();

                // 1. 无锁状态，尝试 CAS 抢占
                if (c == 0) {
                    if (compareAndSetState(0, acquires)) {
                        setExclusiveOwnerThread(current);
                        return true;
                    }
                }
                // 2. 已有锁，校验是否为当前线程重入
                else if (current == getExclusiveOwnerThread()) {
                    int nextc = c + acquires;
                    if (nextc < 0) { // 溢出防护
                        throw new Error("Maximum lock count exceeded");
                    }
                    setState(nextc);
                    return true;
                }

                // 3. 锁已被其它线程持有，获取失败
                return false;
            }

            @Override
            protected boolean tryRelease(int releases) {
                // 释放锁前必须校验调用者是否为持有线程
                if (Thread.currentThread() != getExclusiveOwnerThread()) {
                    throw new IllegalMonitorStateException("当前线程并未持有该锁，无法释放！");
                }

                int c = getState() - releases;
                boolean free = false;
                // 重入次数减为 0 时才真正释放锁持有权
                if (c == 0) {
                    free = true;
                    setExclusiveOwnerThread(null);
                }
                setState(c);
                return free;
            }

            @Override
            protected boolean isHeldExclusively() {
                return getExclusiveOwnerThread() == Thread.currentThread();
            }

            ConditionObject newCondition() {
                return new ConditionObject();
            }

            int getHoldCount() {
                return isHeldExclusively() ? getState() : 0;
            }
        }

        private final Sync sync = new Sync();

        @Override
        public void lock() {
            sync.acquire(1);
        }

        @Override
        public void lockInterruptibly() throws InterruptedException {
            sync.acquireInterruptibly(1);
        }

        @Override
        public boolean tryLock() {
            return sync.tryAcquire(1);
        }

        @Override
        public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
            return sync.tryAcquireNanos(1, unit.toNanos(time));
        }

        @Override
        public void unlock() {
            sync.release(1);
        }

        @Override
        public Condition newCondition() {
            return sync.newCondition();
        }

        public int getHoldCount() {
            return sync.getHoldCount();
        }
    }

    // ======================= 场景模拟与验证 =======================

    private static final SimpleReentrantLock customLock = new SimpleReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("===============================================================");
        System.out.println("       JUC 核心实战：基于 AQS 手写可重入独占锁与多线程争抢验证       ");
        System.out.println("===============================================================\n");

        // 场景 1：单线程重入测试
        System.out.println("--- 场景 1：验证同一个线程的多次可重入性 ---");
        reentrantMethodA();

        System.out.println("\n---------------------------------------------------------------");
        System.out.println("--- 场景 2：验证多线程并发争抢与排他互斥性 ---");

        Runnable task = () -> {
            String threadName = Thread.currentThread().getName();
            System.out.printf("[%s] 准备尝试获取锁...\n", threadName);
            customLock.lock();
            try {
                System.out.printf("  >>> [%s] 成功获取锁！进入临界区，当前重入计数: %d\n", threadName, customLock.getHoldCount());
                // 模拟业务处理
                TimeUnit.MILLISECONDS.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                System.out.printf("  <<< [%s] 临界区执行完毕，释放锁...\n", threadName);
                customLock.unlock();
            }
        };

        Thread t1 = new Thread(task, "Worker-1");
        Thread t2 = new Thread(task, "Worker-2");
        Thread t3 = new Thread(task, "Worker-3");

        t1.start();
        t2.start();
        t3.start();

        t1.join();
        t2.join();
        t3.join();

        System.out.println("\n>>> 所有并发争抢测试全部安全完成，AQS 独占锁逻辑完全闭环！");
    }

    private static void reentrantMethodA() {
        customLock.lock();
        try {
            System.out.printf("[%s] 进入 methodA，当前重入计数 holdCount = %d\n",
                    Thread.currentThread().getName(), customLock.getHoldCount());
            reentrantMethodB();
        } finally {
            customLock.unlock();
            System.out.printf("[%s] 退出 methodA，释放后重入计数 holdCount = %d\n",
                    Thread.currentThread().getName(), customLock.getHoldCount());
        }
    }

    private static void reentrantMethodB() {
        customLock.lock();
        try {
            System.out.printf("[%s]   -> 嵌套进入 methodB，当前重入计数 holdCount = %d\n",
                    Thread.currentThread().getName(), customLock.getHoldCount());
            reentrantMethodC();
        } finally {
            customLock.unlock();
            System.out.printf("[%s]   <- 退出 methodB，释放后重入计数 holdCount = %d\n",
                    Thread.currentThread().getName(), customLock.getHoldCount());
        }
    }

    private static void reentrantMethodC() {
        customLock.lock();
        try {
            System.out.printf("[%s]       -> 嵌套进入 methodC (最深层)，当前重入计数 holdCount = %d\n",
                    Thread.currentThread().getName(), customLock.getHoldCount());
        } finally {
            customLock.unlock();
            System.out.printf("[%s]       <- 退出 methodC，释放后重入计数 holdCount = %d\n",
                    Thread.currentThread().getName(), customLock.getHoldCount());
        }
    }
}
