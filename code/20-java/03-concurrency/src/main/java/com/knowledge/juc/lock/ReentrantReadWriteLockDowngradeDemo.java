package com.knowledge.juc.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 读写锁 ReentrantReadWriteLock 深度实战：
 * 1. 锁降级 (Lock Downgrading): 持有写锁 -> 获取读锁 -> 释放写锁 -> 释放读锁（官方唯一支持的安全降级路径）
 * 2. 为什么需要锁降级？保证在写锁释放后，当前线程能无缝持续感知最新数据，防止被其他抢占写锁的线程插入脏写！
 * 3. 锁升级 (Lock Upgrading) 陷阱：持有读锁尝试获取写锁，若多个读线程同时升级必将导致永久死锁！JUC 源码直接禁止锁升级。
 */
public class ReentrantReadWriteLockDowngradeDemo {

    private final Map<String, String> cache = new HashMap<>();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = rwLock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = rwLock.writeLock();

    public static void main(String[] args) {
        ReentrantReadWriteLockDowngradeDemo demo = new ReentrantReadWriteLockDowngradeDemo();
        System.out.println("========== 1. 规范安全锁降级全流程演示 ==========");
        String val = demo.getDataWithLockDowngrade("config.app.version");
        System.out.println("成功获取数据: " + val);

        System.out.println("\n========== 2. 锁升级死锁禁区原理解析 ==========");
        System.out.println("【为什么 JUC 禁止持有读锁直接获取写锁？】");
        System.out.println("假设线程 A 与线程 B 同时持有读锁。此时 A 尝试获取写锁，必须等待 B 释放读锁；");
        System.out.println("与此同时 B 也尝试获取写锁，必须等待 A 释放读锁。");
        System.out.println("双方互相等待对方释放读锁，瞬间发生环形死锁！因此 ReentrantReadWriteLock 源码中读锁内申请写锁将永远挂起。");
    }

    /**
     * 优雅的读写双重检查与安全锁降级模板
     */
    public String getDataWithLockDowngrade(String key) {
        // 1. 首次加读锁
        readLock.lock();
        String value;
        try {
            value = cache.get(key);
        } finally {
            readLock.unlock();
        }

        // 2. 缓存未命中，需要写入
        if (value == null) {
            // 加写锁
            writeLock.lock();
            try {
                // 双重检查，防止其他线程在当前线程释放读锁后、获取写锁前已经写入
                value = cache.get(key);
                if (value == null) {
                    System.out.println(">>> [写锁持有中] 正在模拟从数据库加载 key=" + key + " 并写入缓存...");
                    value = "v3.3.5-LTS";
                    cache.put(key, value);
                }

                // 3. 【核心：锁降级开始】在释放写锁之前，必须先获取读锁！
                readLock.lock();
                System.out.println(">>> [锁降级完成] 已持有读锁，准备释放写锁...");
            } finally {
                // 4. 释放写锁。此时当前线程依然持有读锁，其他写线程无法插队修改，数据绝对一致！
                writeLock.unlock();
            }

            // 5. 使用数据并最终释放读锁
            try {
                System.out.println(">>> [持有降级后的读锁执行业务] 消费数据: " + value);
            } finally {
                readLock.unlock();
                System.out.println(">>> [流程结束] 读锁已释放。");
            }
        }
        return value;
    }
}
