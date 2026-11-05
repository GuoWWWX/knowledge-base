package com.knowledge.ai.rag.cag;

/**
 * CAG (Cache-Augmented Generation / 提示词缓存) vs 长上下文 (Long-Context) 选型模型：
 * 1. 概念演进：
 *    - 经典 RAG: 外部切片向量检索 -> 拼装进入 Context -> 适合超大知识库 (百万文档)
 *    - Long-Context: 将 100k~200k Token 的全部知识库直接灌入窗口 -> 零检索丢失，但首字延迟 (TTFT) 高且极其昂贵
 *    - CAG (Prompt Caching): 利用 KV Cache 服务端常驻，命中前缀缓存时 Token 计费打 1~2 折，首字响应暴降 80%！
 * 2. 架构选型量化决策计算器实战
 */
public class CagVsLongContextCompareDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. RAG vs Long-Context vs CAG (Prompt Caching) 核心差异 ==========");
        printArchitectureComparisonTable();

        System.out.println("\n========== 2. 企业知识库在不同架构下的成本与 TTFT 首字延迟定量测算 ==========");
        simulateCostAndLatencyAnalysis(50_000, 10_000); // 5万 Token 静态知识库，每天 10,000 次问答请求
    }

    private static void printArchitectureComparisonTable() {
        System.out.printf("%-15s | %-12s | %-15s | %-15s | %-20s%n",
                "架构形态", "召回召全率", "首字延迟(TTFT)", "单次调用成本", "适用知识库体量");
        System.out.println("---------------------------------------------------------------------------------------------");
        System.out.printf("%-15s | %-12s | %-15s | %-15s | %-20s%n",
                "经典 RAG", "依赖 Chunking", "中等 (~1.2s)", "低 (约 2k token)", "超海量 (GB~TB 级)");
        System.out.printf("%-15s | %-12s | %-15s | %-15s | %-20s%n",
                "全量长上下文", "100% 完整上下文", "高 (~4.5s)", "极高 (每次全量计费)", "小型知识库 (<100k token)");
        System.out.printf("%-15s | %-12s | %-15s | %-15s | %-20s%n",
                "CAG 提示词缓存", "100% 完整上下文", "极低 (~0.3s)", "大幅降低 (缓存命中90%)", "中型高频知识库 (<200k token)");
    }

    private static void simulateCostAndLatencyAnalysis(int staticDocTokens, int dailyQueries) {
        double normalInputPricePerMillion = 2.5; // 每百万 Token $2.5
        double cachedInputPricePerMillion = 0.5; // 命中 Prompt Cache 每百万 Token $0.5 (2折)
        double outputPricePerMillion = 10.0;     // 输出 Token 价格
        int avgQueryTokens = 200;
        int avgResponseTokens = 500;

        System.out.println("【基准参数】:");
        System.out.printf("  企业静态文档 Token 数 : %,d Token%n", staticDocTokens);
        System.out.printf("  每日并发问答查询量     : %,d 次%n", dailyQueries);

        // 1. 无缓存长上下文方案 (每次重复传输全量文档)
        double longContextDailyCost = dailyQueries * (
                ((staticDocTokens + avgQueryTokens) / 1_000_000.0) * normalInputPricePerMillion +
                (avgResponseTokens / 1_000_000.0) * outputPricePerMillion
        );

        // 2. CAG 提示词缓存方案 (首次写入缓存，后续 98% 命中已缓存的前缀 KV Cache)
        double cacheHitRate = 0.98;
        double cagDailyCost = dailyQueries * (
                ((staticDocTokens * (1 - cacheHitRate) + avgQueryTokens) / 1_000_000.0) * normalInputPricePerMillion +
                ((staticDocTokens * cacheHitRate) / 1_000_000.0) * cachedInputPricePerMillion +
                (avgResponseTokens / 1_000_000.0) * outputPricePerMillion
        );

        System.out.println("\n【测算结果】:");
        System.out.printf("  1. 无缓存长上下文每日成本 : $%,.2f 美元 / 天%n", longContextDailyCost);
        System.out.printf("  2. CAG 提示词缓存每日成本 : $%,.2f 美元 / 天%n", cagDailyCost);
        System.out.printf("  ★ 成本节省比例          : %.1f%%%n", (1 - (cagDailyCost / longContextDailyCost)) * 100);
        System.out.printf("  ★ 首字延迟 TTFT 提升    : 从 4500ms 暴降至 350ms (提升 12.8 倍体验)%n");
        System.out.println("\n>>> 【架构师选型建议】: 知识库在 100k Token 以内且查询高频时，优先采用 CAG 提示词缓存，无需复杂分块与向量库即可兼得完整理解力与极致性价比！");
    }
}
