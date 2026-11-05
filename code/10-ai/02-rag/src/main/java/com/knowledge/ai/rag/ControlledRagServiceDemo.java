package com.knowledge.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * ============================================================================
 * 实验：工业级受控 RAG (Controlled & Grounded RAG) 全链路防幻觉体系
 * ============================================================================
 * 
 * <h3>核心原理与防幻觉工程</h3>
 * 许多初级 RAG 系统上线后常遭遇"幻觉事故"：当用户询问知识库之外的问题时，大模型经常凭借内化概率胡编乱造，引发客诉。
 * 生产级受控 RAG 通过<b>双重防护门禁</b>确保忠实度 (Faithfulness & Groundedness)：
 * <ol>
 *   <li><b>第一重门禁：检索置信度阈值 (Relevance Score Gate)</b>：
 *       若召回 Top-1 结果的余弦相似度低于预设阈值 (如 0.65)，直接在框架层判定未命中，走确定性兜底，避免浪费推理 Token。</li>
 *   <li><b>第二重门禁：强约束 System Prompt (Negative Constraints)</b>：
 *       严禁大模型引入外部未经证实的常识，强制要求"若参考资料未包含，则必须回复统一兜底模板"，并标注引用来源。</li>
 * </ol>
 * 
 * @author Knowledge Base Architecture Team
 * @version 1.0.0
 */
public class ControlledRagServiceDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("    RAG 核心实验：工业级受控 RAG 全链路工程与防幻觉兜底机制演示                  ");
        System.out.println("================================================================================\n");

        // 1. 初始化受控知识库检索服务
        ControlledRagEngine ragEngine = new ControlledRagEngine(0.65f); // 相似度门槛设定为 0.65

        // ====================================================================
        // 场景 1：命中有据可循的内部知识 (Grounded Answer)
        // ====================================================================
        System.out.println(">>> [测试场景 1] 提问内部企业政策（知识库中存在确凿依据）：");
        String question1 = "请问入职满 3 年的老员工，每年享有几天带薪年休假？病假期间工资如何计算？";
        RagResponse response1 = ragEngine.ask(question1);
        printRagResponse(response1);

        System.out.println("\n--------------------------------------------------------------------------------\n");

        // ====================================================================
        // 场景 2：提问知识库不存在的内容 (Out-of-Domain Query -> 触发受控兜底)
        // ====================================================================
        System.out.println(">>> [测试场景 2] 提问知识库外部问题（验证防幻觉硬拦截与确定性兜底）：");
        String question2 = "请问公司计划何时在火星建立研发中心基地？第一批外派补贴是多少？";
        RagResponse response2 = ragEngine.ask(question2);
        printRagResponse(response2);

        System.out.println("\n================================================================================");
        System.out.println(" [实验结论] 受控 RAG 通过相似度门禁 + 强约束提示词，实现确定性知识交付，根治大模型幻觉。");
        System.out.println("================================================================================");
    }

    private static void printRagResponse(RagResponse resp) {
        System.out.println("  【用户提问】: " + resp.getQuery());
        System.out.println("  【命中判定】: " + (resp.isHit() ? "✔ 知识库有效命中 (高于阈值)" : "✖ 知识库未命中 (触发安全兜底)"));
        System.out.println("  【召回最高分】: " + String.format("%.4f", resp.getTopScore()) + " (阈值门限: " + resp.getThreshold() + ")");
        if (resp.isHit()) {
            System.out.println("  【参考证据来源】:");
            for (String source : resp.getCitedSources()) {
                System.out.println("    • " + source);
            }
        }
        System.out.println("  【AI 最终回答】:\n" + resp.getAnswer());
    }

    // ========================================================================
    // 受控 RAG 服务引擎 (ControlledRagEngine)
    // ========================================================================

    public static class ControlledRagEngine {
        private final float similarityThreshold;
        private final List<KnowledgeItem> internalKnowledgeBase = new ArrayList<>();

        public ControlledRagEngine(float similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
            seedInternalKnowledge();
        }

        private void seedInternalKnowledge() {
            internalKnowledgeBase.add(new KnowledgeItem(
                    "DOC_HR_001",
                    "员工假期福利制度",
                    "入职满1年不满10年者，每年带薪年休假为7个工作日；满10年不满20年者为10个工作日。病假工资按当地最低工资标准的80%计发。",
                    new float[]{0.85f, 0.80f, 0.10f, 0.05f, 0.00f, 0.00f}
            ));
            internalKnowledgeBase.add(new KnowledgeItem(
                    "DOC_HR_002",
                    "差旅与报销规范",
                    "境内出差按一线城市每天400元住宿标准报销，高铁二等座凭票报销，出差期间餐饮补贴标准为每天100元。",
                    new float[]{0.05f, 0.10f, 0.85f, 0.80f, 0.00f, 0.00f}
            ));
        }

        public RagResponse ask(String userQuery) {
            // 1. 模拟文本向量化 (Embedding)
            float[] queryVector = mockEmbed(userQuery);

            // 2. 检索知识库并计算最高相似度
            KnowledgeItem bestMatch = null;
            float maxScore = -1.0f;

            for (KnowledgeItem item : internalKnowledgeBase) {
                float score = cosineSimilarity(queryVector, item.getVector());
                if (score > maxScore) {
                    maxScore = score;
                    bestMatch = item;
                }
            }

            // 3. 第一重门禁：阈值校验 (Threshold Guard)
            if (maxScore < similarityThreshold || bestMatch == null) {
                return RagResponse.builder()
                        .query(userQuery)
                        .isHit(false)
                        .topScore(maxScore)
                        .threshold(similarityThreshold)
                        .answer("抱歉，根据公司内部知识库现有资料，未检索到与您问题相关的内容。建议您咨询 HR 服务台或相关部门负责人。")
                        .citedSources(Collections.emptyList())
                        .build();
            }

            // 4. 第二重门禁：注入严格的受控 Prompt 模板
            String systemPrompt = """
                    你是一名严谨的企业智能行政助理。
                    必须严格仅根据下方【参考资料】中提供的事实内容回答问题。
                    禁止根据任何外部经验或推测编造未提及的信息。
                    回答时请务必注明引用的文档名称。
                    """;

            String userPromptWithContext = String.format("""
                    【参考资料】:
                    - 文档标题: 《%s》 (编号: %s)
                    - 资料正文: %s

                    【用户问题】:
                    %s
                    """, bestMatch.getTitle(), bestMatch.getId(), bestMatch.getContent(), userQuery);

            // 5. 模拟大模型在强上下文约束下的生成
            String generatedAnswer = "根据《" + bestMatch.getTitle() + "》的权威规定：\n" +
                    "1. 入职满 3 年属于“满1年不满10年”区间，您每年享有 7 个工作日 的带薪年休假；\n" +
                    "2. 员工在病假期间的工资，统一按照当地最低工资标准的 80% 进行计发。";

            return RagResponse.builder()
                    .query(userQuery)
                    .isHit(true)
                    .topScore(maxScore)
                    .threshold(similarityThreshold)
                    .answer(generatedAnswer)
                    .citedSources(List.of("《" + bestMatch.getTitle() + "》 (编号: " + bestMatch.getId() + ")"))
                    .build();
        }

        private float[] mockEmbed(String text) {
            if (text.contains("年休假") || text.contains("病假") || text.contains("福利")) {
                return new float[]{0.88f, 0.82f, 0.08f, 0.04f, 0.00f, 0.00f};
            } else if (text.contains("差旅") || text.contains("报销")) {
                return new float[]{0.06f, 0.09f, 0.86f, 0.82f, 0.00f, 0.00f};
            } else {
                // 外界无关内容，例如火星基地：激活正交维度 4 与 5
                return new float[]{0.01f, 0.02f, 0.01f, 0.02f, 0.90f, 0.85f};
            }
        }

        private float cosineSimilarity(float[] a, float[] b) {
            float dot = 0f, normA = 0f, normB = 0f;
            for (int i = 0; i < a.length; i++) {
                dot += a[i] * b[i];
                normA += a[i] * a[i];
                normB += b[i] * b[i];
            }
            return (float) (dot / (Math.sqrt(normA) * Math.sqrt(normB)));
        }
    }

    // ========================================================================
    // 数据模型
    // ========================================================================

    @Data
    @AllArgsConstructor
    public static class KnowledgeItem {
        private String id;
        private String title;
        private String content;
        private float[] vector;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RagResponse {
        private String query;
        private boolean isHit;
        private float topScore;
        private float threshold;
        private String answer;
        private List<String> citedSources;
    }
}
