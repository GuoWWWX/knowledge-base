package com.knowledge.agent.tools;

import com.knowledge.agent.core.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 计算器与日期差值工具
 */
@Slf4j
@Component
public class CalculatorTool implements AgentTool {

    private static final Pattern DATE_RANGE_PATTERN = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})\\s*[,至到\\-~]\\s*(\\d{4}-\\d{2}-\\d{2})");
    private static final Pattern MATH_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*([+\\-*/])\\s*(\\d+(\\.\\d+)?)");

    @Override
    public String getName() {
        return "CalculatorTool";
    }

    @Override
    public String getDescription() {
        return "通用计算工具。支持日期天数差计算（如 DAYS_BETWEEN(2026-10-30, 2026-11-03) 或 '2026-10-30 至 2026-11-03'）与基础四则数学运算（如 '500 - 150'）。";
    }

    @Override
    public String execute(String input) {
        log.info("【Tool执行】CalculatorTool 接收到入参: '{}'", input);
        if (input == null || input.isBlank()) {
            return "错误: 计算表达式不能为空";
        }

        // 1. 尝试匹配两日期求天数差
        Matcher dateMatcher = DATE_RANGE_PATTERN.matcher(input);
        if (dateMatcher.find()) {
            try {
                LocalDate date1 = LocalDate.parse(dateMatcher.group(1));
                LocalDate date2 = LocalDate.parse(dateMatcher.group(2));
                long days = Math.abs(ChronoUnit.DAYS.between(date1, date2));
                return days + " 天";
            } catch (Exception e) {
                return "日期解析计算失败: " + e.getMessage();
            }
        }

        // 2. 尝试匹配基础四则数学运算
        Matcher mathMatcher = MATH_PATTERN.matcher(input);
        if (mathMatcher.find()) {
            try {
                double num1 = Double.parseDouble(mathMatcher.group(1));
                String operator = mathMatcher.group(3);
                double num2 = Double.parseDouble(mathMatcher.group(4));
                double result = switch (operator) {
                    case "+" -> num1 + num2;
                    case "-" -> num1 - num2;
                    case "*" -> num1 * num2;
                    case "/" -> num2 == 0 ? Double.NaN : num1 / num2;
                    default -> 0.0;
                };
                return String.valueOf(result);
            } catch (Exception e) {
                return "数学运算失败: " + e.getMessage();
            }
        }

        return "无法识别计算表达式，支持日期差值与基础四则运算";
    }
}
