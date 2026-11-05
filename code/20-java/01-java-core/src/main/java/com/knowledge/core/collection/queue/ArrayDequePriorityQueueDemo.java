package com.knowledge.core.collection.queue;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.PriorityQueue;

/**
 * 队列高级实战：
 * 1. ArrayDeque 基于循环数组的无锁双端队列，对比 LinkedList 做栈/队列时零对象分配开销
 * 2. PriorityQueue 基于二叉小顶堆的高性能海量数据 Top-K 求解器（时空复杂度极佳）
 */
public class ArrayDequePriorityQueueDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. ArrayDeque 循环数组双端队列与高性能栈操作 ==========");
        demoArrayDeque();

        System.out.println("\n========== 2. PriorityQueue 二叉小顶堆求解 Top-K 算法 ==========");
        demoTopKPriorityQueue();
    }

    private static void demoArrayDeque() {
        // 作为双端队列
        Deque<String> deque = new ArrayDeque<>();
        deque.addFirst("Task-1");
        deque.addLast("Task-2");
        deque.addFirst("Urgent-Task-0");

        System.out.println("双端入队后: " + deque);
        System.out.println("头部出队 (pollFirst): " + deque.pollFirst());
        System.out.println("尾部出队 (pollLast) : " + deque.pollLast());

        // 作为高性能栈（替代遗留废弃的 Vector 子类 java.util.Stack）
        Deque<String> stack = new ArrayDeque<>();
        stack.push("Page 1");
        stack.push("Page 2");
        stack.push("Page 3");
        System.out.println("栈弹顶 (pop): " + stack.pop() + "，剩余: " + stack);
        System.out.println(">>> 性能分析: ArrayDeque 内部是循环数组 (head, tail 指针与 elements.length - 1 做位与运算)，");
        System.out.println(">>> 无论入栈出栈均无需创建 Node 对象，无锁高性能，是 Java 官方首推的栈/双端队列实现！");
    }

    private static void demoTopKPriorityQueue() {
        int[] streamData = {45, 12, 85, 32, 89, 39, 69, 44, 42, 1, 99, 105, 78, 62};
        int k = 4;
        System.out.println("输入海量数据流: " + Arrays.toString(streamData));
        System.out.println("目标：求最大的前 " + k + " 个数 (Top-K)");

        // 维护一个容量为 K 的小顶堆 (默认自然升序，堆顶为当前 K 个数中的最小值)
        PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);

        for (int num : streamData) {
            if (minHeap.size() < k) {
                minHeap.offer(num);
            } else if (num > minHeap.peek()) {
                // 如果当前数大于小顶堆堆顶，弹出堆顶并插入当前数 (O(log K))
                minHeap.poll();
                minHeap.offer(num);
            }
        }

        System.out.println("Top-" + k + " 结果 (小顶堆内部): " + minHeap);
        System.out.print("按从大到小依次输出: ");
        // 取出并逆序输出
        Integer[] topK = minHeap.toArray(new Integer[0]);
        Arrays.sort(topK, (a, b) -> b - a);
        System.out.println(Arrays.toString(topK));
        System.out.println(">>> 复杂度分析: 全量排序需 O(N log N)，使用二叉小顶堆流式处理只需 O(N log K)，空间仅占用 O(K)！");
    }
}
