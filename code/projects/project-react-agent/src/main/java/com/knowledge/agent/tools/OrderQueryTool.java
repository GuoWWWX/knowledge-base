package com.knowledge.agent.tools;

import com.knowledge.agent.core.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 订单查询业务工具
 */
@Slf4j
@Component
public class OrderQueryTool implements AgentTool {

    private static final Pattern ORDER_ID_PATTERN = Pattern.compile("(\\d{4,})");

    /**
     * 模拟订单数据库
     */
    private final Map<String, OrderInfo> mockDatabase = new ConcurrentHashMap<>();

    public OrderQueryTool() {
        mockDatabase.put("1001", new OrderInfo("1001", "智能手环", "LOG-9527", "2026-10-28", "张三", 299.0));
        mockDatabase.put("1002", new OrderInfo("1002", "机械键盘", "LOG-8888", "2026-11-01", "李四", 499.0));
    }

    @Override
    public String getName() {
        return "OrderQueryTool";
    }

    @Override
    public String getDescription() {
        return "根据订单号查询订单详情（返回商品名称、物流单号、下单日期等信息）。入参格式为订单号，如: 1001";
    }

    @Override
    public String execute(String input) {
        log.info("【Tool执行】OrderQueryTool 接收到入参: '{}'", input);
        if (input == null || input.isBlank()) {
            return "错误: 订单号不能为空";
        }

        String orderId = input.trim();
        Matcher matcher = ORDER_ID_PATTERN.matcher(input);
        if (matcher.find()) {
            orderId = matcher.group(1);
        }

        OrderInfo info = mockDatabase.get(orderId);
        if (info == null) {
            return String.format("未查询到订单号为 [%s] 的订单记录", orderId);
        }

        return String.format("订单号: %s, 商品名称: %s, 物流单号: %s, 下单日期: %s, 下单用户: %s, 订单金额: %.2f元",
                info.orderId(), info.productName(), info.logisticsId(), info.orderDate(), info.userName(), info.price());
    }

    public record OrderInfo(
            String orderId,
            String productName,
            String logisticsId,
            String orderDate,
            String userName,
            double price
    ) {}
}
