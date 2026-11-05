package com.knowledge.juc.atomic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * CAS (Compare-And-Swap) 与 ABA 偷梁换柱问题实战：
 * 1. 传统 AtomicReference 只能感知值是否相同，无法感知值经历过 A -> B -> A 的变更轨迹
 * 2. 模拟 ABA 攻击场景：内存对象看似未变，但底层关联状态（如链表节点拓扑、账户流水）已被污染
 * 3. 使用带有版本戳的 AtomicStampedReference (Reference + Stamp) 彻底解决 ABA 问题
 */
public class CasAbaAtomicStampedDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 1. 现场复现 AtomicReference 的 ABA 缺陷 ==========");
        reproduceAbaProblem();

        System.out.println("\n========== 2. 使用 AtomicStampedReference 解决 ABA ==========");
        resolveAbaWithStamp();
    }

    private static void reproduceAbaProblem() throws InterruptedException {
        AtomicReference<String> ref = new AtomicReference<>("A");

        Thread t1 = new Thread(() -> {
            String initial = ref.get();
            System.out.println("[线程 T1] 读取初始值: " + initial + "，准备休眠 1 秒模拟长耗时业务...");
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // T1 唤醒后尝试 CAS 将 A 更改为 C
            boolean success = ref.compareAndSet(initial, "C");
            System.out.println("[线程 T1] CAS(A, C) 结果: " + success + "，当前最新值: " + ref.get());
            System.err.println(">>> 观察: T1 成功执行了 CAS，但在它休眠期间，值早已被其他线程偷偷经历了 A -> B -> A！");
        }, "T1");

        Thread t2 = new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(100); // 确保 T1 先读到 A
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            System.out.println("  [线程 T2] 迅速将 A 改为 B: " + ref.compareAndSet("A", "B"));
            System.out.println("  [线程 T2] 紧接着将 B 改回 A: " + ref.compareAndSet("B", "A"));
            System.out.println("  [线程 T2] 完成了一次完美的 ABA 偷梁换柱！");
        }, "T2");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }

    private static void resolveAbaWithStamp() throws InterruptedException {
        // 初始值 "A"，初始版本号 1
        AtomicStampedReference<String> stampedRef = new AtomicStampedReference<>("A", 1);

        Thread t1 = new Thread(() -> {
            int stamp = stampedRef.getStamp();
            String initial = stampedRef.getReference();
            System.out.println("[线程 T1] 取得初始值: " + initial + "，版本戳 stamp: " + stamp + "，休眠 1 秒...");

            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // CAS 同时校验预期引用与预期版本戳
            boolean success = stampedRef.compareAndSet(initial, "C", stamp, stamp + 1);
            System.out.println("[线程 T1] CAS(A, C, stamp=" + stamp + ") 结果: " + success);
            if (!success) {
                System.out.println("[线程 T1] 成功感知到数据已被篡改！当前实际 stamp=" + stampedRef.getStamp() + ", 拒绝操作！");
            }
        }, "T1-Stamp");

        Thread t2 = new Thread(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            int s1 = stampedRef.getStamp();
            stampedRef.compareAndSet("A", "B", s1, s1 + 1);
            System.out.println("  [线程 T2] 将 A 改为 B，版本戳递增为: " + stampedRef.getStamp());

            int s2 = stampedRef.getStamp();
            stampedRef.compareAndSet("B", "A", s2, s2 + 1);
            System.out.println("  [线程 T2] 将 B 改回 A，版本戳递增为: " + stampedRef.getStamp());
        }, "T2-Stamp");

        t1.start();
        t2.start();
        t1.join();
        t2.join();
    }
}
