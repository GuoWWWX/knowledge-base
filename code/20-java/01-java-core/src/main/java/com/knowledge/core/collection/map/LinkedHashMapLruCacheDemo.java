package com.knowledge.core.collection.map;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于 LinkedHashMap 手写工业级 LRU (Least Recently Used) 缓存
 * 核心原理：
 * 1. LinkedHashMap 内部维护双向链表，记录 entry 插入顺序或访问顺序
 * 2. 构造方法中传入 accessOrder = true，每次 get 或 put 都会将节点移至双向链表尾部
 * 3. 重写 removeEldestEntry 方法，容量超限时自动淘汰链表头部（最久未访问）的元素
 */
public class LinkedHashMapLruCacheDemo {

    public static void main(String[] args) {
        System.out.println("========== 手写 LRU 缓存淘汰演示 (容量=3) ==========");
        LruCache<String, String> cache = new LruCache<>(3);

        System.out.println("1. 依次写入 A, B, C:");
        cache.put("A", "数据A");
        cache.put("B", "数据B");
        cache.put("C", "数据C");
        cache.printOrder(); // 顺序: A -> B -> C

        System.out.println("\n2. 读取最老数据 A (触发双向链表节点重排至尾部):");
        String valA = cache.get("A");
        System.out.println("cache.get('A') = " + valA);
        cache.printOrder(); // 顺序: B -> C -> A

        System.out.println("\n3. 写入新数据 D (容量达到上限，淘汰链表头部最久未被访问的 B):");
        cache.put("D", "数据D");
        cache.printOrder(); // 顺序: C -> A -> D

        System.out.println("\n4. 检验淘汰结果:");
        System.out.println("cache.containsKey('B') = " + cache.containsKey("B") + " (已被安全淘汰)");
        System.out.println("cache.containsKey('A') = " + cache.containsKey("A") + " (最近被访问，幸存)");
        System.out.println("cache.containsKey('C') = " + cache.containsKey("C") + " (幸存)");
        System.out.println("cache.containsKey('D') = " + cache.containsKey("D") + " (最新插入)");
    }

    public static class LruCache<K, V> extends LinkedHashMap<K, V> {
        private final int maxCapacity;

        /**
         * @param maxCapacity 缓存最大容量
         */
        public LruCache(int maxCapacity) {
            // initialCapacity = maxCapacity
            // loadFactor = 0.75f
            // accessOrder = true: 按访问顺序排序（最近访问的排在尾部）；false: 按插入顺序
            super(maxCapacity, 0.75f, true);
            this.maxCapacity = maxCapacity;
        }

        /**
         * 当 put/putAll 添加新元素后，LinkedHashMap 会回调此方法
         * 如果返回 true，则自动移除最老（最久未访问）的 entry
         */
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
            boolean shouldRemove = size() > maxCapacity;
            if (shouldRemove) {
                System.out.println(">>> [LRU 淘汰触发] 移除最久未访问键值: " + eldest.getKey() + "=" + eldest.getValue());
            }
            return shouldRemove;
        }

        public void printOrder() {
            System.out.print("当前缓存内部顺序 (head 最旧 -> tail 最新): ");
            forEach((k, v) -> System.out.print("[" + k + "] "));
            System.out.println();
        }
    }
}
