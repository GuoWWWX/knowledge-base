package com.knowledge.jvm.memory;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

/**
 * 64 位 HotSpot JVM 对象内存布局深度剖析（基于 OpenJDK JOL）
 * <p>
 * 实验重点：
 * 1. 打印 JVM 当前运行环境（压缩指针 Compressed Oops / Compressed Class Pointers 状态）。
 * 2. 空对象 new Object() 内存布局：MarkWord (8B) + Klass Pointer (4B/8B) + 对齐填充 (Padding)。
 * 3. 复合普通对象布局：字段重排序（Field Reordering）与内存对齐补齐机制。
 * 4. 数组对象特殊布局：MarkWord (8B) + Klass Pointer (4B) + 数组长度 (4B) = 16B 头部。
 * 5. synchronized 锁升级对 MarkWord 内部状态标志位的物理修改透视。
 * </p>
 *
 * @author Knowledge Base
 */
public class ObjectMemoryLayoutDemo {

    /**
     * 测试普通复合对象
     */
    static class SampleEntity {
        // 8 字节
        private long id = 10001L;
        // 4 字节
        private int age = 28;
        // 1 字节
        private boolean active = true;
        // 1 字节
        private byte rank = 1;
        // 4 字节（开启指针压缩时为 4 字节，关闭为 8 字节）
        private String name = "Alice";
    }

    public static void main(String[] args) {
        printHeader("JVM 宿主环境与指针压缩状态");
        System.out.println(VM.current().details());

        printHeader("实验 1：空对象 new Object() 的 64 位内存布局");
        demonstrateEmptyObject();

        printHeader("实验 2：复合普通对象（SampleEntity）的字段排列与内存重排序");
        demonstrateSampleEntity();

        printHeader("实验 3：基本类型数组（int[]）的 16 字节头部布局解析");
        demonstrateArrayLayout();

        printHeader("实验 4：synchronized 锁升级对 MarkWord 的物理状态修改透视");
        demonstrateLockStateEvolution();

        printHeader("实验 5：64 位 HotSpot JVM MarkWord 结构速查表");
        printMarkWordSummary();
    }

    private static void demonstrateEmptyObject() {
        Object obj = new Object();
        System.out.println("new Object() 物理布局：");
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());

        System.out.println("【空对象布局深度剖析】：");
        System.out.println(" 1. MarkWord（标记字）：占 8 字节（64-bit），存储 HashCode、GC 分代年龄、锁标志位等信息。");
        System.out.println(" 2. Klass Pointer（元数据类型指针）：在开启 +UseCompressedClassPointers 默认配置下占 4 字节，指向方法区的类元数据。");
        System.out.println(" 3. 实例数据（Instance Data）：空对象没有任何属性，占 0 字节。");
        System.out.println(" 4. 对齐填充（Padding）：8 + 4 = 12 字节，HotSpot 要求对象占用空间必须是 8 字节的整数倍，因此填充 4 字节至 16 字节。");
    }

    private static void demonstrateSampleEntity() {
        SampleEntity entity = new SampleEntity();
        System.out.println("new SampleEntity() 物理布局：");
        System.out.println(ClassLayout.parseInstance(entity).toPrintable());

        System.out.println("【字段重排序规律（Field Reordering）】：");
        System.out.println(" 1. JVM 不会完全按照 Java 源码声明的顺序布局字段，而是按照：long/double(8B) -> int/float(4B) -> short/char(2B) -> byte/boolean(1B) -> 引用类型(oops)。");
        System.out.println(" 2. 目的：为了满足 CPU 缓存行对齐要求，消除内存访问时的不对齐开销，最大化空间压缩效率。");
    }

    private static void demonstrateArrayLayout() {
        int[] intArray = new int[]{10, 20, 30, 40, 50};
        System.out.println("new int[5] 数组对象物理布局：");
        System.out.println(ClassLayout.parseInstance(intArray).toPrintable());

        System.out.println("【数组对象特殊头部】：");
        System.out.println(" 1. 普通对象的头部是 12 字节（MarkWord 8B + Klass 4B）。");
        System.out.println(" 2. 数组对象头部额外增加了 4 字节的 Array Length（记录数组长度），总计 16 字节头部！");
        System.out.println(" 3. 元素数据：5 个 int 占用 5 * 4 = 20 字节。");
        System.out.println(" 4. 总占用：16B(Header) + 20B(Data) = 36B，填充 4 字节 padding，最终占用 40 字节！");
    }

    private static void demonstrateLockStateEvolution() {
        Object lockObj = new Object();

        System.out.println("1. 加锁前（无锁状态 / 无 HashCode 计算）：");
        System.out.println(ClassLayout.parseInstance(lockObj).toPrintable());

        // 计算一次 identityHashCode，观察 MarkWord 中 HashCode 注入
        int hashCode = System.identityHashCode(lockObj);
        System.out.printf("-> 调用 System.identityHashCode() = 0x%s 之后：%n", Integer.toHexString(hashCode));
        System.out.println(ClassLayout.parseInstance(lockObj).toPrintable());

        // 进入重量级/轻量级同步块
        synchronized (lockObj) {
            System.out.println("2. synchronized(lockObj) 代码块内部（加锁状态）：");
            System.out.println(ClassLayout.parseInstance(lockObj).toPrintable());
        }

        System.out.println("3. 退出 synchronized 代码块之后（锁释放状态）：");
        System.out.println(ClassLayout.parseInstance(lockObj).toPrintable());

        System.out.println("【锁状态演进小结】：");
        System.out.println(" 观察第一行 MarkWord 的十六进制值变化，低 2 位标志位在锁竞争前后从 01（无锁）变化为 00（轻量级锁栈指针）或 10（重量级监视器 ObjectMonitor 指针）。");
    }

    private static void printMarkWordSummary() {
        System.out.println("┌─────────────┬─────────────────────────────────────────────────┬──────────┬────────┐");
        System.out.println("│  锁状态     │              64 位 MarkWord 布局 (高位 -> 低位) │ 偏向锁位 │ 锁标志 │");
        System.out.println("├─────────────┼─────────────────────────────────────────────────┼──────────┼────────┤");
        System.out.println("│ 无锁 (Normal)│ unused(25bit) | hashCode(31bit) | age(4bit)     │    0     │   01   │");
        System.out.println("│ 偏向锁(Biased)│ threadId(54bit)| epoch(2bit)    | age(4bit)     │    1     │   01   │");
        System.out.println("│ 轻量级锁(Light)│ 指向当前线程栈中 Lock Record 的指针 (62bit)      │   (无)   │   00   │");
        System.out.println("│ 重量级锁(Heavy)│ 指向互斥监视器 ObjectMonitor 的指针 (62bit)      │   (无)   │   10   │");
        System.out.println("│ GC 标记(Marked)│ CMS/G1/ZGC 垃圾回收标记 (62bit)                 │   (无)   │   11   │");
        System.out.println("└─────────────┴─────────────────────────────────────────────────┴──────────┴────────┘");
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println(" " + title);
        System.out.println("=".repeat(100));
    }
}
