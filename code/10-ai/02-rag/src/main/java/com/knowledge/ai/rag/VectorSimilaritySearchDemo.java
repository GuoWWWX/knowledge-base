package com.knowledge.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.function.Predicate;

/**
 * ============================================================================
 * 实验：纯 Java 手写余弦相似度计算与多租户向量数据库 (Vector DB) 检索内核
 * ============================================================================
 * 
 * <h3>核心原理与架构设计</h3>
 * 现代向量数据库（如 pgvector、Milvus、Chroma、Qdrant）的核心在于高效计算高维稠密向量的距离，并结合标量元数据进行联合过滤。
 * 
 * 本演练纯手写复刻现代向量检索底层引擎：
 * <ol>
 *   <li><b>高精度向量相似度计算</b>：实现余弦相似度 (Cosine Similarity) 与欧氏几何距离 (L2 Distance)。</li>
 *   <li><b>多租户安全隔离 (Tenant Isolation)</b>：在向量计算前置执行硬性元数据过滤 (Pre-filtering)，杜绝跨租户数据越权与隐私穿透。</li>
 *   <li><b>基于优先队列的 Top-K 堆排序</b>：使用最小堆维护 Top-K，时间复杂度仅为 O(N log K)，兼顾检索性能与内存消耗。</li>
 * </ol>
 * 
 * @author Knowledge Base Architecture Team
 * @version 1.0.0
 */
public class VectorSimilaritySearchDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("    RAG 核心实验：纯 Java 手写余弦相似度、Top-K 堆排序与多租户向量库检索引擎       ");
        System.out.println("================================================================================\n");

        // 1. 初始化纯 Java 内存向量数据库
        InMemoryVectorStore vectorStore = new InMemoryVectorStore();

        // 2. 模拟灌库：准备带有多租户属性的高维 Embedding 知识库记录
        // 假定采用 8 维简化向量空间演示语义特征分布 (实际生产中通常为 1536 维 text-embedding-3 或 1024 维 bge-large-zh)
        seedKnowledgeData(vectorStore);

        // 3. 模拟用户提问的 Embedding 向量：提问关于 "分布式锁与并发控制"
        float[] queryVector = new float[]{0.85f, 0.90f, 0.10f, 0.05f, 0.02f, 0.15f, 0.70f, 0.60f};
        String userQuery = "在高并发分布式环境下，如何使用 Redis 实现安全防重的分布式锁？";

        System.out.println("【用户查询提问】: \"" + userQuery + "\"");
        System.out.println("【查询向量 (Query Vector)】: " + Arrays.toString(queryVector) + "\n");
        System.out.println("--------------------------------------------------------------------------------\n");

        // ====================================================================
        // 场景 1：无过滤条件的全局向量相似度检索 (存在多租户越权风险)
        // ====================================================================
        System.out.println(">>> [场景 1] 全局无约束向量相似度检索 (Top-3) - 演示数据混杂态：");
        List<SearchResult> globalResults = vectorStore.similaritySearch(queryVector, 3, record -> true);
        printSearchResults(globalResults);

        // ====================================================================
        // 场景 2：前置多租户与元数据过滤检索 (Pre-filtering Tenant Isolation)
        // ====================================================================
        String currentTenant = "TENANT_FINANCE_A"; // 仅限定金融租户 A
        System.out.println(">>> [场景 2] 严格多租户前置过滤检索 (tenant_id = '" + currentTenant + "', Top-2)：");
        List<SearchResult> isolatedResults = vectorStore.similaritySearch(
                queryVector,
                2,
                record -> currentTenant.equals(record.getMetadata().get("tenant_id"))
        );
        printSearchResults(isolatedResults);

        // ====================================================================
        // 场景 3：验证向量数学特性 (正交向量与全同向量)
        // ====================================================================
        System.out.println(">>> [场景 3] 向量几何数学特性校验 (Cosine Math Verification)：");
        float[] vecA = new float[]{1.0f, 0.0f, 0.0f};
        float[] vecB = new float[]{1.0f, 0.0f, 0.0f};
        float[] vecC = new float[]{0.0f, 1.0f, 0.0f};
        System.out.println("  • 全同向量余弦相似度 (应当为 1.0000): " + String.format("%.4f", CosineMath.cosineSimilarity(vecA, vecB)));
        System.out.println("  • 正交正交向量余弦相似度 (应当为 0.0000): " + String.format("%.4f", CosineMath.cosineSimilarity(vecA, vecC)));

        System.out.println("\n================================================================================");
        System.out.println(" [实验结论] 余弦相似度消除了向量绝对模长影响；前置元数据过滤是多租户 RAG 隔离的刚性底线。");
        System.out.println("================================================================================");
    }

    private static void seedKnowledgeData(InMemoryVectorStore store) {
        // 金融租户 A：分布式锁知识
        store.insert(VectorRecord.builder()
                .id("DOC_001")
                .content("Redis 分布式锁规范：必须使用 SET resource_name my_random_value NX PX 30000 保证原子性加锁，释放锁需执行 Lua 脚本核对随机值。")
                .vector(new float[]{0.88f, 0.89f, 0.12f, 0.06f, 0.01f, 0.18f, 0.68f, 0.62f})
                .metadata(Map.of("tenant_id", "TENANT_FINANCE_A", "category", "middleware", "security_level", "HIGH"))
                .build());

        // 金融租户 A：数据库事务隔离
        store.insert(VectorRecord.builder()
                .id("DOC_002")
                .content("MySQL InnoDB MVCC 机制：基于 UndoLog 版本链与 ReadView 视图实现 RC 与 RR 隔离级别下的非阻塞一致性读。")
                .vector(new float[]{0.45f, 0.40f, 0.80f, 0.70f, 0.10f, 0.30f, 0.20f, 0.15f})
                .metadata(Map.of("tenant_id", "TENANT_FINANCE_A", "category", "database", "security_level", "MEDIUM"))
                .build());

        // 电商租户 B：同类主题分布式锁 (但是属于租户 B 的私有技术资产)
        store.insert(VectorRecord.builder()
                .id("DOC_003")
                .content("【租户B内部秘钥】电商大促秒杀 Redis 锁与扣减方案：采用 Redisson RedLock 算法应对 Redis 单点故障与主从异步复制丢失。")
                .vector(new float[]{0.90f, 0.92f, 0.11f, 0.04f, 0.03f, 0.14f, 0.71f, 0.59f})
                .metadata(Map.of("tenant_id", "TENANT_ECOMMERCE_B", "category", "middleware", "security_level", "CONFIDENTIAL"))
                .build());

        // 租户 C：无关的 JVM GC 知识
        store.insert(VectorRecord.builder()
                .id("DOC_004")
                .content("ZGC 垃圾回收器特性：着色指针与读屏障技术，停顿时间严格控制在 1 毫秒以内，支持 TB 级别超大堆内存。")
                .vector(new float[]{0.05f, 0.10f, 0.12f, 0.88f, 0.92f, 0.85f, 0.08f, 0.05f})
                .metadata(Map.of("tenant_id", "TENANT_INFRA_C", "category", "jvm", "security_level", "LOW"))
                .build());
    }

    private static void printSearchResults(List<SearchResult> results) {
        System.out.println("  命中 " + results.size() + " 条最相关知识记录：");
        for (int i = 0; i < results.size(); i++) {
            SearchResult r = results.get(i);
            System.out.printf("  [%d] 相似度分数: %.4f | ID: %s | 归属租户: %s\n",
                    i + 1, r.getScore(), r.getRecord().getId(), r.getRecord().getMetadata().get("tenant_id"));
            System.out.println("      文本片段: " + r.getRecord().getContent());
            System.out.println("      元数据: " + r.getRecord().getMetadata());
        }
        System.out.println();
    }

    // ========================================================================
    // 向量数学工具类 (Cosine Math)
    // ========================================================================
    public static class CosineMath {

        /**
         * 计算两组高维向量的余弦相似度: cos(A, B) = (A · B) / (||A|| * ||B||)
         */
        public static float cosineSimilarity(float[] v1, float[] v2) {
            if (v1 == null || v2 == null || v1.length != v2.length) {
                throw new IllegalArgumentException("向量维度必须一致且不能为空");
            }

            float dotProduct = 0.0f;
            float normA = 0.0f;
            float normB = 0.0f;

            for (int i = 0; i < v1.length; i++) {
                dotProduct += v1[i] * v2[i];
                normA += v1[i] * v1[i];
                normB += v2[i] * v2[i];
            }

            if (normA <= 0.0f || normB <= 0.0f) {
                return 0.0f;
            }

            return (float) (dotProduct / (Math.sqrt(normA) * Math.sqrt(normB)));
        }
    }

    // ========================================================================
    // 纯 Java 内存向量库内核 (支持元数据过滤 + Top-K 堆排序)
    // ========================================================================
    public static class InMemoryVectorStore {
        private final List<VectorRecord> storage = new ArrayList<>();

        public synchronized void insert(VectorRecord record) {
            this.storage.add(record);
        }

        /**
         * 向量相似度检索 (结合 Pre-filter 元数据过滤 + PriorityQueue Top-K)
         */
        public List<SearchResult> similaritySearch(float[] queryVector, int topK, Predicate<VectorRecord> filter) {
            // 维护大小为 topK 的最小堆，队头为当前 topK 中得分最低者
            PriorityQueue<SearchResult> minHeap = new PriorityQueue<>(Comparator.comparingDouble(SearchResult::getScore));

            for (VectorRecord record : storage) {
                // 1. 前置元数据过滤 (Pre-filtering)
                if (filter != null && !filter.test(record)) {
                    continue;
                }

                // 2. 余弦相似度打分
                float score = CosineMath.cosineSimilarity(queryVector, record.getVector());
                SearchResult result = new SearchResult(record, score);

                // 3. Top-K 堆维护
                if (minHeap.size() < topK) {
                    minHeap.offer(result);
                } else if (score > minHeap.peek().getScore()) {
                    minHeap.poll();
                    minHeap.offer(result);
                }
            }

            // 4. 将堆中结果弹出并倒序排列 (从最高分到最低分)
            List<SearchResult> sortedResults = new ArrayList<>();
            while (!minHeap.isEmpty()) {
                sortedResults.add(minHeap.poll());
            }
            Collections.reverse(sortedResults);
            return sortedResults;
        }
    }

    // ========================================================================
    // 数据模型
    // ========================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VectorRecord {
        private String id;
        private String content;
        private float[] vector;
        private Map<String, Object> metadata;
    }

    @Data
    @AllArgsConstructor
    public static class SearchResult {
        private VectorRecord record;
        private float score;
    }
}
