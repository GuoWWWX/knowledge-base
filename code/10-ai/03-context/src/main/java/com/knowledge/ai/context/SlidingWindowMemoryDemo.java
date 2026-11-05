package com.knowledge.ai.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * ============================================================================
 * 实验：多轮对话上下文工程 —— 滑动窗口记忆与 Token 预算摘要压缩算法
 * ============================================================================
 * 
 * <h3>长对话上下文的核心痛点与工程治理</h3>
 * 随着用户与智能体交互轮次持续增加，对话历史迅速膨胀，面临双重严峻挑战：
 * <ul>
 *   <li><b>Token 窗口膨胀与费用失控</b>：每多携带一轮历史，后续每一次请求都需为之前的全部 Token 重复付费，造成二次方级别的计费放大；</li>
 *   <li><b>“迷失在中间”(Lost in the Middle)</b>：上下文越长，注意力权重越被稀释，模型极易遗忘早前制定的关键规则。</li>
 * </ul>
 * 
 * 本演练实现工业级对话记忆双轨治理算法：
 * <ol>
 *   <li><b>固定轮次滑动窗口 (Turn-based Window)</b>：截断最旧轮次，简单直观但容易丢失早期全局设定；</li>
 *   <li><b>Token 预算硬控制 + 摘要记忆压缩 (Summarization Compression)</b>：
 *       当对话超出预设 Token 预算时，将溢出的历史轮次提炼为简短的“前情摘要 (Summary)”，
 *       回贴至 System Prompt 中，使得关键背景永久留存，同时维持极低的 Token 消耗。</li>
 * </ol>
 * 
 * @author Knowledge Base Architecture Team
 * @version 1.0.0
 */
public class SlidingWindowMemoryDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("    上下文工程实验：滑动窗口记忆与 Token 预算摘要压缩 (Summarization) 演练       ");
        System.out.println("================================================================================\n");

        // 设定上下文硬性预算 (Token Budget) 为 180 Tokens (约 90 个汉字)
        int tokenBudget = 180;
        System.out.println("【系统上下文策略配置】: 严格 Token 预算上限 = " + tokenBudget + " Tokens\n");

        // 初始化支持摘要压缩的智能记忆管理器
        AdaptiveSummaryMemoryManager memoryManager = new AdaptiveSummaryMemoryManager(
                "你是一位专业的 Java 架构咨询顾问，服务于大型金融系统。",
                tokenBudget
        );

        // 模拟多轮长对话
        List<MessagePair> turns = List.of(
                new MessagePair("你好，我们是一家全国性股份制银行的核心账务研发团队，当前面临高并发挑战。",
                        "您好！很高兴为您服务。核心账务系统涉及强一致性与高资金安全，请问目前主要的技术痛点是什么？"),
                new MessagePair("我们在日终跑批与双十一还款高峰期，数据库经常发生严重行锁争用与死锁超时。",
                        "核心账务热点账户的行锁冲突是典型架构瓶颈。通常建议采用账户分级、子账户分摊记账与异步批量汇总的降频方案。"),
                new MessagePair("我们希望在本次改造中引入 Redis 分布式缓存来抵挡读压力，但担心数据脏读。",
                        "账务系统引入缓存需极其审慎。原则上禁止对热点余额采用 Cache-Aside 纯异步回写，应优先考虑基于 Binlog 的严格单向事件驱动一致性保证。"),
                new MessagePair("请根据我们团队的背景和业务特性，给出一份简要的三阶段落地路线图。",
                        "没问题，基于贵行核心账务的特点，三阶段路线建议如下：1.热点账户拆分；2.Binlog一致性同步；3.日终跑批分段对账。")
        );

        for (int i = 0; i < turns.size(); i++) {
            MessagePair turn = turns.get(i);
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println(">>> 注入第 " + (i + 1) + " 轮对话：");
            System.out.println("  User: \"" + turn.userText() + "\"");
            System.out.println("  AI:   \"" + turn.assistantText() + "\"");

            memoryManager.addTurn(turn.userText(), turn.assistantText());

            // 打印当前内存管理状态
            memoryManager.printMemorySnapshot();
        }

        System.out.println("\n================================================================================");
        System.out.println(" [实验结论] 摘要压缩算法将线性膨胀的历史上下文固化为常数级别的紧凑前情提要，");
        System.out.println("            在有限的 Token 预算下，兼顾长期记忆保持与极低计费成本！");
        System.out.println("================================================================================");
    }

    public record MessagePair(String userText, String assistantText) {}

    // ========================================================================
    // 自适应摘要压缩记忆管理器 (AdaptiveSummaryMemoryManager)
    // ========================================================================

    public static class AdaptiveSummaryMemoryManager {
        private final String baseSystemPrompt;
        private final int maxTokenBudget;
        private String rollingSummary = ""; // 滚动摘要（前情提要）
        private final List<ChatMessage> recentMessages = new ArrayList<>();

        public AdaptiveSummaryMemoryManager(String baseSystemPrompt, int maxTokenBudget) {
            this.baseSystemPrompt = baseSystemPrompt;
            this.maxTokenBudget = maxTokenBudget;
        }

        public synchronized void addTurn(String userText, String assistantText) {
            recentMessages.add(new ChatMessage("user", userText));
            recentMessages.add(new ChatMessage("assistant", assistantText));

            // 检查当前总 Token 是否超出预算，若超出则触发摘要压缩驱动
            compactIfNecessary();
        }

        /**
         * 动态压缩检查与旧消息转摘要算法
         */
        private void compactIfNecessary() {
            while (calculateTotalTokens() > maxTokenBudget && recentMessages.size() > 2) {
                // 弹出最早的一轮对话 (User + Assistant)
                ChatMessage oldestUser = recentMessages.remove(0);
                ChatMessage oldestAssistant = recentMessages.remove(0);

                // 启动模拟摘要器：将弹出的旧轮次与当前已有摘要进行增量提炼
                this.rollingSummary = summarizeIncrementally(this.rollingSummary, oldestUser.getContent(), oldestAssistant.getContent());
                System.out.println("  [⚡ 触发记忆压缩] 对话超出预算，最旧轮次已被提炼为紧凑摘要：\"" + rollingSummary + "\"");
            }
        }

        private String summarizeIncrementally(String currentSummary, String user, String assistant) {
            if (user.contains("股份制银行") || user.contains("核心账务")) {
                return "用户为全国性股份制银行核心账务团队，系统在日终及高峰面临热点账户行锁争用与高并发瓶颈。";
            } else if (user.contains("Redis") || user.contains("脏读")) {
                return (currentSummary.isEmpty() ? "" : currentSummary + " ") +
                        "团队计划引入缓存但担忧脏读，顾问建议禁止纯异步回写并审慎引入 Binlog 一致性同步。";
            }
            return currentSummary + " (已完成相关技术阶段确认)";
        }

        public int calculateTotalTokens() {
            int tokens = estimateTokens(baseSystemPrompt);
            if (!rollingSummary.isEmpty()) {
                tokens += estimateTokens("【历史会话前情提要】: " + rollingSummary);
            }
            for (ChatMessage msg : recentMessages) {
                tokens += estimateTokens(msg.getContent());
            }
            return tokens;
        }

        /**
         * 估算中英文字符的 Token 消耗 (中文通常 1 个汉字约 1.2~1.8 个 Token，此处采用经验近似)
         */
        private int estimateTokens(String text) {
            if (text == null) return 0;
            return (int) Math.ceil(text.length() * 1.5);
        }

        public void printMemorySnapshot() {
            System.out.println("\n  【上下文记忆池快照 (Context Window Snapshot)】");
            System.out.println("  ├─ 基础 System 提示: \"" + baseSystemPrompt + "\"");
            System.out.println("  ├─ 滚动提炼摘要 (Summary): " + (rollingSummary.isEmpty() ? "<暂无摘要，当前轮次充足>" : "\"" + rollingSummary + "\""));
            System.out.println("  ├─ 活跃最近消息数: " + recentMessages.size() + " 条 (" + (recentMessages.size() / 2) + " 轮)");
            for (int i = 0; i < recentMessages.size(); i++) {
                ChatMessage m = recentMessages.get(i);
                System.out.println("  │   #" + (i + 1) + " [" + m.getRole() + "]: " + m.getContent());
            }
            int currentTokens = calculateTotalTokens();
            System.out.printf("  └─ 当前总 Token 消耗: %d / %d (利用率: %.1f%%)\n",
                    currentTokens, maxTokenBudget, (currentTokens * 100.0 / maxTokenBudget));
        }
    }

    @Data
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
