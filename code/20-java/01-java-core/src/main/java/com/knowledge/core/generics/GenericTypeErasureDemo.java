package com.knowledge.core.generics;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 泛型深度探索：类型擦除、PECS 原则、Bridge 桥接方法、运行时泛型实参获取
 */
public class GenericTypeErasureDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. 泛型类型擦除 (Type Erasure) 证明 ==========");
        List<String> stringList = new ArrayList<>();
        List<Integer> integerList = new ArrayList<>();
        System.out.println("stringList.getClass()  = " + stringList.getClass());
        System.out.println("integerList.getClass() = " + integerList.getClass());
        System.out.println("运行时 Class 是否相同: " + (stringList.getClass() == integerList.getClass()));

        // 通过反射绕过编译期泛型检查，向 List<String> 塞入 Integer
        Method addMethod = stringList.getClass().getMethod("add", Object.class);
        addMethod.invoke(stringList, 1024);
        System.out.println("反射塞入 Integer 后列表内容: " + stringList);
        System.out.println("获取第0个元素(Object): " + stringList.get(0));

        System.out.println("\n========== 2. PECS 原则 (Producer Extends, Consumer Super) ==========");
        // Producer Extends: 只能作为生产者（取数据），不能向其写入（除 null 外）
        List<Integer> ints = Arrays.asList(1, 2, 3, 4, 5);
        List<? extends Number> producerList = ints;
        Number num = producerList.get(0); // 协变只读安全
        System.out.println("PECS Producer Extends 读取元素: " + num);
        // producerList.add(10); // 编译报错！编译器无法推断具体是 Number 的哪个具体子类型

        // Consumer Super: 只能作为消费者（存数据），不能获取具体类型（只能得到 Object）
        List<Number> numList = new ArrayList<>();
        List<? super Integer> consumerList = numList;
        consumerList.add(100); // 逆变安全写入 Integer 及其子类
        consumerList.add(200);
        Object obj = consumerList.get(0); // 只能以 Object 接收
        System.out.println("PECS Consumer Super 写入并以 Object 读取: " + obj + ", 列表: " + consumerList);

        System.out.println("\n========== 3. 编译器自动生成 Bridge 桥接方法探测 ==========");
        Class<?> subClass = StringSupplier.class;
        for (Method m : subClass.getDeclaredMethods()) {
            System.out.printf("方法名: %-15s 返回类型: %-15s 是否Bridge方法: %s%n",
                    m.getName(), m.getReturnType().getSimpleName(), m.isBridge());
        }

        System.out.println("\n========== 4. 运行时通过 TypeToken / ParameterizedType 获取泛型实参 ==========");
        TypeToken<List<String>> token = new TypeToken<>() {};
        Type type = token.getType();
        System.out.println("匿名内部类获取的完整泛型类型: " + type);
        if (type instanceof ParameterizedType pt) {
            System.out.println("原始类型 (RawType): " + pt.getRawType());
            System.out.println("泛型实参 (ActualTypeArguments): " + Arrays.toString(pt.getActualTypeArguments()));
        }
    }

    // 泛型父接口
    interface Supplier<T> {
        T supply();
    }

    // 子类泛型实参特化为 String，编译器会自动生成 public Object supply() 桥接方法以维持多态虚分派
    static class StringSupplier implements Supplier<String> {
        @Override
        public String supply() {
            return "Hello Generics";
        }
    }

    // 类似 Google Guava 的 TypeToken 原理：基于抽象类匿名子类保存父类 ParameterizedType
    public static abstract class TypeToken<T> {
        private final Type type;

        protected TypeToken() {
            Type superclass = getClass().getGenericSuperclass();
            if (superclass instanceof ParameterizedType pt) {
                this.type = pt.getActualTypeArguments()[0];
            } else {
                throw new IllegalArgumentException("必须是参数化类型");
            }
        }

        public Type getType() {
            return type;
        }
    }
}
