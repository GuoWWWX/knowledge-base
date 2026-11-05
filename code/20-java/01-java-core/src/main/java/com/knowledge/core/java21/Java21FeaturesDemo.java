package com.knowledge.core.java21;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.SequencedCollection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java 21 LTS 核心划时代特性深度实战
 * <p>
 * 实验重点：
 * 1. 虚拟线程（Virtual Threads - JEP 444）：10,000 级高并发 I/O 压测，剖析虚拟线程与 Carrier 载体线程的映射。
 * 2. Record 模式与 switch 模式匹配（Record Patterns - JEP 440 & JEP 441）：解构、when 守卫与密封接口的穷举性。
 * 3. 序列化集合（Sequenced Collections - JEP 431）：统一的头尾访问与逆序视图 API。
 * </p>
 *
 * @author Knowledge Base
 */
public class Java21FeaturesDemo {

    public static void main(String[] args) throws InterruptedException {
        printHeader("特性 1：Virtual Threads（虚拟线程）10,000 并发 I/O 阻塞压测与载体线程透视");
        demonstrateVirtualThreads();

        printHeader("特性 2：Record 模式（Record Patterns）与 switch 模式匹配（Pattern Matching for switch）");
        demonstrateRecordAndSwitchPatterns();

        printHeader("特性 3：Sequenced Collections（序列化集合）统一访问规范");
        demonstrateSequencedCollections();
    }

    /**
     * 1. 虚拟线程高并发压测
     */
    private static void demonstrateVirtualThreads() {
        final int TASK_COUNT = 10_000;
        final int SIMULATED_IO_BLOCK_MS = 100;

        System.out.println("-> 准备启动 " + TASK_COUNT + " 个虚拟线程，每个线程模拟 I/O 阻塞 " + SIMULATED_IO_BLOCK_MS + " ms...");

        AtomicInteger completedCount = new AtomicInteger(0);
        Set<String> carrierThreads = ConcurrentHashMap.newKeySet();

        Instant start = Instant.now();

        // 使用 try-with-resources 自动管理 ExecutorService，它会隐式调用 shutdown() 并阻塞等待所有虚拟线程执行完毕
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= TASK_COUNT; i++) {
                final int taskId = i;
                executor.submit(() -> {
                    try {
                        // 记录前几个任务的虚拟线程与其底层的 Carrier（载体）平台线程名称
                        if (taskId <= 5) {
                            String threadInfo = Thread.currentThread().toString();
                            System.out.printf("   [任务 #%05d] 当前线程对象: %s%n", taskId, threadInfo);
                        }

                        // 获取载体平台线程名称（通常形如 ForkJoinPool-1-worker-x）
                        String threadStr = Thread.currentThread().toString();
                        if (threadStr.contains("@ForkJoinPool")) {
                            carrierThreads.add(threadStr.substring(threadStr.indexOf("@") + 1));
                        }

                        // 模拟高延迟的阻塞 I/O（在虚拟线程中 sleep 不会阻塞物理操作系统线程，而是触发 Continuation.yield() 脱卸）
                        Thread.sleep(Duration.ofMillis(SIMULATED_IO_BLOCK_MS));
                        completedCount.incrementAndGet();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }
        } // 自动等待所有 10000 个任务彻底完成

        Duration elapsed = Duration.between(start, Instant.now());
        System.out.printf("%n【压测完成】成功执行 %d 个虚拟线程任务，总耗时: %d 毫秒！%n", completedCount.get(), elapsed.toMillis());
        System.out.printf(" 底层实际借调的 Carrier 物理线程池采样数: 约 %d 个工作线程（远小于 10,000）%n",
                Math.max(carrierThreads.size(), Runtime.getRuntime().availableProcessors()));
        System.out.println("【核心机制解析】：");
        System.out.println(" 1. 虚拟线程是运行在 JVM 用户态的高效轻量级线程，每个仅占用几百字节内存（传统平台线程约占 1MB 栈空间）。");
        System.out.println(" 2. 当虚拟线程执行阻塞 I/O（如网络调用、sleep、JDBC 阻塞）时，JVM 会自动将该虚拟线程从 Carrier 载体线程上卸载（Unmount），");
        System.out.println("    让 Carrier 线程立即去承载其他虚拟线程执行；I/O 就绪后，再重新装载（Mount）到任一可用的 Carrier 线程继续执行。");
        System.out.println(" 3. 彻底终结了为了高吞吐被迫编写反直觉的 Reactive 响应式异步代码的历史，回归极简直观的 同步阻塞编写、异步非阻塞运行 心智！");
    }

    /**
     * 2. Record 模式与 switch 模式匹配
     */
    sealed interface Graphic permits Circle, Rectangle, Point, CompoundShape {}

    record Point(int x, int y) implements Graphic {}
    record Circle(Point center, double radius) implements Graphic {}
    record Rectangle(double width, double height) implements Graphic {}
    record CompoundShape(Graphic first, Graphic second) implements Graphic {}

    private static void demonstrateRecordAndSwitchPatterns() {
        List<Graphic> testShapes = new ArrayList<>();
        testShapes.add(new Circle(new Point(0, 0), 5.0));
        testShapes.add(new Rectangle(10.0, 10.0)); // 正方形特例
        testShapes.add(new Rectangle(20.0, 5.0));
        testShapes.add(new Point(3, 4));
        testShapes.add(new CompoundShape(new Point(1, 1), new Circle(new Point(2, 2), 3.0)));
        testShapes.add(null); // 测试 null 安全分支

        for (Graphic shape : testShapes) {
            String evaluation = describeGraphic(shape);
            System.out.println("-> 图形判定: " + evaluation);
        }

        System.out.println("\n【Java 21 模式匹配新特性亮点】：");
        System.out.println(" 1. 解构匹配（Record Patterns）：支持直接在 case 中对 Record 组件解包，如 case Circle(Point(int x, int y), double r)。");
        System.out.println(" 2. 守卫表达式（when clause）：可以在模式后追加精确的布尔条件判断，如 case Rectangle(double w, double h) when w == h。");
        System.out.println(" 3. 空安全分支（case null）：彻底告别传统的 switch(null) 抛出 NullPointerException 的隐患。");
        System.out.println(" 4. 穷举性校验（Exhaustiveness）：结合 sealed interface，编译器自动检查所有子类型是否全覆盖，无需冗余 default 兜底。");
    }

    /**
     * 使用 Java 21 增强 switch 模式匹配与嵌套解构
     */
    private static String describeGraphic(Graphic graphic) {
        return switch (graphic) {
            case null -> "【空指针防御】检测到 null 对象，安全优雅处理";
            // 嵌套解构：同时解构 Circle 及其内部的 Point center 坐标
            case Circle(Point(int x, int y), double r) ->
                    String.format("圆形 [圆心: (%d, %d), 半径: %.1f, 面积: %.2f]", x, y, r, Math.PI * r * r);
            // 带有 when 守卫条件的特定模式分支
            case Rectangle(double w, double h) when w == h ->
                    String.format("正方形（长宽相等） [边长: %.1f, 面积: %.2f]", w, w * h);
            case Rectangle(double w, double h) ->
                    String.format("常规矩形 [宽: %.1f, 高: %.1f, 面积: %.2f]", w, h, w * h);
            case Point(int x, int y) ->
                    String.format("孤立坐标点 [X: %d, Y: %d]", x, y);
            case CompoundShape(Graphic g1, Graphic g2) ->
                    String.format("组合图形包含: [%s] 与 [%s]", g1.getClass().getSimpleName(), g2.getClass().getSimpleName());
        };
    }

    /**
     * 3. Sequenced Collections（序列化集合）
     */
    private static void demonstrateSequencedCollections() {
        System.out.println("-> 测试 SequencedCollection 在有序集合中的一致性接口：");

        SequencedCollection<String> seqList = new ArrayList<>(List.of("First", "Middle", "Last"));
        System.out.println(" 初始 List: " + seqList);
        System.out.println("   getFirst(): " + seqList.getFirst());
        System.out.println("   getLast() : " + seqList.getLast());
        System.out.println("   reversed(): " + seqList.reversed());

        SequencedCollection<String> seqSet = new LinkedHashSet<>();
        seqSet.add("Node-1");
        seqSet.add("Node-2");
        seqSet.add("Node-3");
        System.out.println(" 初始 LinkedHashSet: " + seqSet);
        System.out.println("   getFirst(): " + seqSet.getFirst());
        System.out.println("   getLast() : " + seqSet.getLast());
        System.out.println("   reversed(): " + seqSet.reversed());

        System.out.println("\n【历史痛点终结】：过去 List、Deque、SortedSet、LinkedHashSet 各自维护获取首尾元素的异构方法");
        System.out.println("（如 list.get(0), deque.peekFirst(), sortedSet.first()），Java 21 通过 SequencedCollection 统一了全局契约！");
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println(" " + title);
        System.out.println("=".repeat(100));
    }
}
