package com.knowledge.database.lock;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 实验：MySQL InnoDB 锁机制与死锁现场复现与排查
 * 对应知识点：行锁（Record Lock）、间隙锁（Gap Lock）、临键锁（Next-Key Lock）与死锁处理
 */
public class MySqlLockDeadlockInspectDemo {

    private static final String JDBC_URL = "jdbc:mysql://localhost:3306/knowledge_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai";
    private static final String USER = "root";
    private static final String PASS = "root123456";

    public static void main(String[] args) throws InterruptedException {
        System.out.println("================================================================================");
        System.out.println("【Lab 05-1】MySQL InnoDB 锁机制与死锁现场深度探秘");
        System.out.println("================================================================================\n");

        boolean connected = testConnection();
        if (connected) {
            System.out.println("✔ 成功检测到本地 Docker MySQL 8.4 容器，正在启动真实物理死锁碰撞实验...\n");
            runRealMySqlDeadlockExperiment();
        } else {
            System.out.println("⚠ 未检测到本地 MySQL (3306) 服务，正在启动自研锁引擎模拟死锁实验...\n");
            runSimulatedDeadlockExperiment();
        }

        printDeadlockDiagnosisManual();
    }

    private static boolean testConnection() {
        try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASS)) {
            return conn.isValid(2);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 真实 MySQL 死锁碰撞实测
     * 线程 1: 先更新 id=1，休眠 300ms，再请求更新 id=5
     * 线程 2: 先更新 id=5，休眠 300ms，再请求更新 id=1
     * 结果：InnoDB 锁等待图形成环，死锁检测器（Deadlock Detector）主动回滚 Undo 较小的事务！
     */
    private static void runRealMySqlDeadlockExperiment() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASS)) {
                conn.setAutoCommit(false);
                System.out.println("🔹 [事务 1] 开启，执行: UPDATE account_balance SET balance = balance + 10 WHERE id = 1 (加 X 锁)");
                try (PreparedStatement ps = conn.prepareStatement("UPDATE account_balance SET balance = balance + 10 WHERE id = 1")) {
                    ps.executeUpdate();
                }
                Thread.sleep(300);

                System.out.println("🔹 [事务 1] 请求加锁: UPDATE account_balance SET balance = balance - 10 WHERE id = 5 (等待事务2释放)...");
                try (PreparedStatement ps = conn.prepareStatement("UPDATE account_balance SET balance = balance - 10 WHERE id = 5")) {
                    ps.executeUpdate();
                }
                conn.commit();
                System.out.println("✔ [事务 1] 提交成功！");
            } catch (SQLException e) {
                System.err.println("❌ [事务 1] 捕获异常: " + e.getMessage() + " (SQLState: " + e.getSQLState() + ", ErrorCode: " + e.getErrorCode() + ")");
            } catch (InterruptedException ignored) {}
            finally {
                latch.countDown();
            }
        }, "Tx-1");

        Thread t2 = new Thread(() -> {
            try (Connection conn = DriverManager.getConnection(JDBC_URL, USER, PASS)) {
                conn.setAutoCommit(false);
                System.out.println("🔸 [事务 2] 开启，执行: UPDATE account_balance SET balance = balance + 20 WHERE id = 5 (加 X 锁)");
                try (PreparedStatement ps = conn.prepareStatement("UPDATE account_balance SET balance = balance + 20 WHERE id = 5")) {
                    ps.executeUpdate();
                }
                Thread.sleep(300);

                System.out.println("🔸 [事务 2] 请求加锁: UPDATE account_balance SET balance = balance - 20 WHERE id = 1 (等待事务1释放)...");
                try (PreparedStatement ps = conn.prepareStatement("UPDATE account_balance SET balance = balance - 20 WHERE id = 1")) {
                    ps.executeUpdate();
                }
                conn.commit();
                System.out.println("✔ [事务 2] 提交成功！");
            } catch (SQLException e) {
                System.err.println("❌ [事务 2] 捕获异常: " + e.getMessage() + " (SQLState: " + e.getSQLState() + ", ErrorCode: " + e.getErrorCode() + ")");
            } catch (InterruptedException ignored) {}
            finally {
                latch.countDown();
            }
        }, "Tx-2");

        t1.start();
        t2.start();
        latch.await(5, TimeUnit.SECONDS);
    }

    /**
     * 模拟死锁引擎
     */
    private static void runSimulatedDeadlockExperiment() throws InterruptedException {
        ReentrantLock lock1 = new ReentrantLock();
        ReentrantLock lock2 = new ReentrantLock();
        CountDownLatch latch = new CountDownLatch(2);

        Thread t1 = new Thread(() -> {
            lock1.lock();
            try {
                System.out.println("🔹 [模拟事务 1] 持有 Resource-1 锁，请求 Resource-2...");
                Thread.sleep(200);
                if (lock2.tryLock(500, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.println("✔ [模拟事务 1] 成功获取 Resource-2 锁！");
                    } finally {
                        lock2.unlock();
                    }
                } else {
                    System.err.println("❌ [模拟事务 1] 500ms 超时未拿到 Resource-2 锁，检测到死锁等待，主动回滚降级！");
                }
            } catch (InterruptedException ignored) {
            } finally {
                lock1.unlock();
                latch.countDown();
            }
        });

        Thread t2 = new Thread(() -> {
            lock2.lock();
            try {
                System.out.println("🔸 [模拟事务 2] 持有 Resource-2 锁，请求 Resource-1...");
                Thread.sleep(200);
                if (lock1.tryLock(500, TimeUnit.MILLISECONDS)) {
                    try {
                        System.out.println("✔ [模拟事务 2] 成功获取 Resource-1 锁！");
                    } finally {
                        lock1.unlock();
                    }
                } else {
                    System.err.println("❌ [模拟事务 2] 500ms 超时未拿到 Resource-1 锁，检测到死锁等待，主动回滚降级！");
                }
            } catch (InterruptedException ignored) {
            } finally {
                lock2.unlock();
                latch.countDown();
            }
        });

        t1.start();
        t2.start();
        latch.await();
    }

    private static void printDeadlockDiagnosisManual() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("【生产级排查指南】MySQL InnoDB 死锁分析与防御法则");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("1. 死锁日志分析命令：SHOW ENGINE INNODB STATUS; 查看 LATEST DETECTED DEADLOCK 块；");
        System.out.println("2. 核心三要素剖析：");
        System.out.println("   - 事务 1 持有的锁模式（lock_mode X locks rec but not gap）与等待的锁；");
        System.out.println("   - 事务 2 持有的锁模式与等待的锁（彼此形成等待环路）；");
        System.out.println("   - 被牺牲回滚的事务（WE ROLL BACK TRANSACTION (1)）；");
        System.out.println("3. 生产防死锁黄金四法则：");
        System.out.println("   ① 顺序加锁：多个业务更新涉及多张表或多行时，程序层面必须约定统一加锁顺序（如按主键升序排列更新）；");
        System.out.println("   ② 缩短事务链：大事务拆小，杜绝在事务内发起耗时的 RPC 或 HTTP 外部调用；");
        System.out.println("   ③ 合理索引：无索引查询会导致整表记录锁升级为全表扫描并锁全表 Next-Key Lock；");
        System.out.println("   ④ 间隙锁防范：在 RC（Read Committed）隔离级别下，默认不加间隙锁，可大幅度规避并发插入引起的死锁。\n");
    }
}
