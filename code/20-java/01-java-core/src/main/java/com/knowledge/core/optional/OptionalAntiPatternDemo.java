package com.knowledge.core.optional;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Optional 正确使用姿势与反模式：
 * 1. orElse(expensiveCall()) 即使 Optional 有值也会立刻执行无用耗时调用的惨痛陷阱
 * 2. orElseGet(() -> expensiveCall()) Supplier 函数式惰性求值最佳实践
 * 3. 避免将 Optional 作为方法参数、POJO 字段或集合成员的反模式警告
 */
public class OptionalAntiPatternDemo {

    private static final AtomicInteger rpcCallCount = new AtomicInteger();

    public static void main(String[] args) {
        System.out.println("========== 1. orElse() 盲目求值陷阱对比 orElseGet() 惰性求值 ==========");
        
        Optional<String> presentOptional = Optional.of("从高速本地缓存命中的数据");

        System.out.println("--- 测试 1: 使用 orElse() ---");
        rpcCallCount.set(0);
        String res1 = presentOptional.orElse(fetchFromRemoteRpcService());
        System.out.println("获得结果: " + res1);
        System.out.println("RPC 实际被调用次数: " + rpcCallCount.get() + " (明明缓存有值，却依然白白发起了一次昂贵的远程 RPC！)");

        System.out.println("\n--- 测试 2: 使用 orElseGet() ---");
        rpcCallCount.set(0);
        String res2 = presentOptional.orElseGet(OptionalAntiPatternDemo::fetchFromRemoteRpcService);
        System.out.println("获得结果: " + res2);
        System.out.println("RPC 实际被调用次数: " + rpcCallCount.get() + " (惰性求值生效，直接命中短路，零无谓开销！)");

        System.out.println("\n--- 测试 3: 当 Optional 为空时的表现 ---");
        Optional<String> emptyOptional = Optional.empty();
        rpcCallCount.set(0);
        String res3 = emptyOptional.orElseGet(OptionalAntiPatternDemo::fetchFromRemoteRpcService);
        System.out.println("获得结果: " + res3 + ", 此时触发后备逻辑调用次数: " + rpcCallCount.get());

        System.out.println("\n========== 2. Optional 链式优雅处理 ==========");
        User user = new User("Alice", new Address("北京市海淀区中关村大街"));
        String city = Optional.ofNullable(user)
                .map(User::getAddress)
                .map(Address::getDetail)
                .filter(s -> s.contains("北京"))
                .orElse("未知城市");
        System.out.println("深层属性安全提取 (零 NPE): " + city);
    }

    private static String fetchFromRemoteRpcService() {
        rpcCallCount.incrementAndGet();
        System.out.println(">>> [远程调用 RPC 警告] 正在请求下游微服务 (耗时 500ms)...");
        return "远程兜底数据";
    }

    static class User {
        private final String name;
        private final Address address;

        public User(String name, Address address) {
            this.name = name;
            this.address = address;
        }

        public String getName() { return name; }
        public Address getAddress() { return address; }
    }

    static class Address {
        private final String detail;

        public Address(String detail) {
            this.detail = detail;
        }

        public String getDetail() { return detail; }
    }
}
