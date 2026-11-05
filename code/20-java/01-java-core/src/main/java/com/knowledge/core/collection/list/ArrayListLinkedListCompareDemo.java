package com.knowledge.core.collection.list;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * List 核心实现深度剖析：
 * 1. ArrayList 1.5 倍扩容机制反射探测 (oldCapacity + (oldCapacity >> 1))
 * 2. 尾插法、头插法、随机访问性能硬核压测
 * 3. CPU Cache Line 友好度与 LinkedList 缓存颠簸 (Cache Miss) 根因分析
 */
public class ArrayListLinkedListCompareDemo {

    private static final int OP_COUNT = 100_000;

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. ArrayList 1.5 倍扩容阶梯反射追踪 ==========");
        inspectArrayListGrowth();

        System.out.println("\n========== 2. 尾部追加压测 (100,000 次) ==========");
        benchmarkTailAppend();

        System.out.println("\n========== 3. 随机下标访问压测 (100,000 次) ==========");
        benchmarkRandomAccess();

        System.out.println("\n========== 4. 头部插入对比 (10,000 次) ==========");
        benchmarkHeadInsert();
    }

    private static void inspectArrayListGrowth() {
        ArrayList<Integer> list = new ArrayList<>();
        try {
            Field elementDataField = ArrayList.class.getDeclaredField("elementData");
            elementDataField.setAccessible(true);

            System.out.println("初始状态: size=0, capacity=" + ((Object[]) elementDataField.get(list)).length);
            
            int lastCapacity = 0;
            for (int i = 0; i < 50; i++) {
                list.add(i);
                Object[] elementData = (Object[]) elementDataField.get(list);
                int currentCapacity = elementData.length;
                if (currentCapacity != lastCapacity) {
                    System.out.printf("触发扩容 -> 当前 size=%2d, 底层数组容量 capacity: %2d -> %2d (增长比例: %.1f倍)%n",
                            list.size(), lastCapacity, currentCapacity,
                            lastCapacity == 0 ? 0 : (double) currentCapacity / lastCapacity);
                    lastCapacity = currentCapacity;
                }
            }
        } catch (Throwable e) {
            System.out.println("【Java 21 模块强封装保护】: 正在按 JDK 官方源码公式推导 1.5 倍扩容阶梯：");
            System.out.println("  -> 初始无参 new ArrayList<>() 默认持有空数组 DEFAULTCAPACITY_EMPTY_ELEMENTDATA;");
            System.out.println("  -> 首次 add(e) 触发扩容至 DEFAULT_CAPACITY = 10;");
            System.out.println("  -> 后续阶梯扩容公式: newCapacity = oldCapacity + (oldCapacity >> 1)");
            int[] steps = {10, 15, 22, 33, 49, 73, 109, 163};
            for (int i = 0; i < steps.length - 1; i++) {
                System.out.printf("  -> 扩容触发: %3d -> %3d (约 1.5 倍)%n", steps[i], steps[i + 1]);
            }
            System.out.println(">>> [提示] 若想在 IDEA 中物理反射 JDK 内部私有数组，可在 Run Configuration 的 VM options 中添加: --add-opens java.base/java.util=ALL-UNNAMED");
        }
        System.out.println(">>> 扩容公式: newCapacity = oldCapacity + (oldCapacity >> 1)，即 10 -> 15 -> 22 -> 33 -> 49");
    }

    private static void benchmarkTailAppend() {
        // ArrayList 预估容量
        long t1 = System.nanoTime();
        List<Integer> arrayListWithCap = new ArrayList<>(OP_COUNT);
        for (int i = 0; i < OP_COUNT; i++) {
            arrayListWithCap.add(i);
        }
        long t2 = System.nanoTime();

        // ArrayList 默认无初始容量扩容
        long t3 = System.nanoTime();
        List<Integer> arrayListDefault = new ArrayList<>();
        for (int i = 0; i < OP_COUNT; i++) {
            arrayListDefault.add(i);
        }
        long t4 = System.nanoTime();

        // LinkedList 双向链表尾插
        long t5 = System.nanoTime();
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < OP_COUNT; i++) {
            linkedList.add(i);
        }
        long t6 = System.nanoTime();

        System.out.printf("ArrayList (预分配容量): %8.2f ms%n", (t2 - t1) / 1_000_000.0);
        System.out.printf("ArrayList (默认扩容)  : %8.2f ms%n", (t4 - t3) / 1_000_000.0);
        System.out.printf("LinkedList (频繁创建Node): %8.2f ms%n", (t6 - t5) / 1_000_000.0);
        System.out.println(">>> 分析: LinkedList 每次添加都要分配 Node 对象 (24字节开销)，堆内存碎片严重，性能反而不及 ArrayList。");
    }

    private static void benchmarkRandomAccess() {
        ArrayList<Integer> arrayList = new ArrayList<>(OP_COUNT);
        LinkedList<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < OP_COUNT; i++) {
            arrayList.add(i);
            linkedList.add(i);
        }

        Random random = new Random(42);
        int[] indices = new int[OP_COUNT];
        for (int i = 0; i < OP_COUNT; i++) {
            indices[i] = random.nextInt(OP_COUNT);
        }

        // ArrayList 随机访问 (O(1))
        long t1 = System.nanoTime();
        long sum1 = 0;
        for (int idx : indices) {
            sum1 += arrayList.get(idx);
        }
        long t2 = System.nanoTime();

        // LinkedList 随机访问 (O(N) 遍历) - 只测试前 5000 次避免卡死
        int testCount = 5_000;
        long t3 = System.nanoTime();
        long sum2 = 0;
        for (int i = 0; i < testCount; i++) {
            sum2 += linkedList.get(indices[i]);
        }
        long t4 = System.nanoTime();

        System.out.printf("ArrayList  100,000 次随机 get(i): %8.2f ms (连续物理地址，CPU L1/L2 缓存行预取加速)%n", (t2 - t1) / 1_000_000.0);
        System.out.printf("LinkedList   5,000 次随机 get(i): %8.2f ms (等价推算 100,000 次约需 %.0f ms)%n",
                (t4 - t3) / 1_000_000.0, ((t4 - t3) / 1_000_000.0) * (OP_COUNT / testCount));
        System.out.println(">>> 结论: ArrayList 基于连续数组，天然 Cache Line 友好；LinkedList 遍历指针跳跃引发严重 Cache Miss！");
    }

    private static void benchmarkHeadInsert() {
        int count = 10_000;

        long t1 = System.nanoTime();
        List<Integer> arrayList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            arrayList.add(0, i); // System.arraycopy 整体后移
        }
        long t2 = System.nanoTime();

        long t3 = System.nanoTime();
        List<Integer> linkedList = new LinkedList<>();
        for (int i = 0; i < count; i++) {
            linkedList.add(0, i); // 头插只需修改指针
        }
        long t4 = System.nanoTime();

        System.out.printf("ArrayList  10,000 次头插 (add(0, e)): %8.2f ms (数组大量内存搬迁)%n", (t2 - t1) / 1_000_000.0);
        System.out.printf("LinkedList 10,000 次头插 (add(0, e)): %8.2f ms (修改 head 指针仅 O(1))%n", (t4 - t3) / 1_000_000.0);
    }
}
