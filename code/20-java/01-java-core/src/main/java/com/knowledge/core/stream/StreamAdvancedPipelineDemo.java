package com.knowledge.core.stream;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Stream 流式处理高级实战：
 * 1. 垂直流水线与惰性求值 (Lazy Evaluation) & 短路 (Short-Circuit) 性能证明
 * 2. 并行流 parallelStream() 并发安全陷阱（往非线程安全集合并发 add 导致数据丢失或越界）
 * 3. 规避公共 ForkJoinPool.commonPool() 线程池污染的最佳实践
 */
public class StreamAdvancedPipelineDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. Stream 惰性求值与短路操作内部执行轨迹 ==========");
        traceStreamExecutionPipeline();

        System.out.println("\n========== 2. parallelStream 并发写入非线程安全 List 的翻车事故 ==========");
        reproduceParallelStreamConcurrencyBug();

        System.out.println("\n========== 3. 并行流正确归约收集与分区 ==========");
        safeParallelCollection();
    }

    private static void traceStreamExecutionPipeline() {
        List<String> words = List.of("apple", "banana", "avocado", "apricot", "blueberry", "cherry");

        System.out.println("数据流: " + words);
        System.out.println("执行管道: filter(starts with 'a') -> map(toUpperCase) -> limit(2)");

        List<String> result = words.stream()
                .filter(w -> {
                    System.out.println("  [filter 评估]: " + w);
                    return w.startsWith("a");
                })
                .map(w -> {
                    System.out.println("  -> [map 转换]: " + w);
                    return w.toUpperCase();
                })
                .limit(2) // 达到 2 个后立即短路，后续元素绝不进入流水线
                .collect(Collectors.toList());

        System.out.println("最终结果: " + result);
        System.out.println(">>> 观察: apricot, blueberry, cherry 根本没有被后续 map 处理！Stream 采用垂直管道流水线，极度节省 CPU。");
    }

    private static void reproduceParallelStreamConcurrencyBug() {
        int totalElements = 10_000;
        List<Integer> unsafeList = new ArrayList<>();

        // 错误示范：在 parallelStream 中调用非线程安全的 unsafeList.add()
        try {
            IntStream.range(0, totalElements)
                    .parallel()
                    .forEach(unsafeList::add);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("【并发越界异常】: ArrayList 内部扩容时 size++ 非原子性导致 index 越界！");
        }

        System.out.println("期望收集元素数量: " + totalElements);
        System.out.println("实际收集元素数量: " + unsafeList.size() + " (数据发生大量并发丢失！)");
    }

    private static void safeParallelCollection() {
        int totalElements = 10_000;

        // 正确示范 1: 使用线程安全的收集器 collect(Collectors.toList())
        List<Integer> safeList = IntStream.range(0, totalElements)
                .parallel()
                .boxed()
                .collect(Collectors.toList());

        System.out.println("使用 Collectors.toList() 收集数量: " + safeList.size() + " (100% 精准无丢失)");

        // 规约累加 (reduce 天然线程安全)
        long sum = IntStream.rangeClosed(1, 100)
                .parallel()
                .reduce(0, Integer::sum);
        System.out.println("1 到 100 并行累加和: " + sum);
    }
}
