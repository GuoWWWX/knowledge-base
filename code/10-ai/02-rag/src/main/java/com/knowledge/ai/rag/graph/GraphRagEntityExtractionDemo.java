package com.knowledge.ai.rag.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 微型 GraphRAG (知识图谱增强检索) 深度实战：
 * 1. 传统向量 RAG 的局限：对于跨多文档的间接关系、“A 和 C 是什么关系”的多跳 (Multi-hop) 推理，向量相似度常常断裂失效
 * 2. 实体关系抽取 (Entity-Relation Extraction): 将非结构化文本解析为 (Subject, Predicate, Object) 知识三元组
 * 3. 混合图检索 (Graph Traversal): 结合种子实体，沿图谱边进行 1-hop / 2-hop 深度遍历，构建因果图谱子图作为 LLM 上下文
 */
public class GraphRagEntityExtractionDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. 非结构化文档文本与三元组图谱构建 ==========");
        MiniKnowledgeGraph kg = new MiniKnowledgeGraph();

        // 模拟从文档中抽取的实体与关系三元组
        kg.addTriple("张小明", "任职于", "未来科技创新实验室");
        kg.addTriple("未来科技创新实验室", "主导研发", "量子计算加密协议V2");
        kg.addTriple("量子计算加密协议V2", "核心算法依赖", "Shor质因数分解算法");
        kg.addTriple("未来科技创新实验室", "归属于", "国家先进计算研究院");
        kg.addTriple("国家先进计算研究院", "位于", "北京市中关村科技园区");

        kg.printGraphStatistics();

        System.out.println("\n========== 2. 多跳图谱检索 (Multi-Hop Graph Search) ==========");
        String queryEntity = "张小明";
        System.out.println("查询目标实体: " + queryEntity);

        System.out.println("\n--- 1-Hop 直接一度邻接关系 ---");
        List<Triple> oneHop = kg.searchHops(queryEntity, 1);
        oneHop.forEach(t -> System.out.println("  " + t));

        System.out.println("\n--- 2-Hop 跨实体二度深层因果关系 ---");
        List<Triple> twoHops = kg.searchHops(queryEntity, 2);
        twoHops.forEach(t -> System.out.println("  " + t));

        System.out.println("\n========== 3. GraphRAG 上下文拼装与 LLM 增强 ==========");
        String graphContext = kg.buildGraphPromptContext(queryEntity, 2);
        System.out.println("注入给大模型的图谱增强上下文:\n" + graphContext);
        System.out.println(">>> 优势: LLM 可以极其精准地回答出：“张小明通过其任职的未来科技创新实验室，间接主导了量子计算加密协议V2的研发，该协议归属于国家先进计算研究院”！");
    }

    public record Triple(String subject, String predicate, String object) {
        @Override
        public String toString() {
            return String.format("(%s) ---[%s]---> (%s)", subject, predicate, object);
        }
    }

    public static class MiniKnowledgeGraph {
        private final Map<String, List<Triple>> forwardAdjList = new HashMap<>();
        private final Set<String> entities = new HashSet<>();

        public void addTriple(String subject, String predicate, String object) {
            Triple triple = new Triple(subject, predicate, object);
            forwardAdjList.computeIfAbsent(subject, k -> new ArrayList<>()).add(triple);
            entities.add(subject);
            entities.add(object);
        }

        public List<Triple> searchHops(String startEntity, int maxHops) {
            List<Triple> results = new ArrayList<>();
            Set<String> visitedEntities = new HashSet<>();
            Set<String> currentLevel = new HashSet<>();

            currentLevel.add(startEntity);
            visitedEntities.add(startEntity);

            for (int hop = 1; hop <= maxHops; hop++) {
                Set<String> nextLevel = new HashSet<>();
                for (String entity : currentLevel) {
                    List<Triple> neighbors = forwardAdjList.getOrDefault(entity, List.of());
                    for (Triple t : neighbors) {
                        results.add(t);
                        if (!visitedEntities.contains(t.object())) {
                            visitedEntities.add(t.object());
                            nextLevel.add(t.object());
                        }
                    }
                }
                currentLevel = nextLevel;
                if (currentLevel.isEmpty()) break;
            }
            return results;
        }

        public String buildGraphPromptContext(String startEntity, int hops) {
            List<Triple> triples = searchHops(startEntity, hops);
            StringBuilder sb = new StringBuilder("【知识图谱事实推演链】:\n");
            for (Triple t : triples) {
                sb.append("- ").append(t.subject()).append(" ").append(t.predicate()).append(" ").append(t.object()).append("\n");
            }
            return sb.toString();
        }

        public void printGraphStatistics() {
            int edgeCount = forwardAdjList.values().stream().mapToInt(List::size).sum();
            System.out.printf("图谱构建完成: 实体节点数=%d, 关联关系边数=%d%n", entities.size(), edgeCount);
        }
    }
}
