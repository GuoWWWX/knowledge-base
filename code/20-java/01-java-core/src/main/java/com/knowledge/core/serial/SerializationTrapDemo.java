package com.knowledge.core.serial;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;

/**
 * 序列化深度实战：
 * 1. serialVersionUID 的作用：类结构演进时的序列化版本兼容性校验
 * 2. transient 关键字修饰密码等敏感字段防止泄露
 * 3. 序列化攻击破坏单例模式与 readResolve() 终极防御机制
 */
public class SerializationTrapDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. transient 关键字防止敏感字段序列化 ==========");
        UserAccount account = new UserAccount("admin", "SuperSecretPwd123");
        byte[] bytes = serialize(account);
        UserAccount deserialized = deserialize(bytes);
        System.out.println("反序列化后用户名: " + deserialized.getUsername());
        System.out.println("反序列化后密码  : " + deserialized.getPassword() + " (transient 修饰，反序列化时为 null，避免泄漏)");

        System.out.println("\n========== 2. 反序列化直接破解破坏单例模式 ==========");
        BrokenSingleton instance1 = BrokenSingleton.getInstance();
        byte[] brokenBytes = serialize(instance1);
        BrokenSingleton instance2 = deserialize(brokenBytes);
        System.out.println("原生单例引用 instance1: " + instance1.hashCode());
        System.out.println("反序列化生成 instance2: " + instance2.hashCode());
        System.err.println("两者是否为同一实例: " + (instance1 == instance2) + " (单例遭到毁灭性破解！)");

        System.out.println("\n========== 3. 使用 readResolve() 捍卫单例唯一性 ==========");
        SafeSingleton safe1 = SafeSingleton.getInstance();
        byte[] safeBytes = serialize(safe1);
        SafeSingleton safe2 = deserialize(safeBytes);
        System.out.println("原生单例引用 safe1: " + safe1.hashCode());
        System.out.println("反序列化生成 safe2: " + safe2.hashCode());
        System.out.println("两者是否为同一实例: " + (safe1 == safe2) + " (readResolve 拦截并返回唯一单例实例，防守成功！)");
    }

    private static byte[] serialize(Object obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        return baos.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(byte[] bytes) throws Exception {
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) ois.readObject();
        }
    }

    static class UserAccount implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private final String username;
        private final transient String password; // 敏感信息不参与持久化/传输

        public UserAccount(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
    }

    // 易受反序列化破坏的单例
    static class BrokenSingleton implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private static final BrokenSingleton INSTANCE = new BrokenSingleton();

        private BrokenSingleton() {}

        public static BrokenSingleton getInstance() {
            return INSTANCE;
        }
    }

    // 安全的单例
    static class SafeSingleton implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private static final SafeSingleton INSTANCE = new SafeSingleton();

        private SafeSingleton() {}

        public static SafeSingleton getInstance() {
            return INSTANCE;
        }

        /**
         * JVM 在反序列化对象完成前，会通过反射检测类中是否存在 readResolve 方法
         * 如果存在，则直接以该方法的返回值作为反序列化最终结果，丢弃新建对象
         */
        @Serial
        private Object readResolve() {
            return INSTANCE;
        }
    }
}
