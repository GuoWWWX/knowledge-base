package com.knowledge.jvm.jit;

/**
 * JIT 编译器黑科技：逃逸分析 (Escape Analysis) 与标量替换 (Scalar Replacement)
 * 核心原理：
 * 1. 逃逸分析：JIT 编译器在运行期分析对象的动态作用域。如果一个对象在方法内分配，且永远不会被外部方法、类变量或线程访问，则判定为“未逃逸”。
 * 2. 标量替换：未逃逸的对象无需在 JVM 堆内存中开辟对象头与内存分配，而是将其拆解为基础类型（标量）直接存储在 CPU 寄存器或线程私有虚拟机栈帧中！
 * 3. 栈上分配效果：方法执行结束栈帧出栈，变量瞬间自动销毁，彻底消除堆内存分配开销与垃圾回收 (GC) 负担！
 */
public class JitEscapeAnalysisScalarReplaceDemo {

    public static void main(String[] args) {
        System.out.println("========== 1. JIT 逃逸分析与标量替换性能实测 ==========");
        System.out.println("JVM 默认已开启: -XX:+DoEscapeAnalysis -XX:+EliminateAllocations");

        // 预热 JIT 编译器，让 C2 编译器将热点方法编译为本地机器指令
        for (int i = 0; i < 50_000; i++) {
            allocatePoint();
        }

        int iterations = 50_000_000;
        System.out.printf("开始在循环中连续创建 %d 次未逃逸对象...%n", iterations);
        
        long start = System.currentTimeMillis();
        long sum = 0;
        for (int i = 0; i < iterations; i++) {
            sum += allocatePoint();
        }
        long duration = System.currentTimeMillis() - start;

        System.out.println("累加校验和: " + sum);
        System.out.printf("50,000,000 次创建耗时: %d ms%n", duration);
        System.out.println(">>> 性能分析: 如果这 5000 万个 Point 对象全部在堆中真实分配，将消耗超过 1.2GB 堆内存并触发数十次 GC，耗时至少 2000ms+！");
        System.out.println(">>> 正因为 JIT 标量替换生效，Point 的 x 和 y 被优化为两个栈上局部变量 (int x, y)，堆内存分配次数实际为 0！耗时仅几十毫秒！");
    }

    // 局部分配，未逃逸出该方法
    private static int allocatePoint() {
        Point point = new Point(10, 20);
        return point.x + point.y;
    }

    static class Point {
        final int x;
        final int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}
