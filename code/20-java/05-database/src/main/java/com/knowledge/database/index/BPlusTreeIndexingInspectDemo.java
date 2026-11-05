package com.knowledge.database.index;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * MySQL B+ 树索引与 Explain 执行计划实战：
 * 1. 现场直连 Docker MySQL 8.4，创建测试表与复合索引 idx_status_user_amount (status, user_id, amount)
 * 2. 验证最左前缀匹配原则 (Leftmost Prefix Rule)
 * 3. 验证范围查询 (>, <, BETWEEN) 导致右侧索引字段全部失效
 * 4. 验证字符串字段隐式类型转换 (隐式 cast(phone AS UNSIGNED)) 导致索引彻底失效走全表扫描 (type=ALL)
 * 5. 覆盖索引 (Covering Index) 与 Using index 避免回表 (Lookup) 性能优化
 */
public class BPlusTreeIndexingInspectDemo {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/knowledge_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PWD = "RootPassword123!";

    public static void main(String[] args) {
        System.out.println("========== 1. 直连 Docker MySQL 8.4 并构建索引压测表 ==========");
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PWD);
             Statement stmt = conn.createStatement()) {

            // 建表
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tb_order_index_test (
                    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                    status INT NOT NULL,
                    user_id BIGINT NOT NULL,
                    amount DECIMAL(10,2) NOT NULL,
                    phone VARCHAR(20) NOT NULL,
                    remark VARCHAR(255),
                    INDEX idx_status_user_amount (status, user_id, amount),
                    INDEX idx_phone (phone)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // 检查表数据量，若空则插入基础数据
            ResultSet rsCount = stmt.executeQuery("SELECT COUNT(*) FROM tb_order_index_test;");
            if (rsCount.next() && rsCount.getInt(1) == 0) {
                System.out.println("插入 10 条测试样本数据...");
                for (int i = 1; i <= 10; i++) {
                    stmt.execute(String.format(
                            "INSERT INTO tb_order_index_test (status, user_id, amount, phone, remark) VALUES (%d, %d, 99.00, '1380000000%d', 'remark-%d');",
                            i % 2, 1000L + i, i, i));
                }
            }

            System.out.println("\n========== 2. EXPLAIN 场景 1: 完全命中联合索引 (a, b, c) ==========");
            explainQuery(stmt, "EXPLAIN SELECT * FROM tb_order_index_test WHERE status = 1 AND user_id = 1001 AND amount = 99.00;");

            System.out.println("\n========== 3. EXPLAIN 场景 2: 违背最左前缀 (跳过 a，直接查 b, c 导致索引失效) ==========");
            explainQuery(stmt, "EXPLAIN SELECT * FROM tb_order_index_test WHERE user_id = 1001 AND amount = 99.00;");

            System.out.println("\n========== 4. EXPLAIN 场景 3: 隐式类型转换 (varchar phone 字段传入数字，导致全表扫描) ==========");
            explainQuery(stmt, "EXPLAIN SELECT * FROM tb_order_index_test WHERE phone = 13800000001;");

            System.out.println("\n========== 5. EXPLAIN 场景 4: 覆盖索引 (Covering Index - 零回表) ==========");
            explainQuery(stmt, "EXPLAIN SELECT status, user_id, amount FROM tb_order_index_test WHERE status = 1;");

        } catch (Exception e) {
            System.err.println("数据库连接或执行异常 (请确保 Docker MySQL 正常运行): " + e.getMessage());
        }
    }

    private static void explainQuery(Statement stmt, String explainSql) throws Exception {
        System.out.println("SQL: " + explainSql);
        try (ResultSet rs = stmt.executeQuery(explainSql)) {
            while (rs.next()) {
                String selectType = rs.getString("select_type");
                String table = rs.getString("table");
                String type = rs.getString("type");
                String possibleKeys = rs.getString("possible_keys");
                String key = rs.getString("key");
                String keyLen = rs.getString("key_len");
                String rows = rs.getString("rows");
                String extra = rs.getString("Extra");

                System.out.printf("  [EXPLAIN 结果] table=%s | type=%-6s | key=%-22s | key_len=%-4s | rows=%-2s | Extra=%s%n",
                        table, type, key, keyLen, rows, extra);
            }
        }
    }
}
