package com.knowledge.core.string;

/**
 * 字符串常量池（String Table）与 .intern() 机制物理证明实验
 * <p>
 * 实验重点：
 * 1. 物理证明 new String() 与字面量的地址分离（堆对象 vs 常量池引用）。
 * 2. 深入理解 JDK 7+ intern() 机制变革：不再复制对象实体，而是首次遇到时在常量池记录堆对象引用。
 * 3. 经典神题验证：为什么 "计算机软件".intern() == 自身 为 true，而 "java".intern() == 自身 为 false？
 * 4. 编译期常量折叠（Constant Folding）vs 运行时动态拼接（StringConcatFactory / StringBuilder）底层差异。
 * 5. 使用 System.identityHashCode 打印对象物理内存地址哈希指纹，提供无可辩驳的事实证据。
 * </p>
 *
 * @author Knowledge Base
 */
public class StringPoolInternProofDemo {

    public static void main(String[] args) {
        printHeader("实验 1：new String()、字面量与 .intern() 的基础内存地址比对");
        demonstrateBasicAllocation();

        printHeader("实验 2：深入解析《深入理解Java虚拟机》经典面试题：'计算机软件' vs 'java'");
        demonstrateClassicJvmInternRiddle();

        printHeader("实验 3：编译期常量折叠（Constant Folding）vs 运行时动态拼接");
        demonstrateConstantFoldingAndDynamicConcat();

        printHeader("实验 4：JDK 6 与 JDK 7+ 字符串常量池内存架构演进总结");
        summarizeArchitectureEvolution();
    }

    /**
     * 场景一：new String("abc") 与字面量 "abc"
     */
    private static void demonstrateBasicAllocation() {
        // 创建过程：字面量 "HelloJava" 首先在常量池中声明或存在，接着 new String 在堆中分配全新独立对象
        String heapStr = new String("HelloJava");
        String poolStr = "HelloJava";
        String internStr = heapStr.intern();

        System.out.println("heapStr 对象地址指纹       : 0x" + Integer.toHexString(System.identityHashCode(heapStr)));
        System.out.println("poolStr 对象地址指纹       : 0x" + Integer.toHexString(System.identityHashCode(poolStr)));
        System.out.println("heapStr.intern() 地址指纹 : 0x" + Integer.toHexString(System.identityHashCode(internStr)));

        System.out.println("\n【物理地址比对结论】：");
        System.out.println(" 1. heapStr == poolStr ? " + (heapStr == poolStr) +
                " -> false (heapStr 指向堆中的独立 String 实例，poolStr 指向字符串常量池引用)");
        System.out.println(" 2. poolStr == internStr ? " + (poolStr == internStr) +
                " -> true  (heapStr.intern() 在常量池中检测到已存在 'HelloJava'，直接返回常量池已有引用)");
        System.out.println(" 3. heapStr == internStr ? " + (heapStr == internStr) +
                " -> false (调用 intern() 不会改变 heapStr 变量本身所持有的堆对象指针)");
    }

    /**
     * 场景二：经典神题比对——首次出现原则与 JVM 预加载关键字
     */
    private static void demonstrateClassicJvmInternRiddle() {
        System.out.println("【测试组 A：组合全新字符串 '计算机' + '软件'】");
        // 使用 StringBuilder.toString() 生成全新的字符串，此时字符串常量池中绝对没有 "计算机软件" 这 5 个字的完整组合字面量
        String str1 = new StringBuilder("计算机").append("软件").toString();
        String intern1 = str1.intern();

        System.out.println(" str1 原始堆引用指纹       : 0x" + Integer.toHexString(System.identityHashCode(str1)));
        System.out.println(" str1.intern() 返回引用指纹: 0x" + Integer.toHexString(System.identityHashCode(intern1)));
        System.out.println(" 判定结果: (str1.intern() == str1) -> " + (intern1 == str1));

        System.out.println("\n【测试组 B：组合特定字符串 'ja' + 'va'】");
        // 同样通过动态拼接生成 "java"
        String str2 = new StringBuilder("ja").append("va").toString();
        String intern2 = str2.intern();

        System.out.println(" str2 原始堆引用指纹       : 0x" + Integer.toHexString(System.identityHashCode(str2)));
        System.out.println(" str2.intern() 返回引用指纹: 0x" + Integer.toHexString(System.identityHashCode(intern2)));
        System.out.println(" 判定结果: (str2.intern() == str2) -> " + (intern2 == str2));

        System.out.println("\n【核心原理解密】：");
        System.out.println(" 1. 为什么 str1.intern() == str1 为 true？");
        System.out.println("    - JDK 7+ 的 intern() 采用‘首次出现原则’。常量池在堆中，当发现常量池不存在 '计算机软件' 时，");
        System.out.println("      JVM 为节约内存，不再深拷贝对象，而是直接在常量池 StringTable 中记录堆对象 str1 的引用！因此二者指向同一对象。");
        System.out.println(" 2. 为什么 str2.intern() == str2 为 false？");
        System.out.println("    - 因为 'java' 是 JVM 启动时的内置核心关键字！在 JVM 启动初始化阶段加载 System 类、Version 类或基础类库时，");
        System.out.println("      'java' 字符串已经被静态加载并常驻到常量池中。");
        System.out.println("    - 当我们调用 str2.intern() 时，常量池中已存在先验的 'java' 引用，因此 intern() 返回的是系统初始化时的老引用，");
        System.out.println("      它与我们动态 new 出来的 str2 堆对象自然不同（0x" + Integer.toHexString(System.identityHashCode(intern2)) +
                " != 0x" + Integer.toHexString(System.identityHashCode(str2)) + "）！");
    }

    /**
     * 场景三：编译期常量折叠 vs 运行时动态拼接
     */
    private static void demonstrateConstantFoldingAndDynamicConcat() {
        String base = "HelloWorld";

        // 1. 纯字面量相加：编译期常量折叠
        String folded = "Hello" + "World";
        System.out.println("1. \"HelloWorld\" == (\"Hello\" + \"World\") ? " + (base == folded) +
                " -> true (javac 编译期直接将字面量相加优化折叠为单一常量 \"HelloWorld\")");

        // 2. 变量参与拼接：运行时动态构建
        String prefix = "Hello";
        String dynamicConcat = prefix + "World";
        System.out.println("2. \"HelloWorld\" == (prefix + \"World\")    ? " + (base == dynamicConcat) +
                " -> false (变量无法在编译期确定，JDK 9+ 采用 makeConcatWithConstants / StringBuilder 在运行时堆中分配新对象)");

        // 3. final 常量修饰的变量拼接：同样触发编译期折叠
        final String finalPrefix = "Hello";
        String finalFolded = finalPrefix + "World";
        System.out.println("3. \"HelloWorld\" == (finalPrefix + \"World\") ? " + (base == finalFolded) +
                " -> true (final 修饰的局部常量具有编译期常量属性，编译期直接内联折叠)");
    }

    /**
     * 场景四：架构演进总结
     */
    private static void summarizeArchitectureEvolution() {
        System.out.println("【JVM 字符串常量池历史演进与内存定位】：");
        System.out.println(" ┌───────────┬───────────────────────────────┬────────────────────────────────────────────────────────┐");
        System.out.println(" │ JDK 版本  │ 字符串常量池物理存储位置       │ intern() 行为特性                                       │");
        System.out.println(" ├───────────┼───────────────────────────────┼────────────────────────────────────────────────────────┤");
        System.out.println(" │ JDK 6及前 │ 永久代（Permanent Generation） │ 常量池若无此字符串，会在永久代完整克隆复制一份对象实例 │");
        System.out.println(" │           │ 极易引发 java.lang.OutOfMemoryError: PermGen space                                     │");
        System.out.println(" ├───────────┼───────────────────────────────┼────────────────────────────────────────────────────────┤");
        System.out.println(" │ JDK 7+    │ Java 堆空间（Java Heap）       │ 常量池若无此字符串，仅在 StringTable 中记录堆对象引用，│");
        System.out.println(" │ (含Java21)│ 移出永久代/元空间             │ 避免多余实例深拷贝，并交由垃圾回收器同一管理           │");
        System.out.println(" └───────────┴───────────────────────────────┴────────────────────────────────────────────────────────┘");
        System.out.println(" 性能提示：JDK 9+ 引入 Compact Strings（紧凑字符串），用 byte[] + coder 标志位替代了传统的 char[]，");
        System.out.println(" 对于全 Latin-1 字符仅占 1 字节/字符，使大型应用的堆内存占用直接减少 30%~50%！");
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println(" " + title);
        System.out.println("=".repeat(100));
    }
}
