package com.knowledge.ai.framework;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * ============================================================================
 * 实验：Spring AI ChatClient Fluent API 设计与链式调用原理
 * ============================================================================
 * 
 * <h3>核心背景与架构思想</h3>
 * 传统大模型 SDK 通常采用臃肿的请求/响应 POJO 包装，参数传递碎片化且容易出错。
 * Spring AI 引入了类似于 {@code WebClient} 与 {@code RestClient} 的现代 Fluent API 规范 —— {@code ChatClient}：
 * <ul>
 *   <li><b>声明式链式编排</b>：通过 {@code prompt().system(...).user(...).advisors(...).call().content()} 实现自解释的调用流。</li>
 *   <li><b>动态模板注入</b>：支持 Prompt 模板化与占位符动态绑定。</li>
 *   <li><b>统一响应抽象</b>：将厂商各异的返回结构统一抽象为 {@code ChatResponse}、{@code Generation} 和 {@code Usage}。</li>
 *   <li><b>双模态输出</b>：一套链式结构同时支持同步阻塞调用 ({@code call()}) 与流式响应 ({@code stream()})。</li>
 * </ul>
 * 
 * 本演练以精巧且生产级结构完整的模拟内核，完整复刻 Spring AI ChatClient 的核心链路与流式体验。
 * 
 * @author Knowledge Base Architecture Team
 * @version 1.0.0
 */
public class ChatClientFluentApiDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("    Spring AI 核心设计实验：ChatClient Fluent API 链式调用与响应架构演示          ");
        System.out.println("================================================================================\n");

        // 1. 初始化模拟 ChatModel (底层可接入 OpenAI, Ollama, DeepSeek 等)
        SimulatedChatModel chatModel = new SimulatedChatModel();

        // 2. 构建类似 Spring AI 的 ChatClient 客户端
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一位资深的分布式系统与 Java 架构专家，擅长使用简洁严谨的语言回答技术问题。")
                .build();

        // ====================================================================
        // 场景 1：基础链式调用 (System + User Prompt -> call() -> content())
        // ====================================================================
        System.out.println(">>> [场景 1] 基础链式调用获取文本内容 (call().content())");
        String responseContent = chatClient.prompt()
                .user("请简要说明虚拟线程 (Virtual Threads) 对高并发 I/O 密集型服务带来的核心收益。")
                .call()
                .content();
        System.out.println("AI 响应内容:\n" + responseContent);
        System.out.println("\n--------------------------------------------------------------------------------\n");

        // ====================================================================
        // 场景 2：Prompt 参数化模板填充 (Template & Params)
        // ====================================================================
        System.out.println(">>> [场景 2] 动态模板占位符渲染与元数据获取 (Template + call().chatResponse())");
        ChatResponse fullResponse = chatClient.prompt()
                .user(u -> u.template("请为 {role} 角色设计一份关于 {topic} 的技术面试评估维度，重点考察 {focus}。")
                        .param("role", "P7 级别后端架构师")
                        .param("topic", "高可用与容量保障")
                        .param("focus", "缓存穿透/击穿/雪崩及分布式锁边界"))
                .call()
                .chatResponse();

        System.out.println("AI 完整响应 (Generation):\n" + fullResponse.getResult().getOutput().getContent());
        System.out.println("\n响应元数据统计 (Metadata & Token Usage):");
        System.out.println("  - 模型名称 (Model): " + fullResponse.getMetadata().getModelName());
        System.out.println("  - 输入 Tokens (Prompt Tokens): " + fullResponse.getMetadata().getUsage().promptTokens());
        System.out.println("  - 输出 Tokens (Generation Tokens): " + fullResponse.getMetadata().getUsage().generationTokens());
        System.out.println("  - 耗时统计 (Latency): " + fullResponse.getMetadata().getLatencyMs() + " ms");
        System.out.println("\n--------------------------------------------------------------------------------\n");

        // ====================================================================
        // 场景 3：流式输出体验 (stream() 模拟逐 Token 打字机效果)
        // ====================================================================
        System.out.println(">>> [场景 3] 响应式流式输出体验 (stream() 流式推送)");
        System.out.print("AI 流式输出: ");
        chatClient.prompt()
                .user("用一首四言绝句总结 Java 21 带来的划时代演进。")
                .stream()
                .subscribe(chunk -> {
                    System.out.print(chunk);
                    try {
                        TimeUnit.MILLISECONDS.sleep(60); // 模拟网络流式返回的逐字打字机延迟
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
        System.out.println("\n\n================================================================================");
        System.out.println(" [实验结论] ChatClient 消除厂商 SDK 异构性，提供声明式、类型安全、可插拔的最高层交互抽象。");
        System.out.println("================================================================================");
    }

    // ========================================================================
    // Spring AI 核心抽象轻量复刻体系 (架构与接口定义)
    // ========================================================================

    /**
     * ChatClient 构建入口与核心门面
     */
    public static class ChatClient {
        private final SimulatedChatModel chatModel;
        private final String defaultSystemText;

        private ChatClient(SimulatedChatModel chatModel, String defaultSystemText) {
            this.chatModel = chatModel;
            this.defaultSystemText = defaultSystemText;
        }

        public static Builder builder(SimulatedChatModel chatModel) {
            return new Builder(chatModel);
        }

        public PromptSpec prompt() {
            return new PromptSpec(this.chatModel, this.defaultSystemText);
        }

        public static class Builder {
            private final SimulatedChatModel chatModel;
            private String defaultSystemText = "";

            public Builder(SimulatedChatModel chatModel) {
                this.chatModel = Objects.requireNonNull(chatModel, "ChatModel 不能为空");
            }

            public Builder defaultSystem(String defaultSystemText) {
                this.defaultSystemText = defaultSystemText;
                return this;
            }

            public ChatClient build() {
                return new ChatClient(this.chatModel, this.defaultSystemText);
            }
        }
    }

    /**
     * 链式 Prompt 规格规范 (PromptSpec)
     */
    public static class PromptSpec {
        private final SimulatedChatModel chatModel;
        private String systemText;
        private String userText;

        public PromptSpec(SimulatedChatModel chatModel, String defaultSystemText) {
            this.chatModel = chatModel;
            this.systemText = defaultSystemText;
        }

        public PromptSpec system(String systemText) {
            this.systemText = systemText;
            return this;
        }

        public PromptSpec user(String userText) {
            this.userText = userText;
            return this;
        }

        public PromptSpec user(Consumer<PromptUserSpec> userSpecConsumer) {
            PromptUserSpec spec = new PromptUserSpec();
            userSpecConsumer.accept(spec);
            this.userText = spec.render();
            return this;
        }

        public CallResponseSpec call() {
            return new CallResponseSpec(chatModel, systemText, userText);
        }

        public StreamResponseSpec stream() {
            return new StreamResponseSpec(chatModel, systemText, userText);
        }
    }

    /**
     * 用户 Prompt 动态模板与参数规格
     */
    public static class PromptUserSpec {
        private String template;
        private final Map<String, Object> params = new LinkedHashMap<>();

        public PromptUserSpec template(String template) {
            this.template = template;
            return this;
        }

        public PromptUserSpec param(String key, Object value) {
            this.params.put(key, value);
            return this;
        }

        public String render() {
            if (template == null) {
                return "";
            }
            String result = template;
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
            }
            return result;
        }
    }

    /**
     * 同步调用返回规格 (CallResponseSpec)
     */
    public static class CallResponseSpec {
        private final SimulatedChatModel chatModel;
        private final String systemText;
        private final String userText;

        public CallResponseSpec(SimulatedChatModel chatModel, String systemText, String userText) {
            this.chatModel = chatModel;
            this.systemText = systemText;
            this.userText = userText;
        }

        public String content() {
            return chatResponse().getResult().getOutput().getContent();
        }

        public ChatResponse chatResponse() {
            return chatModel.call(new PromptRequest(systemText, userText));
        }
    }

    /**
     * 流式调用返回规格 (StreamResponseSpec)
     */
    public static class StreamResponseSpec {
        private final SimulatedChatModel chatModel;
        private final String systemText;
        private final String userText;

        public StreamResponseSpec(SimulatedChatModel chatModel, String systemText, String userText) {
            this.chatModel = chatModel;
            this.systemText = systemText;
            this.userText = userText;
        }

        public void subscribe(Consumer<String> chunkConsumer) {
            chatModel.stream(new PromptRequest(systemText, userText), chunkConsumer);
        }
    }

    // ========================================================================
    // 数据模型实体 (DTO / Record)
    // ========================================================================

    public record PromptRequest(String systemMessage, String userMessage) {}

    public record Usage(int promptTokens, int generationTokens, int totalTokens) {}

    @Getter
    @Builder
    @ToString
    public static class ChatResponseMetadata {
        private final String modelName;
        private final Usage usage;
        private final long latencyMs;
        private final LocalDateTime timestamp;
    }

    @Getter
    @Builder
    @ToString
    public static class AssistantMessage {
        private final String content;
    }

    @Getter
    @Builder
    @ToString
    public static class Generation {
        private final AssistantMessage output;
    }

    @Getter
    @Builder
    @ToString
    public static class ChatResponse {
        private final Generation result;
        private final ChatResponseMetadata metadata;
    }

    // ========================================================================
    // 模拟底座大模型 (SimulatedChatModel)
    // ========================================================================

    public static class SimulatedChatModel {
        private static final String MODEL_NAME = "deepseek-v3-simulated";

        public ChatResponse call(PromptRequest request) {
            long start = System.currentTimeMillis();
            String responseText;

            if (request.userMessage().contains("虚拟线程")) {
                responseText = "1. 百万级轻量协程：降低线程栈空间消耗(从数 MB 降至几百字节)。\n" +
                        "2. 阻塞即让出载体线程：在 Socket I/O 等待时自动 unpark，由少量的 Carrier 线程承载数十万吞吐。\n" +
                        "3. 保持同步直观心智：彻底告别 Reactive 响应式地狱(Mono/Flux/回调链)，回归传统简洁的 try-catch 同步代码风格。";
            } else if (request.userMessage().contains("面试评估")) {
                responseText = "【P7 架构师高可用保障面试评估框架】：\n" +
                        "  1. 缓存穿透防御：布隆过滤器(BloomFilter)前置拦截、空值缓存兜底与异步重建。\n" +
                        "  2. 缓存击穿与雪崩：热点数据逻辑过期机制、互斥分布式锁(Redisson 分段加锁)、TTL 加随机扰动因子打散。\n" +
                        "  3. 分布式锁边界：锁超时看门狗续期(Watchdog)、业务事务与锁释放的一致性先后顺序、可重入与安全释放(Lua 脚本校验)。";
            } else {
                responseText = "已收到系统指令与用户问题，基于架构标准生成专业解答。";
            }

            long latency = Math.max(12, System.currentTimeMillis() - start + 45);
            int promptTokens = (request.systemMessage().length() + request.userMessage().length()) / 2;
            int completionTokens = responseText.length() / 2;

            return ChatResponse.builder()
                    .result(Generation.builder()
                            .output(AssistantMessage.builder().content(responseText).build())
                            .build())
                    .metadata(ChatResponseMetadata.builder()
                            .modelName(MODEL_NAME)
                            .latencyMs(latency)
                            .timestamp(LocalDateTime.now())
                            .usage(new Usage(promptTokens, completionTokens, promptTokens + completionTokens))
                            .build())
                    .build();
        }

        public void stream(PromptRequest request, Consumer<String> chunkConsumer) {
            String fullText = "Java二十一，\n虚程撼九级。\n模式守恒策，\n架构铸新基。";
            for (char ch : fullText.toCharArray()) {
                chunkConsumer.accept(String.valueOf(ch));
            }
        }
    }
}
