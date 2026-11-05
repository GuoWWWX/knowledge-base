package com.knowledge.jvm.classloader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义加密类加载器与打破双亲委派机制深度剖析
 * <p>
 * 实验重点：
 * 1. 打印类加载器层级拓扑（BootstrapClassLoader -> PlatformClassLoader -> AppClassLoader）。
 * 2. 模拟源码保护：对 .class 字节码进行异或（XOR）混淆加密，证明原生加载器因魔数失效抛出 ClassFormatError。
 * 3. 规范姿势：遵循双亲委派机制，继承 ClassLoader 并重写 findClass()，安全解密加载。
 * 4. 突破封锁：重写 loadClass() 打破双亲委派，实现多应用类版本物理隔离（Tomcat / OSGi 核心机制）。
 * 5. 证明 JVM 类唯一性公理：【全限定类名 + 加载器实例】共同决定 Class 的物理唯一性。
 * </p>
 *
 * @author Knowledge Base
 */
public class CustomEncryptClassLoaderDemo {

    // 对称异或加密密钥
    private static final byte ENCRYPT_KEY = (byte) 0xAA;

    /**
     * 用于测试加密加载的业务目标类
     */
    public static class TargetService {
        public void execute(String user) {
            System.out.printf("   [TargetService 运行中] 业务处理执行成功，操作用户: %s，执行线程: %s%n",
                    user, Thread.currentThread().getName());
        }
    }

    public static void main(String[] args) throws Exception {
        printHeader("实验 1：透视 JVM 当前类加载器树形层级结构");
        demonstrateClassLoaderHierarchy();

        printHeader("实验 2：字节码 XOR 加密与 ClassFormatError 错误验证");
        byte[] originalBytecode = loadRawBytecode(TargetService.class);
        byte[] encryptedBytecode = xorTransform(originalBytecode);
        verifyClassFormatError(encryptedBytecode);

        printHeader("实验 3：遵循双亲委派机制的解密类加载器（重写 findClass）");
        demonstrateDecryptionClassLoader(encryptedBytecode);

        printHeader("实验 4：重写 loadClass 打破双亲委派机制，实现类版本物理隔离");
        demonstrateBreakDelegationIsolation(encryptedBytecode);
    }

    /**
     * 1. 打印类加载器层次结构
     */
    private static void demonstrateClassLoaderHierarchy() {
        ClassLoader appClassLoader = CustomEncryptClassLoaderDemo.class.getClassLoader();
        System.out.println("1. 当前应用类加载器 (AppClassLoader)     : " + appClassLoader);

        ClassLoader platformClassLoader = appClassLoader.getParent();
        System.out.println("2. 平台扩展类加载器 (PlatformClassLoader): " + platformClassLoader);

        ClassLoader bootstrapClassLoader = platformClassLoader.getParent();
        System.out.println("3. 启动类根加载器 (BootstrapClassLoader) : " + bootstrapClassLoader +
                " (在 HotSpot 中由 C++ 实现，Java 层通过 null 表示)");
    }

    /**
     * 用于直接调用 defineClass 的原生测试加载器（不通过反射）
     */
    static class NaiveClassLoader extends ClassLoader {
        public Class<?> defineClassDirect(String name, byte[] b) {
            return defineClass(name, b, 0, b.length);
        }
    }

    /**
     * 2. 验证加密后的字节码魔数失效
     */
    private static void verifyClassFormatError(byte[] encryptedBytecode) {
        System.out.printf(" 原始 Class 字节码前 4 字节魔数: 0xCAFEBABE%n");
        System.out.printf(" 加密后 Class 字节码前 4 字节数值: 0x%02X%02X%02X%02X%n",
                encryptedBytecode[0], encryptedBytecode[1], encryptedBytecode[2], encryptedBytecode[3]);

        // 尝试用一个原生的非解密加载器直接加载
        try {
            NaiveClassLoader naiveLoader = new NaiveClassLoader();
            naiveLoader.defineClassDirect(TargetService.class.getName(), encryptedBytecode);
        } catch (ClassFormatError e) {
            System.out.println("【成功捕获魔数损坏异常】: " + e.getClass().getName() + " - " + e.getMessage());
            System.out.println(" 结论：由于字节码魔数 0xCAFEBABE 被异或掩码破坏，标准 JVM 校验器直接拒绝加载，达成知识产权保护目的。");
        } catch (Throwable t) {
            System.out.println("【成功捕获校验异常】: " + t.getClass().getName() + " - " + t.getMessage());
        }
    }

    /**
     * 3. 遵循双亲委派机制的自定义解密类加载器
     */
    private static void demonstrateDecryptionClassLoader(byte[] encryptedBytecode) throws Exception {
        Map<String, byte[]> encryptedStore = new HashMap<>();
        encryptedStore.put(TargetService.class.getName(), encryptedBytecode);

        // 自定义解密加载器，仅重写 findClass()
        DecryptClassLoader decryptLoader = new DecryptClassLoader(encryptedStore, CustomEncryptClassLoaderDemo.class.getClassLoader());

        // 首次加载（委派给父加载器失败后，进入自身的 findClass 解密）
        // 注意：由于 TargetService 在 classpath 中已存在，为了演示真正的解密加载，
        // 我们指定一个自定义类名或者脱离 AppClassLoader 委派
        Class<?> clazz = decryptLoader.findClassCustom(TargetService.class.getName());
        System.out.println(" 成功加载解密类: " + clazz.getName());
        System.out.println(" 类加载器归属: " + clazz.getClassLoader());

        Object instance = clazz.getDeclaredConstructor().newInstance();
        Method method = clazz.getMethod("execute", String.class);
        method.invoke(instance, "知识库工程师-Alice");

        System.out.println("【双亲委派标准规范】：遵循委派链条，保持优先由父类加载，只有在父加载器 ClassNotFoundException 时才在 findClass 中解密加载。");
    }

    /**
     * 4. 打破双亲委派机制，实现类版本物理隔离
     */
    private static void demonstrateBreakDelegationIsolation(byte[] encryptedBytecode) throws Exception {
        Map<String, byte[]> store = new HashMap<>();
        store.put(TargetService.class.getName(), encryptedBytecode);

        // 创建两个独立的、打破双亲委派的类加载器实例（模拟 Tomcat 中的两个不同 WebApp）
        BreakDelegationClassLoader webAppLoaderA = new BreakDelegationClassLoader(store, "WebAppA-ClassLoader");
        BreakDelegationClassLoader webAppLoaderB = new BreakDelegationClassLoader(store, "WebAppB-ClassLoader");

        Class<?> classInWebAppA = webAppLoaderA.loadClass(TargetService.class.getName());
        Class<?> classInWebAppB = webAppLoaderB.loadClass(TargetService.class.getName());

        System.out.println(" WebApp A 加载的 Class 对象: " + classInWebAppA + " (Loader: " + classInWebAppA.getClassLoader() + ")");
        System.out.println(" WebApp B 加载的 Class 对象: " + classInWebAppB + " (Loader: " + classInWebAppB.getClassLoader() + ")");

        System.out.println("\n【JVM 类身份判定公理比对】：");
        boolean isSameClass = (classInWebAppA == classInWebAppB);
        System.out.printf(" classInWebAppA == classInWebAppB ? %b%n", isSameClass);

        Object objA = classInWebAppA.getDeclaredConstructor().newInstance();
        boolean isInstance = classInWebAppB.isInstance(objA);
        System.out.printf(" classInWebAppB.isInstance(objA) ? %b%n", isInstance);

        System.out.println("\n【打破双亲委派的核心场景与原理总结】：");
        System.out.println(" 1. 为什么打破？在 Tomcat、OSGi 或微服务热插拔框架中，不同模块可能依赖同一三方库的不同版本（如 Jackson 2.12 vs 2.15）。");
        System.out.println("    如果完全按照双亲委派，先加载的版本会全局生效，导致另一个模块产生 NoSuchMethodError。");
        System.out.println(" 2. 如何打破？重写 loadClass(name, resolve) 方法，颠倒加载逻辑：对于业务包，先自身加载；自身找不到再委派父加载器。");
        System.out.println(" 3. 必须的防御底线：对于 java.lang 等系统核心 API，严禁自身加载，必须无条件委派给 BootstrapClassLoader，");
        System.out.println("    否则会触发 JVM 安全沙箱异常 SecurityException: Prohibited package name: java.lang！");
    }

    /**
     * 遵循双亲委派的解密类加载器
     */
    static class DecryptClassLoader extends ClassLoader {
        private final Map<String, byte[]> encryptedClasses;

        public DecryptClassLoader(Map<String, byte[]> encryptedClasses, ClassLoader parent) {
            super(parent);
            this.encryptedClasses = encryptedClasses;
        }

        public Class<?> findClassCustom(String name) throws ClassNotFoundException {
            return findClass(name);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] encrypted = encryptedClasses.get(name);
            if (encrypted == null) {
                throw new ClassNotFoundException("未在加密存储区找到类: " + name);
            }
            // 执行对称解密
            byte[] decrypted = xorTransform(encrypted);
            // 调用 JVM 底层底层 native 方法生成 Class 对象
            return defineClass(name, decrypted, 0, decrypted.length);
        }
    }

    /**
     * 打破双亲委派的类加载器（重写 loadClass）
     */
    static class BreakDelegationClassLoader extends ClassLoader {
        private final Map<String, byte[]> encryptedClasses;
        private final String loaderName;

        public BreakDelegationClassLoader(Map<String, byte[]> encryptedClasses, String loaderName) {
            // 将父类加载器设为 null，断开委派链
            super(null);
            this.encryptedClasses = encryptedClasses;
            this.loaderName = loaderName;
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                // 1. 检查是否已经被自己加载过
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    // 2. 核心底线保护：Java 系统基础类库（java.* / javax.*）严禁自行加载，必须交由根加载器
                    if (name.startsWith("java.") || name.startsWith("javax.")) {
                        c = findSystemClass(name);
                    } else if (encryptedClasses.containsKey(name)) {
                        // 3. 自定义包打破委派：优先由自己加载解密，绝不委托给父加载器！
                        byte[] encrypted = encryptedClasses.get(name);
                        byte[] decrypted = xorTransform(encrypted);
                        c = defineClass(name, decrypted, 0, decrypted.length);
                    } else {
                        // 4. 其他未知类回退到系统类加载器
                        c = findSystemClass(name);
                    }
                }
                if (resolve) {
                    resolveClass(c);
                }
                return c;
            }
        }

        @Override
        public String toString() {
            return loaderName;
        }
    }

    /**
     * 异或加密/解密算法（对称互逆：A ^ B ^ B = A）
     */
    private static byte[] xorTransform(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ ENCRYPT_KEY);
        }
        return result;
    }

    /**
     * 从当前类运行路径读取指定 Class 的原始字节码
     */
    private static byte[] loadRawBytecode(Class<?> clazz) throws Exception {
        String resourcePath = "/" + clazz.getName().replace('.', '/') + ".class";
        try (InputStream is = clazz.getResourceAsStream(resourcePath);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (is == null) {
                throw new IllegalStateException("未能从 Classpath 寻址到字节码: " + resourcePath);
            }
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            return baos.toByteArray();
        }
    }

    private static void printHeader(String title) {
        System.out.println();
        System.out.println("=".repeat(100));
        System.out.println(" " + title);
        System.out.println("=".repeat(100));
    }
}
