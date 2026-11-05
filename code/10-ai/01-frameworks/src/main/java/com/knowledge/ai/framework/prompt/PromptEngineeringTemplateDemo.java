package com.knowledge.ai.framework.prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 现代 Prompt 工程深度实战：
 * 1. PromptTemplate 动态变量占位符安全替换 ({variable})
 * 2. Few-Shot (少样本示例) 动态拼接注入技术
 * 3. Chain-of-Thought (CoT 思维链) 引导模型分步严密推理
 * 4. 结构化 JSON Schema 约束指令生成
 */
public class PromptEngineeringTemplateDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. PromptTemplate 动态变量替换引擎 ==========");
        PromptTemplate template = new PromptTemplate("""
            你是一位高级资深 Java 后端架构师。
            请分析以下异常信息并给出修复方案：
            【服务名】: {serviceName}
            【异常类型】: {exceptionType}
            【堆栈信息】: {stackTrace}
            请严格按照 Markdown 格式输出分析与规避建议。
            """);

        Map<String, String> variables = Map.of(
                "serviceName", "payment-service",
                "exceptionType", "CannotAcquireLockException",
                "stackTrace", "Deadlock found when trying to get lock; try restarting transaction"
        );
        String renderedPrompt = template.render(variables);
        System.out.println("渲染后的最终 Prompt:\n" + renderedPrompt);

        System.out.println("\n========== 2. Few-Shot (少样本示例) 动态组装 ==========");
        FewShotPromptBuilder fewShotBuilder = new FewShotPromptBuilder("判断用户评论的情感倾向（POSITIVE, NEGATIVE, NEUTRAL）：");
        fewShotBuilder.addExample("这款软件启动极其迅速，界面简洁直观！", "POSITIVE");
        fewShotBuilder.addExample("更新后闪退频繁，而且客服根本不理人。", "NEGATIVE");
        fewShotBuilder.addExample("今天收到了快递包裹。", "NEUTRAL");

        String fewShotPrompt = fewShotBuilder.build("虽然有些小bug，但客服响应很快并积极修复了，总体满意。");
        System.out.println(fewShotPrompt);

        System.out.println("\n========== 3. Chain-of-Thought (CoT 思维链) 引导模板 ==========");
        String cotPrompt = buildCoTPrompt("小明有 5 个苹果，他吃了 2 个，又买了 3 箱苹果，每箱有 6 个，然后分给小红一半。请问小明还剩多少个苹果？");
        System.out.println(cotPrompt);
    }

    public static class PromptTemplate {
        private final String template;
        private static final Pattern PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

        public PromptTemplate(String template) {
            this.template = template;
        }

        public String render(Map<String, String> model) {
            Matcher matcher = PATTERN.matcher(template);
            StringBuilder sb = new StringBuilder();
            while (matcher.find()) {
                String key = matcher.group(1);
                String val = model.getOrDefault(key, "{" + key + "}");
                matcher.appendReplacement(sb, Matcher.quoteReplacement(val));
            }
            matcher.appendTail(sb);
            return sb.toString();
        }
    }

    public static class FewShotPromptBuilder {
        private final String instruction;
        private final List<Example> examples = new ArrayList<>();

        public FewShotPromptBuilder(String instruction) {
            this.instruction = instruction;
        }

        public void addExample(String input, String output) {
            examples.add(new Example(input, output));
        }

        public String build(String userInput) {
            StringBuilder sb = new StringBuilder();
            sb.append(instruction).append("\n\n");
            for (int i = 0; i < examples.size(); i++) {
                Example ex = examples.get(i);
                sb.append("示例 ").append(i + 1).append(":\n");
                sb.append("输入: ").append(ex.input).append("\n");
                sb.append("输出: ").append(ex.output).append("\n\n");
            }
            sb.append("请处理以下输入:\n输入: ").append(userInput).append("\n输出: ");
            return sb.toString();
        }

        record Example(String input, String output) {}
    }

    private static String buildCoTPrompt(String question) {
        return """
            【问题】: %s
            【指令】:
            请利用思维链 (Chain-of-Thought) 逐步推理该数学应用题。
            思考步骤要求：
            步骤 1: 厘清初始数量与第一次减少量；
            步骤 2: 计算新增批量数量；
            步骤 3: 计算此时持有总数；
            步骤 4: 执行分配并得出最终答案。
            最终在最后一行以 ### 最终答案: [数值] ### 输出。
            """.formatted(question);
    }
}
