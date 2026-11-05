package com.knowledge.database.sharding;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * 读写分离与分库分表核心架构实战：
 * 1. 基于 ThreadLocal 的读写分离数据源路由键上下文 (Master 写 / Slave 读)
 * 2. 精准分表哈希算法：用户 ID 散列到 4 库 16 表 (库哈希、表哈希公式推导)
 * 3. 一致性哈希算法 (Consistent Hashing) 带虚拟节点模拟：扩容时仅迁移 1/N 数据，避免雪崩再平衡
 */
public class ShardingReadWriteRoutingDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. ThreadLocal 读写分离动态数据源路由演示 ==========");
        simulateReadWriteRouting();

        System.out.println("\n========== 2. 4 库 16 表精准哈希分片推导演练 ==========");
        simulateDatabaseTableSharding();

        System.out.println("\n========== 3. 一致性哈希虚拟节点分布与节点扩容平滑迁移 ==========");
        simulateConsistentHashing();
    }

    private static void simulateReadWriteRouting() {
        // 模拟写请求：订单支付
        DataSourceContextHolder.setWriteMaster();
        System.out.printf("当前事务路由数据源: %s (写操作必走主库以防主从同步延迟脏读)%n", DataSourceContextHolder.getDataSourceKey());
        DataSourceContextHolder.clear();

        // 模拟只读请求：个人主页查询
        DataSourceContextHolder.setReadOnlySlave();
        System.out.printf("当前事务路由数据源: %s (读操作负载均衡至只读副本从库)%n", DataSourceContextHolder.getDataSourceKey());
        DataSourceContextHolder.clear();
    }

    private static void simulateDatabaseTableSharding() {
        int dbCount = 4;
        int tableCountPerDb = 4;
        int totalTables = dbCount * tableCountPerDb; // 16 表

        long[] sampleUserIds = {10001L, 10002L, 10003L, 10088L, 20099L, 99999L};
        for (long userId : sampleUserIds) {
            // 分库键路由：根据 userId 哈希值先定位库，再定位表
            int dbIndex = (int) ((userId / tableCountPerDb) % dbCount);
            int tableIndex = (int) (userId % totalTables);

            System.out.printf("用户 user_id=%-5d -> 路由目标: [db_%d].[t_order_%02d]%n",
                    userId, dbIndex, tableIndex);
        }
    }

    private static void simulateConsistentHashing() {
        ConsistentHashRouter router = new ConsistentHashRouter(100); // 每个物理节点对应 100 个虚拟节点
        router.addNode("DB_Node_A");
        router.addNode("DB_Node_B");
        router.addNode("DB_Node_C");

        String[] keys = {"ORDER_1001", "ORDER_1002", "ORDER_1003", "ORDER_1004", "ORDER_1005"};
        System.out.println("【扩容前】节点路由结果:");
        Map<String, String> originalRouting = new HashMap<>();
        for (String key : keys) {
            String node = router.route(key);
            originalRouting.put(key, node);
            System.out.printf("  Key [%-10s] -> %s%n", key, node);
        }

        System.out.println("【平滑扩容】新增物理节点 DB_Node_D...");
        router.addNode("DB_Node_D");

        System.out.println("【扩容后】重新路由与数据迁移检测:");
        int migrated = 0;
        for (String key : keys) {
            String newNode = router.route(key);
            boolean changed = !newNode.equals(originalRouting.get(key));
            if (changed) migrated++;
            System.out.printf("  Key [%-10s] -> %-10s %s%n",
                    key, newNode, (changed ? "★ (平滑迁移至新节点)" : "(保持不变)"));
        }
        System.out.printf("迁移数据占比: %d/%d (仅约 1/N 的数据发生迁移，避免传统取模全量搬迁！)%n",
                migrated, keys.length);
    }

    // ThreadLocal 上下文管理器
    public static class DataSourceContextHolder {
        private static final ThreadLocal<String> CONTEXT = ThreadLocal.withInitial(() -> "MASTER");

        public static void setWriteMaster() { CONTEXT.set("MASTER"); }
        public static void setReadOnlySlave() { CONTEXT.set("SLAVE_" + (System.currentTimeMillis() % 2 + 1)); }
        public static String getDataSourceKey() { return CONTEXT.get(); }
        public static void clear() { CONTEXT.remove(); }
    }

    // 一致性哈希环
    static class ConsistentHashRouter {
        private final int virtualNodes;
        private final SortedMap<Integer, String> ring = new TreeMap<>();

        public ConsistentHashRouter(int virtualNodes) {
            this.virtualNodes = virtualNodes;
        }

        public void addNode(String node) {
            for (int i = 0; i < virtualNodes; i++) {
                int hash = (node + "#VN" + i).hashCode();
                ring.put(hash, node);
            }
        }

        public String route(String key) {
            if (ring.isEmpty()) return null;
            int hash = key.hashCode();
            if (!ring.containsKey(hash)) {
                SortedMap<Integer, String> tailMap = ring.tailMap(hash);
                hash = tailMap.isEmpty() ? ring.firstKey() : tailMap.firstKey();
            }
            return ring.get(hash);
        }
    }
}
