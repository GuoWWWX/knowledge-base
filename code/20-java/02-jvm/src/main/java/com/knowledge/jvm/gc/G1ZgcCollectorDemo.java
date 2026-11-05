package com.knowledge.jvm.gc;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * 现代 JVM 垃圾回收器深度实战与原理探测：
 * 1. 运行时动态采集 GarbageCollectorMXBean 探测当前虚拟机启用的垃圾收集器与 GC 次数、总停顿耗时
 * 2. 模拟高频短命临时对象与超大对象 (Humongous Object) 分配触发 GC
 * 3. 核心机制对比：
 *    - G1 GC: 基于 Region 划分 (Eden, Survivor, Old, Humongous)，停顿预测模型 (MaxGCPauseMillis)
 *    - ZGC: 染色指针 (Colored Pointers, 4位元数据)、读屏障 (Load Barrier)、全并发标记整理，实现停顿 < 1ms
 */
public class G1ZgcCollectorDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 1. 当前 JVM 垃圾收集器 (GC) 运行状态监控 ==========");
        printGcStatus();

        System.out.println("\n========== 2. 模拟高频短命对象分配 (触发 Minor GC / Young GC) ==========");
        simulateYoungGenerationAllocation();

        System.out.println("\n========== 3. 模拟超大对象分配 (Humongous Object) ==========");
        simulateHumongousAllocation();

        System.out.println("\n========== 4. GC 执行后指标复盘 ==========");
        printGcStatus();

        System.out.println("\n>>> 【生产调优建议与架构选型】:");
        System.out.println("1. 堆内存 4GB - 16GB: 推荐使用 G1 GC (-XX:+UseG1GC -XX:MaxGCPauseMillis=200)");
        System.out.println("2. 堆内存 16GB+ 或超低延迟场景: 强烈推荐 Java 21+ 分代 ZGC (-XX:+UseZGC -XX:+ZGenerational)");
        System.out.println("   ZGC 无论堆是 16GB 还是 16TB，GC 停顿时间始终压制在 1 毫秒以内，彻底告别 Stop-The-World 恶梦！");
    }

    private static void printGcStatus() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            System.out.printf("收集器名称: %-25s | 收集总次数: %5d 次 | 累计停顿耗时: %5d ms%n",
                    gcBean.getName(), gcBean.getCollectionCount(), gcBean.getCollectionTime());
        }
    }

    private static void simulateYoungGenerationAllocation() {
        long start = System.currentTimeMillis();
        // 分配 500,000 个短命对象，迅速撑爆 Young 代 Eden 区，触发并发/STW 垃圾回收
        for (int i = 0; i < 500_000; i++) {
            byte[] shortLived = new byte[1024]; // 1KB
            if (i % 100_000 == 0) {
                // 偶尔有轻微引用暂留
                shortLived[0] = (byte) (i & 0xFF);
            }
        }
        long duration = System.currentTimeMillis() - start;
        System.out.printf("短命对象快速分配完成，耗时: %d ms%n", duration);
    }

    private static void simulateHumongousAllocation() {
        // G1 中，如果对象大小超过 Region 大小的一半 (默认 Region 为 1MB~32MB)，会判定为 Humongous 大对象
        // 大对象直接在老年代连续的 Humongous Region 分配，容易引发内存碎片和早期并发标记
        List<byte[]> bigObjects = new ArrayList<>();
        System.out.println("正在分配 5 个 2MB 大对象...");
        for (int i = 0; i < 5; i++) {
            bigObjects.add(new byte[2 * 1024 * 1024]);
        }
        System.out.println("大对象分配完成，当前持有引用数: " + bigObjects.size());
        bigObjects.clear(); // 释放引用，允许回收
    }
}
