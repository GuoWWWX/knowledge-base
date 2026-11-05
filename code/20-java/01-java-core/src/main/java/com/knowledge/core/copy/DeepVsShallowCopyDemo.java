package com.knowledge.core.copy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 对象拷贝与契约设计深度辨析：
 * 1. clone() 默认浅拷贝 (Shallow Copy) 引用共享修改导致原始对象被篡改的踩坑事故
 * 2. 基于序列化流 (Deep Copy) 实现深拷贝彻底阻断内存对象共享
 * 3. 生产血泪：重写 equals 但忘记重写 hashCode，导致 HashMap.get() 永远返回 null 事故复现
 */
public class DeepVsShallowCopyDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. clone() 浅拷贝引用共享导致原始数据被篡改 ==========");
        demonstrateShallowCopyTrap();

        System.out.println("\n========== 2. 序列化流 (Deep Copy) 深拷贝实测 ==========");
        demonstrateDeepCopy();

        System.out.println("\n========== 3. equals 与 hashCode 契约失配导致 HashMap 无法检索 ==========");
        reproduceEqualsHashCodeContractViolation();
    }

    private static void demonstrateShallowCopyTrap() throws CloneNotSupportedException {
        UserProfile original = new UserProfile("Alice", new Address("北京", "海淀区"));
        UserProfile shallowClone = (UserProfile) original.clone();

        System.out.println("原始对象: " + original);
        System.out.println("浅克隆体: " + shallowClone);

        // 修改浅拷贝对象内部的引用对象字段
        shallowClone.getAddress().setCity("上海");
        System.out.println("--- 修改浅克隆对象的城市为 '上海' 后 ---");
        System.out.println("浅克隆体: " + shallowClone);
        System.out.println("原始对象: " + original + " (悲剧: 原始对象的城市也被静默篡改为上海！)");
    }

    private static void demonstrateDeepCopy() throws Exception {
        UserProfile original = new UserProfile("Bob", new Address("广州", "天河区"));
        UserProfile deepClone = deepCopyBySerialization(original);

        deepClone.getAddress().setCity("深圳");
        System.out.println("--- 修改深克隆对象的城市为 '深圳' 后 ---");
        System.out.println("深克隆体: " + deepClone);
        System.out.println("原始对象: " + original + " (成功: 原始对象完好无损，城市依然是广州)");
    }

    private static void reproduceEqualsHashCodeContractViolation() {
        // 创建两个属性值完全相同的 Key 对象
        BrokenKey key1 = new BrokenKey("ID_1001");
        BrokenKey key2 = new BrokenKey("ID_1001");

        System.out.println("key1.equals(key2) = " + key1.equals(key2) + " (业务逻辑上绝对相等)");
        System.out.println("key1.hashCode()   = " + key1.hashCode() + " (基于 System.identityHashCode 随机内存生成)");
        System.out.println("key2.hashCode()   = " + key2.hashCode() + " (两者哈希值截然不同)");

        Map<BrokenKey, String> map = new HashMap<>();
        map.put(key1, "VIP_PAYMENT_CONFIG");

        String retrieved = map.get(key2);
        System.err.println("使用 key2 获取配置结果: " + retrieved + " (明明 equals 相等，却返回 null！)");
        System.out.println(">>> 根因分析: HashMap 先通过 key.hashCode() 计算数组桶索引，哈希值不同直接寻址到不同桶甚至链表不匹配！");
        System.out.println(">>> 契约规定: 如果两个对象 equals 相等，其 hashCode 必须绝对相等！");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Serializable> T deepCopyBySerialization(T obj) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(obj);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        try (ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (T) ois.readObject();
        }
    }

    static class Address implements Cloneable, Serializable {
        private String city;
        private String district;

        public Address(String city, String district) {
            this.city = city;
            this.district = district;
        }

        public void setCity(String city) { this.city = city; }
        public String getCity() { return city; }

        @Override
        public String toString() {
            return city + "-" + district;
        }
    }

    static class UserProfile implements Cloneable, Serializable {
        private String username;
        private Address address;

        public UserProfile(String username, Address address) {
            this.username = username;
            this.address = address;
        }

        public Address getAddress() { return address; }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            // Object.clone() 默认仅执行浅拷贝：逐个字段按位复制引用指针
            return super.clone();
        }

        @Override
        public String toString() {
            return "UserProfile{" + "username='" + username + '\'' + ", address=" + address + '}';
        }
    }

    // 错误示范：重写 equals 但未重写 hashCode
    static class BrokenKey {
        private final String code;

        public BrokenKey(String code) {
            this.code = code;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BrokenKey brokenKey = (BrokenKey) o;
            return Objects.equals(code, brokenKey.code);
        }

        // 故意缺少 hashCode() 重写，采用 Object 原生实现
    }
}
