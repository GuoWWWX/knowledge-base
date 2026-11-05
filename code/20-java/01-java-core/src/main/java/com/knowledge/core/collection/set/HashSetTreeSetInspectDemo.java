package com.knowledge.core.collection.set;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Set 集合深度实战：
 * 1. HashSet 适配器模式本质探测（内部持有 HashMap，所有 Value 共享静态占位对象 PRESENT）
 * 2. TreeSet 红黑树天然排序与范围检索
 * 3. TreeSet 生产大坑：compareTo 与 equals 契约不一致导致的元素“被静默吞掉”Bug
 */
public class HashSetTreeSetInspectDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. HashSet 底层适配器机制反射探测 ==========");
        inspectHashSetInternal();

        System.out.println("\n========== 2. TreeSet 生产经典踩坑：compareTo 导致元素丢失 ==========");
        reproduceTreeSetCompareTrap();
    }

    private static void inspectHashSetInternal() {
        HashSet<String> set = new HashSet<>();
        set.add("Java");
        set.add("Spring");
        set.add("AI");

        try {
            // 反射提取 HashSet 内部的 private transient HashMap<E,Object> map
            Field mapField = HashSet.class.getDeclaredField("map");
            mapField.setAccessible(true);
            Object internalMap = mapField.get(set);

            // 反射提取 private static final Object PRESENT
            Field presentField = HashSet.class.getDeclaredField("PRESENT");
            presentField.setAccessible(true);
            Object presentObj = presentField.get(null);

            System.out.println("HashSet 内部引用的 Map 实例: " + internalMap.getClass().getName());
            System.out.println("HashSet 内部的 PRESENT 占位符对象: " + presentObj);
        } catch (Throwable e) {
            // Java 16+ 模块化系统 (JPMS JEP 403) 默认开启强封装，保护 java.base 内部私有字段
            System.out.println("【Java 21 模块强封装保护生效】: 探测到系统未显式开启 --add-opens 参数");
            System.out.println("  -> 源码本质: HashSet 内部持有一个 private transient HashMap<E,Object> map 成员；");
            System.out.println("  -> 占位符设计: 持有 private static final Object PRESENT = new Object() 作为统一伪值；");
        }
        System.out.println(">>> 源码剖析: set.add(e) 底层等价于 map.put(e, PRESENT) == null，完美复用 HashMap 哈希寻址与去重！");
        System.out.println(">>> [提示] 若想在 IDEA 中直接物理反射 JDK 内部私有成员，可在 Run Configuration 的 VM options 中添加: --add-opens java.base/java.util=ALL-UNNAMED");
    }

    private static void reproduceTreeSetCompareTrap() {
        // 创建两个员工，工号不同但业绩分数相同
        Employee emp1 = new Employee(1001, "张三", 90);
        Employee emp2 = new Employee(1002, "李四", 90);

        System.out.println("emp1.equals(emp2): " + emp1.equals(emp2) + " (根据员工ID判断，绝对是两个人)");

        // 放入普通 HashSet
        HashSet<Employee> hashSet = new HashSet<>();
        hashSet.add(emp1);
        hashSet.add(emp2);
        System.out.println("HashSet 元素总数: " + hashSet.size() + " (正常去重，保留两人)");

        // 放入 TreeSet (按分数从高到低排序，若 compareTo 仅比较 score)
        TreeSet<Employee> treeSet = new TreeSet<>();
        treeSet.add(emp1);
        boolean added = treeSet.add(emp2);
        System.out.println("TreeSet 添加 emp2 结果: " + added);
        System.out.println("TreeSet 元素总数: " + treeSet.size() + "，内容: " + treeSet);
        System.err.println(">>> 致命陷阱: TreeSet 内部依据 compareTo() 是否为 0 判定对象等价，而不是 equals()！");
        System.err.println(">>> 当 compareTo 仅比较 score 时，同分的李四被判定为“已存在”，导致业务数据直接被吞掉！");
    }

    static class Employee implements Comparable<Employee> {
        private final int id;
        private final String name;
        private final int score;

        public Employee(int id, String name, int score) {
            this.id = id;
            this.name = name;
            this.score = score;
        }

        // 错误示范：仅按分数排序，忽略唯一标识 id，破坏了 (x.compareTo(y)==0) == (x.equals(y)) 契约
        @Override
        public int compareTo(Employee o) {
            return Integer.compare(this.score, o.score);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Employee employee = (Employee) o;
            return id == employee.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Employee{" + "id=" + id + ", name='" + name + '\'' + ", score=" + score + '}';
        }
    }
}
