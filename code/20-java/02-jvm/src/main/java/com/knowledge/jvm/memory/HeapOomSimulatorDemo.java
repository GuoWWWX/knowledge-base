package com.knowledge.jvm.memory;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

/**
 * 堆内存溢出（Java heap space）模拟与生产级全链路排障指南
 * <p>
 * 实验重点：
 * 1. 动态探测当前 JVM 堆内存规格（Init, Used, Committed, Max）。
 * 2. 受控阶梯式递增内存占用，实时打印 JVM 堆内存逼近极限的物理消耗轨迹。
 * 3. 捕获 java.lang.OutOfMemoryError 并在异常现场打印诊断报告。
 * 4. 深度总结生产级 OOM 黄金排障三板斧：JVM 参数守卫、排障工具链、根因分析法。
 * </p>
 *
 * @author Knowledge Base
 */
public class HeapOomSimulatorDemo {

    public static void main(String[] args) {
        printHeader("JVM 堆内存配置概况");
        printHeapUsage("初始状态");

        printHeader("实验：模拟高吞吐大对象分配引发 java.lang.OutOfMemoryError: Java heap space");
        simulateHeapOom();

        printHeader("生产级 OOM 黄金排障全景手册");
        printOomTroubleshootingGuide();
    }

    /**
     * 阶梯式模拟内存打满
     */
    private static void simulateHeapOom() {
        List<byte[]> memoryBlackHole = new ArrayList<>();
        final int CHUNK_SIZE_MB = 2; // 每次分配 2MB
        final byte[] dummyData = new byte[CHUNK_SIZE_MB * 1024 * 1024];

        System.out.println("-> 开始以每次 " + CHUNK_SIZE_MB + " MB 的步长持续向强引用 List 追加 byte 数组...");
        System.out.println("-----------------------------------------------------------------------------------------");
        System.out.printf("%-8s | %-12s | %-14s | %-14s | %-14s%n",
                "迭代次数", "累计分配(MB)", "已用堆(Used MB)", "已申请(Commit MB)", "最大堆(Max MB)");
        System.out.println("-----------------------------------------------------------------------------------------");

        int iteration = 0;
        try {
            while (true) {
                iteration++;
                // 持续追加新的 2MB 数组（保持强引用，垃圾收集器无法回收）
                memoryBlackHole.add(new byte[CHUNK_SIZE_MB * 1024 * 1024]);

                // 每累计分配 20MB 打印一次内存使用水位
                if (iteration % 10 == 0) {
                    MemoryUsage usage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
                    long usedMb = usage.getUsed() / (1024 * 1024);
                    long commitMb = usage.getCommitted() / (1024 * 1024);
                    long maxMb = usage.getMax() / (1024 * 1024);
                    long allocatedTotal = (long) iteration * CHUNK_SIZE_MB;

                    System.out.printf("#%-7d | %-12d | %-14d | %-14d | %-14d%n",
                            iteration, allocatedTotal, usedMb, commitMb, maxMb);
                }
            }
        } catch (OutOfMemoryError oom) {
            System.out.println("-----------------------------------------------------------------------------------------");
            System.err.println("【现场成功捕获 OOM 异常】: " + oom);
            System.out.printf(" 崩塌时刻总迭代轮次: %d 次，累计强引用内存载荷: 约 %d MB%n",
                    iteration, (long) iteration * CHUNK_SIZE_MB);

            // 释放强引用集合，使得后续打印报告能够正常进行，避免死循环二次 OOM
            memoryBlackHole = null;
            System.gc();

            System.out.println("【内存恢复】强引用解除并建议 System.gc()，当前 JVM 恢复正常运行状态。");
        }
    }

    private static void printHeapUsage(String stage) {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();

        System.out.printf("[%s] JVM 堆内存规格：%n", stage);
        System.out.printf("  - 初始内存 (Init)     : %6d MB%n", heapUsage.getInit() / (1024 * 1024));
        System.out.printf("  - 当前已用 (Used)     : %6d MB%n", heapUsage.getUsed() / (1024 * 1024));
        System.out.printf("  - 当前已提交 (Committed): %6d MB%n", heapUsage.getCommitted() / (1024 * 1024));
        System.out.printf("  - 最大可用 (Max)      : %6d MB%n", heapUsage.getMax() / (1024 * 1024));
    }

    private static void printOomTroubleshootingGuide() {
        System.out.println("【第一步：生产 JVM 启动底线参数（必须标配）】：");
        System.out.println("  -XX:+HeapDumpOnOutOfMemoryError");
        System.out.println("      -> 保证在 OOM 发生时，JVM 会自动在崩溃现场生成当时的快照 .hprof 文件。");
        System.out.println("  -XX:HeapDumpPath=/data/logs/jvm_dump.hprof");
        System.out.println("      -> 指定快照写入目录，务必保证该磁盘分区剩余空间充足（至少大于最大堆内存）。");
        System.out.println("  -XX:+ExitOnOutOfMemoryError");
        System.out.println("      -> 在微服务/K8s 容器环境下，OOM 后进程内部线程通常处于僵死或半失控状态，立刻退出让 Pod 自动重启是最健壮方案！");
        System.out.println();
        System.out.println("【第二步：内存泄露（Memory Leak） vs 内存溢出（Memory Overflow）本质研判】：");
        System.out.println(" 1. 内存溢出（Overflow）：业务吞吐量瞬间暴涨，堆内存确实不够用，或者大批量请求并发拉取了超大结果集。");
        System.out.println("    - 解决方案：扩容堆内存（如 -Xmx 从 2G -> 4G）、限制单次批量查询数据量（分页）、限流降级。");
        System.out.println(" 2. 内存泄露（Leak）：对象已经无用，但由于垃圾收集器判定其存在到 GC Roots 的强引用链，无法被回收。");
        System.out.println("    - 典型根因一：static 静态集合（Map/List）只加不减，未配置弱引用（WeakReference）或淘汰策略。");
        System.out.println("    - 典型根因二：ThreadLocal 在使用后没有显式在 finally 块中调用 remove()，线程池复用导致对象越积越多。");
        System.out.println("    - 典型根因三：内部类/匿名类隐式持有外部类引用，导致外部大对象无法被回收。");
        System.out.println();
        System.out.println("【第三步：排障工具链与分析三部曲】：");
        System.out.println(" 1. 现场观测：使用 arthas 的 `dashboard` 或 `heapdump /tmp/dump.hprof` 在线诊断。");
        System.out.println(" 2. 离线分析：将 dump 文件拉取到本地，使用 Eclipse MAT (Memory Analyzer Tool) 或 JProfiler 打开。");
        System.out.println(" 3. 核心视图：");
        System.out.println("    - 查看 'Leak Suspects'（泄露嫌疑报告）：MAT 自动计算疑似最大内存元凶。");
        System.out.println("    - 查看 'Dominator Tree'（支配树）：按 Retained Size（保留内存大小）降序排列，定位占用空间最大的前几个类。");
        System.out.println("    - 查看 'Path to GC Roots'：顺藤摸瓜找到是哪条静态变量或常驻线程死死拽住了该对象，直接锁定问题代码行！");
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println(" " + title);
        System.out.println("=".repeat(100));
    }
}
