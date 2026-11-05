package com.knowledge.core.collection;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

/**
 * 集合安全删除机制深度剖析
 * <p>
 * 实验内容：
 * 1. 传统 for-each 循环中调用 list.remove() 触发 ConcurrentModificationException 的本质原因。
 * 2. 诡异边界情况：为什么删除“倒数第二个元素”可能不抛异常却导致逻辑漏洞？
 * 3. Iterator.remove() 的内部机制：如何同步 expectedModCount 与 modCount。
 * 4. List.removeIf() (Java 8+) 的底层原理：基于位图批量标记与 System.arraycopy 单次压缩，实现 O(N) 性能跃升。
 * 5. 大数据量（10万级）下的 iterator.remove() vs list.removeIf() 性能压测对比。
 * </p>
 *
 * @author Knowledge Base
 */
public class CollectionSafeRemoveDemo {

    public static void main(String[] args) {
        printHeader("实验 1：传统 for-each 中 list.remove() 抛出 ConcurrentModificationException 验证");
        demonstrateForEachException();

        printHeader("实验 2：经典面试陷阱——为什么删除倒数第二个元素偶尔不会抛异常？");
        demonstratePenultimateElementTrap();

        printHeader("实验 3：标准姿势——使用 Iterator.remove() 安全删除");
        demonstrateIteratorSafeRemove();

        printHeader("实验 4：现代推荐姿势——使用 List.removeIf() 声明式极速删除");
        demonstrateListRemoveIf();

        printHeader("实验 5：十万量级性能对比：Iterator.remove() vs List.removeIf()");
        benchmarkRemovalPerformance();
    }

    /**
     * 场景一：for-each 循环中调用 list.remove()
     * 原理：for-each 是语法糖，底层依赖 Iterator。
     * ArrayList.Itr 维护了 cursor(下一个元素游标)、lastRet(上一个元素索引) 和 expectedModCount(创建时的修改次数)。
     * 调用 list.remove() 会导致 list 的 modCount++，但 Itr 的 expectedModCount 并未同步更新。
     * 下一次循环进入 it.next() 时，执行 checkForComodification():
     *    if (modCount != expectedModCount) throw new ConcurrentModificationException();
     */
    private static void demonstrateForEachException() {
        List<String> list = new ArrayList<>(List.of("Java", "Go", "Python", "Rust"));
        System.out.println("初始列表: " + list);
        try {
            for (String item : list) {
                if ("Go".equals(item)) {
                    System.out.println("-> 尝试在 for-each 中调用 list.remove(\"Go\")...");
                    list.remove(item);
                }
            }
            System.out.println("意外成功（未抛异常）");
        } catch (ConcurrentModificationException e) {
            System.out.println("【成功捕获异常】" + e.getClass().getName());
            System.out.println("【底层原理剖析】：");
            System.out.println(" 1. for-each 底层由 Iterator 驱动，初始化时记录 expectedModCount = modCount = 4。");
            System.out.println(" 2. 当检测到 item == 'Go' 时调用 list.remove()，ArrayList 内部 modCount++ 变为 5，数组元素移动，size 变为 3。");
            System.out.println(" 3. 下一次循环迭代进入 Iterator.next()，首先执行 checkForComodification()。");
            System.out.println(" 4. 判定 modCount(5) != expectedModCount(4)，立刻触发 Fail-Fast 机制，抛出 ConcurrentModificationException。");
        }
    }

    /**
     * 场景二：删除倒数第二个元素的隐蔽 Bug
     * 假设列表有 3 个元素 ["A", "B", "C"]，在 for-each 中删除 "B"（倒数第二个）。
     * 当遍历到 "B" 时，cursor 已经指向 2；执行 remove("B") 后，列表 size 变为 2。
     * 下一次循环条件判断 it.hasNext() 时，判定依据是 `cursor != size`。
     * 此时 cursor == 2 且 size == 2，`cursor != size` 返回 false，循环直接意外提前终止！
     * 因此根本没有机会执行 it.next() 中的 checkForComodification()，导致不仅不抛异常，且最后一个元素被静默漏掉！
     */
    private static void demonstratePenultimateElementTrap() {
        List<String> list = new ArrayList<>(List.of("A", "B", "C"));
        System.out.println("初始列表（3个元素）: " + list);
        System.out.println("试图删除倒数第2个元素 'B'...");

        try {
            for (String item : list) {
                if ("B".equals(item)) {
                    list.remove(item);
                    System.out.println("  已调用 list.remove('B')，当前列表状态: " + list);
                }
            }
            System.out.println("【执行结果】神奇地没有抛出 ConcurrentModificationException！");
            System.out.println("【隐藏危害与陷阱解析】：");
            System.out.println(" 1. 元素 'C' 根本没有被遍历到（循环提前假死终止）。");
            System.out.println(" 2. 原因：删除 'B' 后 size 从 3 缩减为 2，此时 cursor 恰好为 2。");
            System.out.println(" 3. hasNext() 判断 `cursor != size` 结果为 false（2 != 2 为假），直接跳出循环！");
            System.out.println(" 4. 结论：此乃极危险的隐式逻辑缺陷，切忌因为偶发不报错而心存侥幸！");
        } catch (ConcurrentModificationException e) {
            System.out.println("捕获到了异常: " + e);
        }
    }

    /**
     * 场景三：使用 Iterator.remove() 安全删除
     * 原理：
     * Iterator 的 remove() 方法在执行删除后：
     * 1. 调用 ArrayList.this.remove(lastRet)；
     * 2. cursor = lastRet（重置游标防止越位）；
     * 3. expectedModCount = modCount（显式同步内部版本号）！
     * 因此下一次调用 next() 时 `modCount == expectedModCount` 校验通过。
     */
    private static void demonstrateIteratorSafeRemove() {
        List<String> list = new ArrayList<>(List.of("Spring", "MyBatis", "Redis", "Kafka"));
        System.out.println("初始列表: " + list);

        Iterator<String> it = list.iterator();
        while (it.hasNext()) {
            String item = it.next();
            if ("Redis".equals(item)) {
                System.out.println("-> 命中目标，通过 iterator.remove() 移除: " + item);
                it.remove();
            }
        }
        System.out.println("删除后列表: " + list);
        System.out.println("【安全原理】：Iterator.remove() 内部在执行宿主列表删除后，将 expectedModCount = modCount 同步，保证版本一致。");
    }

    /**
     * 场景四：使用 List.removeIf() (Java 8+) 声明式删除
     * 原理：
     * ArrayList.removeIf() 重写了底层实现：
     * 1. 使用 BitSet 标记所有满足谓词条件的元素下标（单次扫描，耗时 O(N)）。
     * 2. 如果无匹配项，直接返回 false，无需任何多余数组复制。
     * 3. 若有匹配项，单次遍历压缩数组：将未标记保留的元素前移覆盖，避免了普通迭代器每次删除引发的多次 System.arraycopy。
     * 4. 末尾统一置 null 协助 GC，一次性更新 size 与 modCount。
     */
    private static void demonstrateListRemoveIf() {
        List<String> list = new ArrayList<>(List.of("Apple", "Banana", "Orange", "Avocado", "Blueberry"));
        System.out.println("初始水果列表: " + list);

        // 删除所有以 'A' 或 'B' 开头的水果
        boolean changed = list.removeIf(item -> item.startsWith("A") || item.startsWith("B"));

        System.out.println("执行 list.removeIf(s -> s.startsWith('A') || s.startsWith('B')) 结果: " + changed);
        System.out.println("删除后水果列表: " + list);
        System.out.println("【底层原理】：内部基于 BitSet 批量位标记 + 单趟数组元素前移压缩，避免多次 System.arraycopy，效率极高！");
    }

    /**
     * 场景五：十万量级性能对比测试
     * 对比当数据量较大且删除大量元素时，Iterator.remove() 与 List.removeIf() 的耗时差异。
     */
    private static void benchmarkRemovalPerformance() {
        final int ELEMENT_COUNT = 100_000;
        System.out.println("准备初始化两个大小为 " + ELEMENT_COUNT + " 的 ArrayList 并删除所有偶数...");

        // 构造 Iterator 待测集合
        List<Integer> listForIterator = new ArrayList<>(ELEMENT_COUNT);
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            listForIterator.add(i);
        }

        // 构造 removeIf 待测集合
        List<Integer> listForRemoveIf = new ArrayList<>(ELEMENT_COUNT);
        for (int i = 0; i < ELEMENT_COUNT; i++) {
            listForRemoveIf.add(i);
        }

        // 1. 测试 Iterator.remove()
        long start1 = System.nanoTime();
        Iterator<Integer> it = listForIterator.iterator();
        while (it.hasNext()) {
            if (it.next() % 2 == 0) {
                it.remove();
            }
        }
        long durationIteratorMs = (System.nanoTime() - start1) / 1_000_000;
        System.out.printf("  [方式一] Iterator.remove() 耗时: %d ms (剩余大小: %d)%n", durationIteratorMs, listForIterator.size());

        // 2. 测试 List.removeIf()
        long start2 = System.nanoTime();
        listForRemoveIf.removeIf(num -> num % 2 == 0);
        long durationRemoveIfMs = (System.nanoTime() - start2) / 1_000_000;
        System.out.printf("  [方式二] List.removeIf()    耗时: %d ms (剩余大小: %d)%n", durationRemoveIfMs, listForRemoveIf.size());

        System.out.println("\n【性能分析结论】：");
        System.out.println(" 1. Iterator.remove() 每删除一个元素，底层 ArrayList 都会调用 System.arraycopy 将后续所有元素前移一位。");
        System.out.printf("    总复制操作次数为 O(N^2)，在此 10 万级数据量下性能急剧劣化 (耗时约 %d ms)。%n", durationIteratorMs);
        System.out.println(" 2. List.removeIf() 采用两阶段算法：第一阶段用 BitSet 批量标记（时间 O(N)），第二阶段单趟紧凑移动非删除元素（单次 O(N)）。");
        System.out.printf("    总时间复杂度严格为 O(N)，在此 10 万级数据量下仅需约 %d ms，性能提升数十乃至上百倍！%n", durationRemoveIfMs);
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("=".repeat(80));
        System.out.println(" " + title);
        System.out.println("=".repeat(80));
    }
}
