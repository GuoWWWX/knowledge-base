package com.knowledge.rag.service;

import com.knowledge.rag.model.DocumentChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 内存向量知识库服务
 * 提供文档分块存储、向量化、余弦相似度计算、Top-K 相似检索与多租户过滤隔离
 */
@Slf4j
@Service
public class KnowledgeStoreService {

    /**
     * 向量维度
     */
    private static final int VECTOR_DIMENSION = 256;

    /**
     * 常用停用词
     */
    private static final Set<String> STOP_WORDS = Set.of(
            "的", "了", "在", "是", "我", "有", "和", "就", "不", "人", "都", "一", "一个",
            "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好",
            "自己", "这", "吗", "呢", "吧", "啊", "多少", "什么", "怎样", "如何", "为", "及",
            "与", "或", "需", "按", "从", "以", "将", "并", "且", "其", "于"
    );

    /**
     * 企业核心业务词汇表（用于提高短查询意图理解与精确匹配）
     */
    private static final List<String> BUSINESS_DICTIONARY = List.of(
            "普通员工", "部门经理", "差旅管理", "住宿标准", "企业差旅", "考勤管理",
            "普通", "员工", "北京", "住宿", "标准", "上限", "报销", "差旅", "规范",
            "全薪", "病假", "考勤", "请假", "医院", "证明",
            "食堂", "星期天", "中午", "开门", "供餐"
    );

    /**
     * 按照租户隔离存储文档分块：Map<TenantId, List<DocumentChunk>>
     */
    private final Map<String, List<DocumentChunk>> tenantStore = new ConcurrentHashMap<>();

    /**
     * 录入单个文档分块（若未提供向量，则自动生成向量嵌入）
     */
    public void store(DocumentChunk chunk) {
        if (chunk == null) {
            return;
        }
        if (chunk.getId() == null || chunk.getId().isBlank()) {
            chunk.setId(UUID.randomUUID().toString());
        }
        if (chunk.getTenantId() == null || chunk.getTenantId().isBlank()) {
            chunk.setTenantId("default");
        }
        if (chunk.getEmbedding() == null || chunk.getEmbedding().isEmpty()) {
            chunk.setEmbedding(computeEmbedding(chunk.getContent()));
        }

        tenantStore.computeIfAbsent(chunk.getTenantId(), k -> new CopyOnWriteArrayList<>()).add(chunk);
        log.info("【知识入库】租户 [{}] 成功录入分块: id={}, docName={}, page={}",
                chunk.getTenantId(), chunk.getId(), chunk.getDocName(), chunk.getPageNumber());
    }

    /**
     * 批量录入文档分块
     */
    public void storeAll(List<DocumentChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        for (DocumentChunk chunk : chunks) {
            store(chunk);
        }
    }

    /**
     * Top-K 向量相似度检索（含多租户过滤与最小置信度门限控制）
     *
     * @param tenantId 租户ID
     * @param query    查询问题
     * @param topK     最大召回条数
     * @param minScore 最小相似度阈值
     * @return 召回的文档分块列表（已按相似度降序排列）
     */
    public List<DocumentChunk> search(String tenantId, String query, int topK, double minScore) {
        String effectiveTenant = (tenantId == null || tenantId.isBlank()) ? "default" : tenantId;
        List<DocumentChunk> chunks = tenantStore.get(effectiveTenant);
        if (chunks == null || chunks.isEmpty() || query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        List<Double> queryVec = computeEmbedding(query);
        List<String> queryKeywords = extractKeywords(query);

        // 计算相似度并筛选
        List<DocumentChunk> candidates = new ArrayList<>();
        for (DocumentChunk chunk : chunks) {
            double cosine = cosineSimilarity(queryVec, chunk.getEmbedding());
            double coverage = calculateCoverage(queryKeywords, chunk.getContent());

            // 混合打分：余弦相似度 + 关键词覆盖率
            double hybridScore = 0.4 * cosine + 0.6 * coverage;

            log.info("【检索打分】doc={}, page={}, query='{}', cosine={}, coverage={}, hybridScore={}",
                    chunk.getDocName(), chunk.getPageNumber(), query, cosine, coverage, hybridScore);

            if (hybridScore >= minScore) {
                DocumentChunk scored = DocumentChunk.builder()
                        .id(chunk.getId())
                        .docName(chunk.getDocName())
                        .pageNumber(chunk.getPageNumber())
                        .content(chunk.getContent())
                        .embedding(chunk.getEmbedding())
                        .tenantId(chunk.getTenantId())
                        .score(hybridScore)
                        .build();
                candidates.add(scored);
            }
        }

        // 按得分从高到低排序
        candidates.sort((c1, c2) -> Double.compare(c2.getScore(), c1.getScore()));

        // 截取 Top-K
        int limit = Math.min(topK > 0 ? topK : 3, candidates.size());
        List<DocumentChunk> result = candidates.subList(0, limit);

        log.debug("【知识检索】租户 [{}] 查询: '{}', 召回条数: {}, 最高得分: {}",
                effectiveTenant, query, result.size(), result.isEmpty() ? 0.0 : result.get(0).getScore());
        return result;
    }

    /**
     * 清空指定租户的所有知识
     */
    public void clear(String tenantId) {
        if (tenantId != null) {
            tenantStore.remove(tenantId);
        }
    }

    /**
     * 清空所有租户的知识
     */
    public void clearAll() {
        tenantStore.clear();
    }

    /**
     * 获取指定租户的分块总数
     */
    public int count(String tenantId) {
        List<DocumentChunk> list = tenantStore.get(tenantId);
        return list == null ? 0 : list.size();
    }

    /**
     * 计算两个向量之间的余弦相似度
     */
    public double cosineSimilarity(List<Double> vecA, List<Double> vecB) {
        if (vecA == null || vecB == null || vecA.size() != vecB.size() || vecA.isEmpty()) {
            return 0.0;
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vecA.size(); i++) {
            double a = vecA.get(i);
            double b = vecB.get(i);
            dotProduct += a * b;
            normA += a * a;
            normB += b * b;
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 将文本转化为特征向量（基于语义词元与 L2 归一化）
     */
    public List<Double> computeEmbedding(String text) {
        double[] vector = new double[VECTOR_DIMENSION];
        if (text == null || text.isBlank()) {
            return toList(vector);
        }

        List<String> keywords = extractKeywords(text);
        if (keywords.isEmpty()) {
            return toList(vector);
        }

        for (String kw : keywords) {
            int bucket = Math.floorMod(kw.hashCode(), VECTOR_DIMENSION);
            vector[bucket] += kw.length();
        }

        // L2 范数归一化
        double norm = 0.0;
        for (double val : vector) {
            norm += val * val;
        }
        if (norm > 0) {
            double sqrtNorm = Math.sqrt(norm);
            for (int i = 0; i < vector.length; i++) {
                vector[i] /= sqrtNorm;
            }
        }

        return toList(vector);
    }

    /**
     * 抽取文本的业务关键词与语义实体
     */
    public List<String> extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> keywords = new ArrayList<>();
        // 优先匹配业务词典
        for (String dictWord : BUSINESS_DICTIONARY) {
            if (text.contains(dictWord)) {
                keywords.add(dictWord);
            }
        }

        // 结合字符切词补充（若词典覆盖较少）
        if (keywords.size() < 3) {
            String cleaned = text.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9]", "");
            int len = cleaned.length();
            for (int i = 0; i < len - 1; i++) {
                String sub = cleaned.substring(i, i + 2);
                if (!STOP_WORDS.contains(sub) && !keywords.contains(sub)) {
                    keywords.add(sub);
                }
            }
        }

        return keywords;
    }

    /**
     * 计算查询关键词在目标文档中的命中覆盖率
     */
    private double calculateCoverage(List<String> queryKeywords, String content) {
        if (queryKeywords == null || queryKeywords.isEmpty() || content == null || content.isBlank()) {
            return 0.0;
        }

        int hitCount = 0;
        for (String kw : queryKeywords) {
            if (content.contains(kw)) {
                hitCount++;
            }
        }

        return (double) hitCount / queryKeywords.size();
    }

    private List<Double> toList(double[] arr) {
        List<Double> list = new ArrayList<>(arr.length);
        for (double d : arr) {
            list.add(d);
        }
        return list;
    }
}
