package com.knowledge.ai.context;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * ============================================================================
 * 实验：任务型多轮会话状态机 (FSM) 与动态槽位填充 (Slot Filling)
 * ============================================================================
 * 
 * <h3>架构核心思想与工业痛点</h3>
 * 在任务型对话（如智能客服订票、挂号、开户办卡）场景中，若完全依赖大模型进行多轮上下文自由发挥，
 * 极易产生"槽位丢失"、"前后矛盾"、"重复反问"或"未经确认擅自下单"等致命隐患。
 * 
 * 工业级方案普遍采用 <b>有限状态机 (FSM) + 槽位跟踪器 (Slot Tracker)</b> 作为骨架底座：
 * <ol>
 *   <li><b>确定性状态约束</b>：将会话严密划分为 INIT -> COLLECTING_SLOTS -> CONFIRMING -> BOOKED / CANCELLED 闭环状态。</li>
 *   <li><b>增量槽位填充 (Slot Filling)</b>：每轮对话仅让 LLM 执行精准的实体提取，由状态机核心比对已填槽位与缺失槽位。</li>
 *   <li><b>靶向引导反问</b>：一旦发现必填槽位缺失，精准驱动追问生成器，杜绝无限空转。</li>
 * </ol>
 * 
 * @author Knowledge Base Architecture Team
 * @version 1.0.0
 */
public class ConversationStateMachineDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("    多轮对话实验：有限状态机 (FSM) 驱动的任务型会话与槽位填充 (Slot Filling)     ");
        System.out.println("================================================================================\n");

        // 1. 初始化订票会话状态机上下文
        TicketBookingSession session = new TicketBookingSession();

        // 2. 模拟真实用户的多轮渐进式交互流程
        List<String> simulatedUserTurns = List.of(
                "我想订一张去北京的高铁票，请帮我看看。",
                "我打算从上海虹桥出发，时间是明天上午。",
                "对，信息完全准确，请帮我立即出票！"
        );

        int turnIndex = 1;
        for (String userUtterance : simulatedUserTurns) {
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("【第 " + turnIndex++ + " 轮交互】");
            System.out.println("用户输入: \"" + userUtterance + "\"");

            // 状态机处理当前轮次输入
            String botResponse = session.processTurn(userUtterance);

            // 打印状态追踪与槽位快照
            System.out.println("\n当前会话状态机快照:");
            System.out.println("  • 当前状态 (State): [" + session.getCurrentState() + "]");
            System.out.println("  • 槽位状态池 (Slots): " + session.getSlots());
            System.out.println("机器人应答:\n  \"" + botResponse + "\"");
        }

        System.out.println("\n================================================================================");
        System.out.println(" [实验结论] 状态机赋予大模型严谨的业务边界，使不确定的概率生成收敛于确定性业务闭环。");
        System.out.println("================================================================================");
    }

    // ========================================================================
    // 会话状态定义与槽位模型
    // ========================================================================

    public enum SessionState {
        INIT,              // 初始会话状态
        COLLECTING_SLOTS,  // 槽位收集与追问中
        CONFIRMING,        // 槽位齐全，等待用户终审确认
        COMPLETED,         // 业务履约成功完成
        CANCELLED          // 用户主动取消
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TicketSlots {
        private String origin;          // 出发地 (必填)
        private String destination;     // 目的地 (必填)
        private String travelDate;      // 出发日期 (必填)
        private Integer passengerCount; // 乘车人数 (必填，默认1人)

        public boolean isComplete() {
            return origin != null && !origin.isBlank()
                    && destination != null && !destination.isBlank()
                    && travelDate != null && !travelDate.isBlank();
        }

        public String getFirstMissingSlot() {
            if (destination == null || destination.isBlank()) return "destination";
            if (origin == null || origin.isBlank()) return "origin";
            if (travelDate == null || travelDate.isBlank()) return "travelDate";
            return null;
        }

        @Override
        public String toString() {
            return String.format("[出发地=%s, 目的地=%s, 出行日期=%s, 人数=%s人]",
                    origin == null ? "待填" : origin,
                    destination == null ? "待填" : destination,
                    travelDate == null ? "待填" : travelDate,
                    passengerCount == null ? "1" : passengerCount);
        }
    }

    // ========================================================================
    // 状态机会话管理器 (TicketBookingSession)
    // ========================================================================

    public static class TicketBookingSession {
        private SessionState currentState = SessionState.INIT;
        private final TicketSlots slots = new TicketSlots();

        public SessionState getCurrentState() {
            return currentState;
        }

        public TicketSlots getSlots() {
            return slots;
        }

        /**
         * 状态机核心轮次驱动方法
         */
        public String processTurn(String userText) {
            switch (currentState) {
                case INIT -> {
                    currentState = SessionState.COLLECTING_SLOTS;
                    extractSlotsFromNlu(userText);
                    return evaluateSlotsAndRespond();
                }
                case COLLECTING_SLOTS -> {
                    extractSlotsFromNlu(userText);
                    return evaluateSlotsAndRespond();
                }
                case CONFIRMING -> {
                    if (userText.contains("确认") || userText.contains("准") || userText.contains("对") || userText.contains("买")) {
                        currentState = SessionState.COMPLETED;
                        return String.format("🎉 出票成功！已为您锁定车次：【%s ➔ %s】，出行日期：%s，乘客 1 人。祝您旅途愉快！",
                                slots.getOrigin(), slots.getDestination(), slots.getTravelDate());
                    } else if (userText.contains("取消") || userText.contains("不要了")) {
                        currentState = SessionState.CANCELLED;
                        return "已为您取消本次出票行程，感谢您的使用。";
                    } else {
                        // 修正槽位或重试
                        extractSlotsFromNlu(userText);
                        return evaluateSlotsAndRespond();
                    }
                }
                case COMPLETED -> {
                    return "当前订单已完成出票，如需预订新的行程，请告诉我您的出发地和目的地。";
                }
                case CANCELLED -> {
                    return "当前会话已结束。若需重新预订，请重新发起提问。";
                }
                default -> throw new IllegalStateException("未知会话状态: " + currentState);
            }
        }

        /**
         * 评估当前槽位完整度，决定状态流转或靶向反问
         */
        private String evaluateSlotsAndRespond() {
            if (slots.isComplete()) {
                currentState = SessionState.CONFIRMING;
                return String.format("已为您收集齐车票信息：%s ➔ %s，出行时间：%s。请核对是否无误并回复【确认】以立即出票。",
                        slots.getOrigin(), slots.getDestination(), slots.getTravelDate());
            }

            // 槽位未齐全，检查第一个缺失项
            String missing = slots.getFirstMissingSlot();
            return switch (missing) {
                case "destination" -> "请问您计划前往哪个目的地城市？";
                case "origin" -> "已记录您的目的地为 " + slots.getDestination() + "，请问您打算从哪里出发？";
                case "travelDate" -> "好的，出发地为 " + slots.getOrigin() + "，目的地为 " + slots.getDestination() + "，请问您打算何时出行（如明天、后天、周五）？";
                default -> "请补充完整的出行信息。";
            };
        }

        /**
         * 模拟 LLM NLU 实体识别与槽位抽取组件
         */
        private void extractSlotsFromNlu(String text) {
            if (text.contains("北京")) {
                slots.setDestination("北京南站");
            }
            if (text.contains("上海") || text.contains("虹桥")) {
                slots.setOrigin("上海虹桥");
            }
            if (text.contains("明天")) {
                slots.setTravelDate("2026-11-04 (明天上午)");
            } else if (text.contains("下周一")) {
                slots.setTravelDate("2026-11-09 (下周一)");
            }
            if (slots.getPassengerCount() == null) {
                slots.setPassengerCount(1);
            }
        }
    }
}
