package com.knowledge.ai.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================================
 * 实验：RAG 文档分块 (Chunking) 策略对比与评估
 * ============================================================================
 * 
 * <h3>核心原理与技术权衡 (Trade-offs)</h3>
 * 文档切分是 RAG 质量的"生命线"（Garbage In, Garbage Out）。切分粒度过大，会导致检索召回噪声过多、超出上下文窗口；
 * 切分粒度过小，则会导致语义破碎、丢失核心上下文。
 * 
 * 本演练对比三种业界核心切分算法：
 * <ol>
 *   <li><b>固定长度切分 (Fixed-size Chunking with Overlap)</b>：简单高效，但极易硬生生切断句子或代码块，造成语义损伤。</li>
 *   <li><b>标点符号断句聚合切分 (Sentence Boundary Chunking)</b>：按标点符号分句后动态合并，确保每句话的语法完整性。</li>
 *   <li><b>Markdown 层次结构切分 (Markdown Header Chunking)</b>：基于标题树提取小节，并在每个 Chunk 中注入父级标题路径上下文，彻底解决孤立片段语义漂移问题。</li>
 * </ol>
 * 
 * @author Knowledge Base Architecture Team
 * @version 1.0.0
 */
public class DocumentChunkingCompareDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("    RAG 核心实验：三大主流文档切分 (Chunking) 算法对比与上下文保持分析             ");
        System.out.println("================================================================================\n");

        // 待切分的样例技术文档
        String sampleDocument = """
                # 交易核心服务架构规范
                
                ## 1. 幂等性控制标准
                在分布式集群与网络抖动场景下，上游重试机制极易引发资金重复扣减与脏数据生成。
                所有涉及资产变更的写接口必须强制实现幂等控制。推荐方案为 Redis 分布式防重锁 + 数据库全局唯一业务流水号约束。
                防重 Key 必须包含业务场景码、用户标识与客户端请求序号，默认有效时间应当覆盖最大网络重试窗口。
                
                ## 2. 缓存一致性保障
                为了提升亿级用户并发读性能，系统普遍采用 Cache-Aside 旁路缓存模式。
                更新数据时的标准顺序必须为：先更新数据库底表，事务成功提交后再异步淘汰缓存（Cache Eviction）。
                严禁采用先淘汰缓存再更新数据库的反模式，否则在并发读写碰撞下将导致旧值回填并持久化驻留。
                若业务对最终一致性有严苛诉求，应接入 Canal 监听 MySQL Binlog 变更消息队列进行双重延时删除。
                """;

        System.out.println("【原始输入文档】(长度: " + sampleDocument.length() + " 字符)\n" + sampleDocument);
        System.out.println("--------------------------------------------------------------------------------\n");

        // ====================================================================
        // 策略 1：固定长度切分 (Fixed Size = 80, Overlap = 20)
        // ====================================================================
        System.out.println(">>> [策略 1] 固定长度滑动窗口切分 (Fixed-size Chunking: Size=80, Overlap=20)");
        List<Chunk> fixedChunks = chunkByFixedSize(sampleDocument, 80, 20);
        printChunks(fixedChunks);

        // ====================================================================
        // 策略 2：基于标点符号语法切分 (Sentence-based, Max Size = 100)
        // ====================================================================
        System.out.println(">>> [策略 2] 标点符号语法完整度分句切分 (Sentence Boundary Chunking: Max=100)");
        List<Chunk> sentenceChunks = chunkBySentence(sampleDocument, 100);
        printChunks(sentenceChunks);

        // ====================================================================
        // 策略 3：Markdown 层次结构切分 (Markdown Header Chunking)
        // ====================================================================
        System.out.println(">>> [策略 3] Markdown 语义层级切分 (Markdown Header Context Chunking)");
        List<Chunk> markdownChunks = chunkByMarkdownHeader(sampleDocument);
        printChunks(markdownChunks);

        // ====================================================================
        // 策略总结与对比分析
        // ====================================================================
        System.out.println("================================================================================");
        System.out.println("【文档分块策略选型技术白皮书】");
        System.out.println(" 1. 固定长度切分：实现简单、计算 O(N)，但极易在标点前或词语中间强行断开，语义完整性低；");
        System.out.println(" 2. 标点符号切分：保持自然语言句子语义完整，适合平铺直叙的非结构化大段文本；");
        System.out.println(" 3. Markdown 结构切分：结构化强，将父级章节上下文(Path)直接注入 Chunk，使向量召回自带语义定位，为企业级 RAG 首选！");
        System.out.println("================================================================================");
    }

    // ========================================================================
    // 分块算法 1：固定长度切分 (带重叠 Overlap)
    // ========================================================================
    public static List<Chunk> chunkByFixedSize(String text, int chunkSize, int overlap) {
        List<Chunk> chunks = new ArrayList<>();
        int step = chunkSize - overlap;
        int length = text.length();

        int chunkIndex = 1;
        for (int i = 0; i < length; i += step) {
            int end = Math.min(i + chunkSize, length);
            String content = text.substring(i, end).trim();
            if (!content.isEmpty()) {
                chunks.add(new Chunk(chunkIndex++, "FixedLength", content, Map.of("startOffset", i, "endOffset", end)));
            }
            if (end == length) {
                break;
            }
        }
        return chunks;
    }

    // ========================================================================
    // 分块算法 2：标点符号分句聚合切分
    // ========================================================================
    public static List<Chunk> chunkBySentence(String text, int maxChunkSize) {
        List<Chunk> chunks = new ArrayList<>();
        // 按照换行、句号、感叹号、问号切分为独立句子
        String[] rawSentences = text.split("(?<=[。！？\n])");
        
        StringBuilder currentChunk = new StringBuilder();
        int chunkIndex = 1;

        for (String sentence : rawSentences) {
            String trimmed = sentence.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            // 若当前累计长度 + 新句子长度超过单块上限，先将当前块封包
            if (currentChunk.length() + trimmed.length() > maxChunkSize && currentChunk.length() > 0) {
                chunks.add(new Chunk(chunkIndex++, "SentenceBoundary", currentChunk.toString(), Map.of("charCount", currentChunk.length())));
                currentChunk = new StringBuilder();
            }

            currentChunk.append(trimmed).append("\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(new Chunk(chunkIndex++, "SentenceBoundary", currentChunk.toString().trim(), Map.of("charCount", currentChunk.length())));
        }

        return chunks;
    }

    // ========================================================================
    // 分块算法 3：Markdown 标题层次上下文切分
    // ========================================================================
    public static List<Chunk> chunkByMarkdownHeader(String markdown) {
        List<Chunk> chunks = new ArrayList<>();
        String[] lines = markdown.split("\n");

        String h1 = "";
        String h2 = "";
        StringBuilder contentBuilder = new StringBuilder();
        int chunkIndex = 1;

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("# ")) {
                if (contentBuilder.length() > 0) {
                    flushMarkdownChunk(chunks, chunkIndex++, h1, h2, contentBuilder.toString());
                    contentBuilder = new StringBuilder();
                }
                h1 = trimmed.substring(2).trim();
                h2 = "";
            } else if (trimmed.startsWith("## ")) {
                if (contentBuilder.length() > 0) {
                    flushMarkdownChunk(chunks, chunkIndex++, h1, h2, contentBuilder.toString());
                    contentBuilder = new StringBuilder();
                }
                h2 = trimmed.substring(3).trim();
            } else {
                if (!trimmed.isEmpty()) {
                    contentBuilder.append(trimmed).append("\n");
                }
            }
        }

        if (contentBuilder.length() > 0) {
            flushMarkdownChunk(chunks, chunkIndex++, h1, h2, contentBuilder.toString());
        }

        return chunks;
    }

    private static void flushMarkdownChunk(List<Chunk> chunks, int index, String h1, String h2, String content) {
        String path = (h1.isEmpty() ? "" : h1) + (h2.isEmpty() ? "" : " > " + h2);
        // 上下文注入：将标题路径直接附加至切片首部，使向量检索时同时捕获层级关系
        String enrichedContent = "【所属章节: " + path + "】\n" + content.trim();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("h1", h1);
        metadata.put("h2", h2);
        metadata.put("contextPath", path);
        chunks.add(new Chunk(index, "MarkdownHeader", enrichedContent, metadata));
    }

    private static void printChunks(List<Chunk> chunks) {
        System.out.println("  总计切分为 " + chunks.size() + " 个片段 (Chunks):");
        for (Chunk c : chunks) {
            System.out.println("  ┌─ Chunk #" + c.getIndex() + " (" + c.getStrategy() + ") | 元数据: " + c.getMetadata());
            String[] lines = c.getContent().split("\n");
            for (String line : lines) {
                System.out.println("  │  " + line);
            }
            System.out.println("  └─────────────────────────────────────────────────────────────");
        }
        System.out.println();
    }

    // ========================================================================
    // 数据模型
    // ========================================================================

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chunk {
        private int index;
        private String strategy;
        private String content;
        private Map<String, Object> metadata;
    }
}
