package com.knowledge.juc.threadlocal;

import java.util.concurrent.*;

/**
 * ThreadLocal 线程池复用下的脏数据串号与内存泄漏机理演示
 *
 * 核心原理深入剖析：
 * 1. 为什么会脏数据串号？
 *    - Web 容器（Tomcat）或自定义业务线程池均基于“线程复用”模型；
 *    - 线程在执行完前一个请求后并不销毁，其内部维护的 ThreadLocalMap 依然保留着历史数据；
 *    - 若前置请求未显式调用 remove()，后置未认证请求便会意外读到前者的敏感信息。
 *
 * 2. 为什么会内存泄漏？
 *    - ThreadLocalMap.Entry 结构：static class Entry extends WeakReference<ThreadLocal<?>> { Object value; }
 *    - Key 是弱引用：当外部 ThreadLocal 强引用断开后，发生 GC 时 Key 会被回收变为 null；
 *    - Value 是强引用：存在引用链 Current Thread -> ThreadLocalMap -> Entry -> Value；
 *    - 只要线程池中的工作线程不退出，Value 对象就永远无法被垃圾回收器回收，从而造成内存泄漏！
 */
public class ThreadLocalLeakSimulationDemo {

    // 存储当前用户上下文会话信息
    private static final ThreadLocal<String> currentUserContext = new ThreadLocal<>();

    // 存储模拟大对象以演示内存占用
    private static final ThreadLocal<byte[]> bigPayloadContext = new ThreadLocal<>();

    public static void main(String[] args) throws Exception {
        System.out.println("===============================================================");
        System.out.println("   ThreadLocal 线程池复用场景下的脏数据串号与内存泄漏模拟验证   ");
        System.out.println("===============================================================\n");

        // 仅使用单线程池，100% 保证后续任务复用同一线程
        ExecutorService singleWorkerPool = Executors.newSingleThreadExecutor(r -> new Thread(r, "pool-worker-reused"));

        // 1. 模拟脏数据串号现象
        testDirtyDataPollution(singleWorkerPool);

        TimeUnit.MILLISECONDS.sleep(500);

        // 2. 模拟内存泄漏风险剖析
        testMemoryLeakRisk();

        TimeUnit.MILLISECONDS.sleep(500);

        // 3. 正确规范的最佳实践：try-finally remove 规避
        testSafePracticeWithFinally(singleWorkerPool);

        singleWorkerPool.shutdown();
    }

    /**
     * 场景 1：未调用 remove() 导致的脏数据串号严重事故
     */
    private static void testDirtyDataPollution(ExecutorService pool) throws Exception {
        System.out.println("---------------------------------------------------------------");
        System.out.println("【事故现场 1】未调用 remove() 导致的用户身份串号：");

        // 请求 A：VIP 用户 Alice 登录，业务处理完毕后忘记清理
        pool.submit(() -> {
            String threadName = Thread.currentThread().getName();
            currentUserContext.set("VIP_User_Alice (ID: 88888)");
            System.out.printf("  [请求 A 处理完毕] 线程 [%s] 设置用户上下文: [%s]，【遗漏调用 remove()】\n",
                    threadName, currentUserContext.get());
        }).get();

        // 请求 B：未登录的匿名访客发起请求，理论上应为 null
        pool.submit(() -> {
            String threadName = Thread.currentThread().getName();
            String context = currentUserContext.get();
            System.out.printf("  [请求 B 处理中...] 线程 [%s] 本地未设置身份，尝试读取上下文...\n", threadName);
            if (context != null) {
                System.err.printf("  💥 [严重安全事故] 匿名请求读到了残留身份: [%s]，造成特权越权与数据串号！\n", context);
            } else {
                System.out.println("  ✔ 正常：上下文为空。");
            }
        }).get();
    }

    /**
     * 场景 2：ThreadLocal 内存泄漏底层原理剖析
     */
    private static void testMemoryLeakRisk() {
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【深度剖析 2】ThreadLocal 弱引用与强引用链引发的内存泄漏原理：");
        System.out.println("  1. 结构引用链：Thread -> ThreadLocalMap -> Entry(WeakReference(Key), StrongReference(Value))");
        System.out.println("  2. 当 ThreadLocal 实例失去强引用后，GC 会将 Entry 中的 key 标记回收置为 null；");
        System.out.println("  3. 此时只要工作线程（Worker）继续在线程池中复用存活，其 value 依然被强引用！");
        System.out.println("  4. 唯有在下次调用 get()/set()/remove() 遇到 key==null 时才可能触发 expungeStaleEntry 清理；");
        System.out.println("  5. 若线程被长期待机或后续不再调用相同哈希槽，这些 Value 将成为永久无法回收的垃圾对象！");

        // 模拟分配一个 5MB 的大数组
        byte[] largeData = new byte[5 * 1024 * 1024];
        bigPayloadContext.set(largeData);
        System.out.printf("  -> 已向当前线程上下文存入 5MB 模拟数据，当前堆占用正常。\n");

        // 正确规避方式：使用完毕立即清理
        bigPayloadContext.remove();
        System.out.println("  -> 显式执行 bigPayloadContext.remove()，Entry 彻底被清空，断开对大对象的强引用！");
    }

    /**
     * 场景 3：生产级最佳实践 —— 严格遵循 try-finally 必调 remove()
     */
    private static void testSafePracticeWithFinally(ExecutorService pool) throws Exception {
        System.out.println("\n---------------------------------------------------------------");
        System.out.println("【最佳实践 3】在 try-finally 块中百分之百确保 remove() 调用：");

        // 请求 C：按照生产开发规约使用 try-finally
        pool.submit(() -> {
            String threadName = Thread.currentThread().getName();
            try {
                currentUserContext.set("User_Bob (ID: 66666)");
                System.out.printf("  [请求 C 处理中] 线程 [%s] 绑定上下文: [%s]\n",
                        threadName, currentUserContext.get());
                // 执行核心业务逻辑...
            } finally {
                currentUserContext.remove();
                System.out.printf("  [请求 C 结束] 线程 [%s] 在 finally 块中彻底执行 remove() 清除！\n", threadName);
            }
        }).get();

        // 请求 D：再次复用该线程发起未登录访问
        pool.submit(() -> {
            String threadName = Thread.currentThread().getName();
            String context = currentUserContext.get();
            System.out.printf("  [请求 D 处理中] 线程 [%s] 读取上下文: [%s]\n",
                    threadName, context);
            if (context == null) {
                System.out.println("  ✅ [完全安全] 线程复用环境上下文干净整洁，未产生任何串号与泄漏风险！");
            } else {
                System.err.println("  ❌ 依然存在残留数据！");
            }
        }).get();
    }
}
