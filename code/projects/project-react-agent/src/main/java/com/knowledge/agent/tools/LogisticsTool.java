package com.knowledge.agent.tools;

import com.knowledge.agent.core.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 物流查询业务工具
 */
@Slf4j
@Component
public class LogisticsTool implements AgentTool {

    private static final Pattern LOGISTICS_ID_PATTERN = Pattern.compile("(LOG[-_]?\\d+)");

    private final Map<String, LogisticsRecord> mockDatabase = new ConcurrentHashMap<>();

    public LogisticsTool() {
        mockDatabase.put("LOG-9527", new LogisticsRecord("LOG-9527", "已发货", "2026-10-30", "北京转运中心", "顺丰速运"));
        mockDatabase.put("LOG-8888", new LogisticsRecord("LOG-8888", "运输中", "2026-11-02", "上海分拨中心", "京东物流"));
    }

    @Override
    public String getName() {
        return "LogisticsTool";
    }

    @Override
    public String getDescription() {
        return "根据物流单号查询物流轨迹与实时状态（返回物流状态、发货日期、当前节点与承运商）。入参格式为物流单号，如: LOG-9527";
    }

    @Override
    public String execute(String input) {
        log.info("【Tool执行】LogisticsTool 接收到入参: '{}'", input);
        if (input == null || input.isBlank()) {
            return "错误: 物流单号不能为空";
        }

        String logId = input.trim();
        Matcher matcher = LOGISTICS_ID_PATTERN.matcher(input);
        if (matcher.find()) {
            logId = matcher.group(1).replace("_", "-");
        }

        LogisticsRecord record = mockDatabase.get(logId);
        if (record == null) {
            return String.format("未检索到物流单号 [%s] 的物流记录", logId);
        }

        return String.format("物流单号: %s, 物流状态: %s, 发货日期: %s, 当前节点: %s, 承运商: %s",
                record.logisticsId(), record.status(), record.shipDate(), record.currentLocation(), record.carrier());
    }

    public record LogisticsRecord(
            String logisticsId,
            String status,
            String shipDate,
            String currentLocation,
            String carrier
    ) {}
}
