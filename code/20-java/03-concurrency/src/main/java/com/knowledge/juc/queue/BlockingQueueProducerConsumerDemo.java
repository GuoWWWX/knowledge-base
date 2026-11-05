package com.knowledge.juc.queue;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 手写有界阻塞队列与生产消费模型：
 * 1. 深度还原 ArrayBlockingQueue 底层原理
 * 2. 基于 ReentrantLock + 两个 Condition 条件变量 (notFull / notEmpty)
 * 3. 队列满时阻塞生产者，入队后精准唤醒阻塞的消费者；队列空时阻塞消费者，出队后精准唤醒阻塞的生产者
 */
public class BlockingQueueProducerConsumerDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 手写阻塞队列实测 (容量=2) ==========");
        CustomBlockingQueue<String> queue = new CustomBlockingQueue<>(2);

        // 启动生产者线程：生产速度快
        Thread producer = new Thread(() -> {
            try {
                for (int i = 1; i <= 4; i++) {
                    String task = "Task-" + i;
                    queue.put(task);
                    System.out.println("-> [生产者成功生产]: " + task + " (当前队列大小: " + queue.size() + ")");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Producer-Thread");

        // 启动消费者线程：消费速度慢（模拟耗时业务）
        Thread consumer = new Thread(() -> {
            try {
                // 先休眠 300ms，让生产者迅速把容量为 2 的队列打满并触发阻塞
                TimeUnit.MILLISECONDS.sleep(300);
                for (int i = 1; i <= 4; i++) {
                    String task = queue.take();
                    System.out.println("<- [消费者成功提取]: " + task + " (当前队列大小: " + queue.size() + ")");
                    TimeUnit.MILLISECONDS.sleep(200);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Consumer-Thread");

        producer.start();
        consumer.start();

        producer.join();
        consumer.join();
        System.out.println(">>> 生产消费流水线圆满完成，零死锁、零忙轮询，线程精准挂起与唤醒！");
    }

    public static class CustomBlockingQueue<E> {
        private final Object[] items;
        private int takeIndex = 0;
        private int putIndex = 0;
        private int count = 0;

        private final ReentrantLock lock = new ReentrantLock();
        private final Condition notEmpty = lock.newCondition();
        private final Condition notFull = lock.newCondition();

        public CustomBlockingQueue(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException();
            this.items = new Object[capacity];
        }

        public void put(E element) throws InterruptedException {
            lock.lockInterruptibly();
            try {
                // 队列已满，生产者挂起等待 notFull 信号（必须在 while 循环中判断防止虚假唤醒 Spurious Wakeup）
                while (count == items.length) {
                    System.out.println(">>> [队列已满 " + count + "/" + items.length + "] 生产者挂起等待空位...");
                    notFull.await();
                }

                // 循环数组入队
                items[putIndex] = element;
                if (++putIndex == items.length) putIndex = 0;
                count++;

                // 唤醒可能阻塞的消费者
                notEmpty.signal();
            } finally {
                lock.unlock();
            }
        }

        @SuppressWarnings("unchecked")
        public E take() throws InterruptedException {
            lock.lockInterruptibly();
            try {
                // 队列为空，消费者挂起等待 notEmpty 信号
                while (count == 0) {
                    System.out.println("<<< [队列已空] 消费者挂起等待新数据...");
                    notEmpty.await();
                }

                // 循环数组出队
                E item = (E) items[takeIndex];
                items[takeIndex] = null; // help GC
                if (++takeIndex == items.length) takeIndex = 0;
                count--;

                // 唤醒可能阻塞的生产者
                notFull.signal();
                return item;
            } finally {
                lock.unlock();
            }
        }

        public int size() {
            lock.lock();
            try {
                return count;
            } finally {
                lock.unlock();
            }
        }
    }
}
