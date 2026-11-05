package com.knowledge.jvm.gc;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.util.ArrayList;
import java.util.List;

/**
 * =========================================================================================
 * 深入理解新生代对象分配、Survivor 轮换与老年代晋升（Allocation, Promotion & Tenuring）
 * =========================================================================================
 *
 * <p>【学习目标与核心心智模型】：
 * 1. 为什么是 8:1:1？
 *    - 基于弱分代假说：90%~98% 对象朝生夕死。
 *    - Eden (80%) + S0 (10%) + S1 (10%)，日常可用空间达 90%，仅牺牲 10% 作为中转缓冲，彻底消除碎片。
 * 2. 存活对象何时进老年代（晋升决策分流，一步到位）？
 *    - 规则一：年龄达到阈值（MaxTenuringThreshold，默认 15，对象头 4bit 限制）；
 *    - 规则二：动态对象年龄判定（同龄对象累加占用超过 Survivor 的 50% / TargetSurvivorRatio）；
 *    - 规则三：Survivor 空间放不下（分配担保直接晋升老年代，绝不是先塞入 To 区再挑出来）；
 *    - 规则四：大对象直接进老年代（PretenureSizeThreshold / G1 Humongous）。
 *
 * <p>【推荐最佳体验运行参数】（在 IDEA Run Configuration 中添加 VM options）：
 * {@code -Xms60m -Xmx60m -XX:NewRatio=2 -XX:SurvivorRatio=8 -Xlog:gc* -XX:+UseSerialGC}
 * （使用固定 60M 堆 + SerialGC 能最纯粹、直观地看清分代内存池的每一次涨落）
 *
 * @author Antigravity
 * @version 1.0
 */
public class YoungGenPromotionAndTenuringDemo {

    private static final int _1MB = 1024 * 1024;
    private static final int _512KB = 512 * 1024;

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=====================================================================");
        System.out.println("   JVM 新生代对象分配、Survivor 轮换与分配担保晋升机制实战演练");
        System.out.println("=====================================================================\n");

        printMemoryPools("【初始基线状态】");

        // ---------------------------------------------------------------------------------
        // 场景 1：小对象在 Eden 诞生与高频朝生夕灭（体现 8:1:1 的优势）
        // ---------------------------------------------------------------------------------
        System.out.println("\n---------------- 场景 1：短命小对象在 Eden 批量生成与自生自灭 ----------------");
        System.out.println("-> 模拟产生 30MB 临时数据，但全部为局部变量，不保留外部引用...");
        for (int i = 0; i < 30; i++) {
            byte[] transientData = new byte[_1MB]; // 循环内分配，循环迭代后即变垃圾
            if (transientData.length == 0) System.out.println("unreachable");
        }
        System.out.println("-> 临时对象产生完毕，绝大多数已被 Minor GC 轻松推平回收！");
        printMemoryPools("【场景 1 完成后状态】");

        // ---------------------------------------------------------------------------------
        // 场景 2：Survivor 空间充裕时，存活对象保留在新生代 Survivor 区
        // ---------------------------------------------------------------------------------
        System.out.println("\n---------------- 场景 2：少量存活对象进入 Survivor 区缓冲 ----------------");
        System.out.println("-> 分配 1.5MB 长期存活对象（常驻列表中），Survivor (To) 区足以容纳...");
        List<byte[]> survivorList = new ArrayList<>();
        survivorList.add(new byte[_512KB]);
        survivorList.add(new byte[_512KB]);
        survivorList.add(new byte[_512KB]);

        // 触发一次垃圾收集，将 Eden 中有引用的对象复制到 Survivor
        System.gc();
        Thread.sleep(100);
        printMemoryPools("【场景 2: 存活对象进入 Survivor 之后】");

        // ---------------------------------------------------------------------------------
        // 场景 3：存活对象大小超过 To 区容量，触发【分配担保】，直接晋升到老年代
        // ---------------------------------------------------------------------------------
        System.out.println("\n---------------- 场景 3：突发大批存活对象超过 Survivor 承载，触发分配担保 ----------------");
        System.out.println("-> 【核心原理验证】：");
        System.out.println("   此时持续生成大批量强引用对象（如 15MB 缓存）；");
        System.out.println("   由于 Survivor 空间（通常仅几 MB）根本放不下，JVM 在搬迁时直接一步到位将它们晋升到老年代！");
        List<byte[]> promotionList = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            promotionList.add(new byte[_1MB]);
        }

        System.gc(); // 强制触发 GC，观察老年代（Old / Tenured）激增
        Thread.sleep(100);
        printMemoryPools("【场景 3: 触发分配担保直接晋升老年代之后】");

        // ---------------------------------------------------------------------------------
        // 场景 4：超大对象分配（单次占用巨大），直接避开 Eden，直入老年代
        // ---------------------------------------------------------------------------------
        System.out.println("\n---------------- 场景 4：单次大对象创建，绕过新生代直入老年代 ----------------");
        System.out.println("-> 分配单个 10MB 连续大数组：");
        System.out.println("   若 Eden 装不下或超过阈值，JVM 避免在新生代倒手拷贝，直接在老年代开辟空间！");
        byte[] hugeArray = new byte[10 * _1MB];
        printMemoryPools("【场景 4: 产生单个大对象后】");

        // 保持引用防止被 JIT 优化消除
        int totalHash = survivorList.size() + promotionList.size() + hugeArray.length;
        System.out.println("\n=====================================================================");
        System.out.println("   演练总结 (Hash: " + totalHash + ")：");
        System.out.println("   1. Eden 负责承担 98% 短命对象的快速产生与推平；");
        System.out.println("   2. Survivor (From/To) 作为 10% 的缓冲堤坝，过滤微量幸存者；");
        System.out.println("   3. 老年代作为终极水库，通过【分配担保机制】承接一切 Survivor 装不下的对象！");
        System.out.println("=====================================================================");
    }

    /**
     * 格式化打印当前各个内存池（Eden、Survivor、Old/Tenured）的实时水位
     */
    private static void printMemoryPools(String stageName) {
        System.out.println("\n" + stageName);
        System.out.printf("%-26s | %-12s | %-12s | %-8s%n", "Memory Pool Name", "Used (MB)", "Max (MB)", "Usage");
        System.out.println("---------------------------------------------------------------------");

        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if (pool.getType() == MemoryType.HEAP) {
                String name = pool.getName();
                long used = pool.getUsage().getUsed() / _1MB;
                long max = pool.getUsage().getMax() / _1MB;
                String usagePct = max > 0 ? String.format("%.1f%%", (used * 100.0) / max) : "N/A";

                // 美化常见名称标注
                String label = name;
                if (name.contains("Eden")) {
                    label += " (新生代 Eden)";
                } else if (name.contains("Survivor")) {
                    label += " (幸存区 S0/S1)";
                } else if (name.contains("Old") || name.contains("Tenured")) {
                    label += " (老年代 Old Gen)";
                }

                System.out.printf("%-26s | %10d MB | %10d MB | %8s%n", label, used, max, usagePct);
            }
        }
    }
}
