package com.knowledge.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * ============================================================================
 * 实验：工业级倒数排名融合算法 (Reciprocal Rank Fusion, RRF) 混合检索实现
 * ============================================================================
 * 
 * <h3>混合检索 (Hybrid Search) 的核心痛点与 RRF 破局</h3>
 * 单纯依赖向量语义检索（Dense）易在专有名词、错误码、精确定位上发生“语义漂移”；单纯依赖 BM25 稀疏检索（Sparse）则无法理解同义词与长尾意图。
 * 因此，工业界主流方案是“BM25 关键词检索 + 向量语义检索”双路召回。
 * 
 * <b>为什么不能简单按分数加权求和？</b>
 * <ul>
 *   <li>BM25 得分通常为无上界的浮点数（如 18.5、4.2），受文档长度与词频极大影响。</li>
 *   <li>向量余弦相似度得分处于 [0, 1] 闭区间内。</li>
 *   <li>两者的数值量纲、概率分布完全不同，简单的线性加权 $\alpha \cdot S_1 + (1-\alpha) \cdot S_2$ 极不稳定。</li>
 * </ul>
 * 
 * <b>RRF 算法原理</b>：
 * 舍弃绝对分值，仅采用文档在各路检索器中的<b>排序位置 (Rank)</b> 计算倒数累加：
 * $$RRF\_Score(d) = \sum_{m \in M} \frac{1}{k + r_m(d)}$$
 * 其中 $k$ 为平滑常数（业界标准通常取 $k = 60$），$r_m(d)$ 为文档在检索器 $m$ 中的名次（从 1 开始）。
 * 
 * @author Knowledge Base Architecture Team
 * @version 1.0.0
 */
public class RrfRankFusionAlgorithmDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("    RAG 核心实验：工业级倒数排名融合算法 (RRF) 混合检索与重排序演练              ");
        System.out.println("================================================================================\n");

        // 1. 模拟用户提问
        String query = "Spring AI 中出现 NoSuchBeanDefinitionException 异常排查与解决办法";
        System.out.println("【用户检索提问】: \"" + query + "\"\n");

        // 2. 模拟第一路：BM25 关键词倒排稀疏检索结果 (偏好精准匹配 NoSuchBeanDefinitionException)
        List<RankItem> bm25Results = List.of(
                new RankItem("DOC_A", "排查 Spring 容器中的 NoSuchBeanDefinitionException：检查 @Component 与包扫描路径", 18.65),
                new RankItem("DOC_B", "Spring Boot 自动配置与 Bean 注入故障诊断清单", 12.30),
                new RankItem("DOC_C", "Spring AI ChatClient 核心架构与 Advisor 机制深度解析", 6.80),
                new RankItem("DOC_D", "Java 虚拟机类加载机制与 ClassNotFoundException 分析", 4.10)
        );

        // 3. 模拟第二路：Dense 稠密向量语义检索结果 (偏好语义相似度：Spring AI 的配置与初始化)
        List<RankItem> denseResults = List.of(
                new RankItem("DOC_C", "Spring AI ChatClient 核心架构与 Advisor 机制深度解析", 0.912),
                new RankItem("DOC_A", "排查 Spring 容器中的 NoSuchBeanDefinitionException：检查 @Component 与包扫描路径", 0.885),
                new RankItem("DOC_E", "Spring AI 与 LangChain4j 框架选型与实战指南", 0.840),
                new RankItem("DOC_B", "Spring Boot 自动配置与 Bean 注入故障诊断清单", 0.795)
        );

        System.out.println(">>> [单路 1: BM25 关键词检索结果 Top-4 (分数无上界)]:");
        printRankList(bm25Results);

        System.out.println(">>> [单路 2: Dense 向量语义检索结果 Top-4 (分数为余弦相似度)]:");
        printRankList(denseResults);

        // 4. 执行 RRF 融合重排序算法 (标准平滑系数 k = 60)
        int rrfConstantK = 60;
        List<FusedDocResult> fusedResults = computeRrfFusion(List.of(bm25Results, denseResults), rrfConstantK);

        System.out.println(">>> [RRF 倒数排名融合算法生成的最终混合重排序结果 (k=60)]:");
        printFusedResults(fusedResults, rrfConstantK);

        // 5. 核心价值洞察
        System.out.println("================================================================================");
        System.out.println("【RRF 排序颠覆性优势分析】:");
        System.out.println(" • DOC_A 在 BM25 排名第 1，在 Dense 向量排名第 2，双路皆优，毫无悬念登顶 RRF 榜首！");
        System.out.println(" • DOC_C 虽然在 Dense 向量排第 1，但在 BM25 仅排第 3，综合得分被双路皆前茅的 DOC_A 稳健反超；");
        System.out.println(" • RRF 完全避免了人工调教 BM25 与 Dense 浮点权重乘数的脆弱性，具备高度鲁棒性。");
        System.out.println("================================================================================");
    }

    /**
     * 工业级 RRF 融合计算算法
     * 
     * @param retrievalRuns 多路检索结果列表（每路均为按自身分值排好序的列表）
     * @param k 平滑常数（业界推荐 60）
     * @return 经过 RRF 统一重排序的文档列表
     */
    public static List<FusedDocResult> computeRrfFusion(List<List<RankItem>> retrievalRuns, int k) {
        Map<String, FusedDocResult> docMap = new HashMap<>();

        for (int runIdx = 0; runIdx < retrievalRuns.size(); runIdx++) {
            List<RankItem> run = retrievalRuns.get(runIdx);
            for (int rank = 0; rank < run.size(); rank++) {
                RankItem item = run.get(rank);
                int actualRank = rank + 1; // 排名从 1 开始
                double rrfContribution = 1.0 / (k + actualRank);

                FusedDocResult fused = docMap.computeIfAbsent(item.getDocId(), id -> new FusedDocResult(
                        id,
                        item.getTitle(),
                        0.0,
                        new LinkedHashMap<>()
                ));

                fused.setRrfScore(fused.getRrfScore() + rrfContribution);
                fused.getRankDetails().put("Run#" + (runIdx + 1), actualRank);
            }
        }

        // 按照 RRF 综合得分从高到低排序
        List<FusedDocResult> sortedList = new ArrayList<>(docMap.values());
        sortedList.sort(Comparator.comparingDouble(FusedDocResult::getRrfScore).reversed());
        return sortedList;
    }

    private static void printRankList(List<RankItem> list) {
        for (int i = 0; i < list.size(); i++) {
            RankItem item = list.get(i);
            System.out.printf("  第 %d 名 | ID: %s | 原始得分: %-8.3f | 《%s》\n",
                    i + 1, item.getDocId(), item.getRawScore(), item.getTitle());
        }
        System.out.println();
    }

    private static void printFusedResults(List<FusedDocResult> list, int k) {
        for (int i = 0; i < list.size(); i++) {
            FusedDocResult doc = list.get(i);
            System.out.printf("  ★ 最终综合排名 [%d] | ID: %s | RRF 融合得分: %.6f\n",
                    i + 1, doc.getDocId(), doc.getRrfScore());
            System.out.println("     标题: 《" + doc.getTitle() + "》");
            System.out.println("     各路排名明细: " + doc.getRankDetails() + "  [公式: Σ 1/(" + k + " + rank)]");
        }
        System.out.println();
    }

    // ========================================================================
    // 数据模型
    // ========================================================================

    @Data
    @AllArgsConstructor
    public static class RankItem {
        private String docId;
        private String title;
        private double rawScore;
    }

    @Data
    @AllArgsConstructor
    public static class FusedDocResult {
        private String docId;
        private String title;
        private double rrfScore;
        private Map<String, Integer> rankDetails;
    }
}
