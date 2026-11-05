package com.knowledge.distributed.circuitbreaker;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * 分布式服务容错：微型熔断降级断路器状态机 (Circuit Breaker)
 * 1. 深度对标：Resilience4j / Sentinel / Hystrix 底层状态机
 * 2. 三大核心状态跃迁：
 *    - CLOSED (闭合/健康): 正常处理流量，滑动统计失败率。当失败率超过阈值 (如 50%)，触发熔断跃迁为 OPEN。
 *    - OPEN (开启/熔断): 流量直接快速失败或触发降级 fallback，避免雪崩冲击下游。启动熔断冷却倒计时 (如 1 秒)。
 *    - HALF_OPEN (半开/试探): 冷却时间到，允许少量探针流量通过。若探针连续成功则自愈恢复 CLOSED；若依然报错则再次击穿回 OPEN。
 */
public class CircuitBreakerStateMachineDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 1. 构建微型断路器 (阈值: 连续 3 次失败触发熔断, 冷却期: 500ms) ==========");
        MiniCircuitBreaker breaker = new MiniCircuitBreaker(3, 500);

        // 模拟正常调用
        System.out.println("\n--- 第 1 阶段: 正常调用 (CLOSED 状态) ---");
        for (int i = 0; i < 2; i++) {
            String res = breaker.execute(() -> "RPC 调用成功", () -> "降级兜底数据");
            System.out.println("调用结果: " + res);
        }

        // 模拟下游故障连续报错
        System.out.println("\n--- 第 2 阶段: 下游微服务宕机，连续抛出异常触发熔断 ---");
        for (int i = 1; i <= 3; i++) {
            final int count = i;
            String res = breaker.execute(() -> {
                throw new RuntimeException("503 Service Unavailable (网络超时)");
            }, () -> "降级兜底数据 [本地缓存已失效，返回静态兜底]");
            System.out.println("失败调用 #" + count + " 结果: " + res);
        }

        // 处于 OPEN 状态，此时调用应被秒级拦截（快速失败，不发起远程网络开销）
        System.out.println("\n--- 第 3 阶段: 断路器进入 OPEN 状态，流量被瞬时短路拦截 ---");
        String shortCircuitRes = breaker.execute(() -> "尝试发包", () -> "【断路器已熔断 OPEN】快速拒绝并执行降级！");
        System.out.println("拦截结果: " + shortCircuitRes);

        // 等待冷却期过去 (600ms)
        System.out.println("\n--- 第 4 阶段: 等待 600ms 冷却时间，断路器自动进入 HALF_OPEN 半开放试探 ---");
        TimeUnit.MILLISECONDS.sleep(600);

        // 试探流量成功，自愈恢复
        System.out.println("注入探针流量 (下游服务已重启恢复)...");
        String recoveredRes = breaker.execute(() -> "探针请求: 下游恢复健康", () -> "降级兜底");
        System.out.println("探针结果: " + recoveredRes);

        System.out.println("检验断路器当前状态是否已完全自愈恢复为 CLOSED: " + breaker.getState());
    }

    public enum State {
        CLOSED, OPEN, HALF_OPEN
    }

    public static class MiniCircuitBreaker {
        private final int failureThreshold;
        private final long openCooldownMs;

        private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);
        private volatile long lastStateChangedTimestamp = System.currentTimeMillis();

        public MiniCircuitBreaker(int failureThreshold, long openCooldownMs) {
            this.failureThreshold = failureThreshold;
            this.openCooldownMs = openCooldownMs;
        }

        public <T> T execute(Supplier<T> normalAction, Supplier<T> fallbackAction) {
            checkAndTransitionState();

            State currentState = state.get();
            if (currentState == State.OPEN) {
                // 熔断直接降级
                return fallbackAction.get();
            }

            try {
                T result = normalAction.get();
                onSuccess();
                return result;
            } catch (Exception e) {
                onError(e);
                return fallbackAction.get();
            }
        }

        private void checkAndTransitionState() {
            if (state.get() == State.OPEN) {
                long elapsed = System.currentTimeMillis() - lastStateChangedTimestamp;
                if (elapsed >= openCooldownMs) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        lastStateChangedTimestamp = System.currentTimeMillis();
                        halfOpenSuccessCount.set(0);
                        System.out.println("  ★ [状态流转] 冷却期满: OPEN -> HALF_OPEN (允许少量探针流量通过)");
                    }
                }
            }
        }

        private void onSuccess() {
            if (state.get() == State.HALF_OPEN) {
                if (halfOpenSuccessCount.incrementAndGet() >= 1) {
                    state.set(State.CLOSED);
                    failureCount.set(0);
                    lastStateChangedTimestamp = System.currentTimeMillis();
                    System.out.println("  ★ [状态自愈] 探针成功: HALF_OPEN -> CLOSED (系统全面恢复健康！)");
                }
            } else if (state.get() == State.CLOSED) {
                failureCount.set(0);
            }
        }

        private void onError(Exception e) {
            System.err.println("  -> [调用异常]: " + e.getMessage());
            if (state.get() == State.HALF_OPEN) {
                state.set(State.OPEN);
                lastStateChangedTimestamp = System.currentTimeMillis();
                System.err.println("  ✖ [状态流转] 探针失败: HALF_OPEN -> OPEN (下游仍未恢复，重新熔断！)");
            } else if (state.get() == State.CLOSED) {
                int count = failureCount.incrementAndGet();
                if (count >= failureThreshold) {
                    state.set(State.OPEN);
                    lastStateChangedTimestamp = System.currentTimeMillis();
                    System.err.printf("  ✖ [状态流转] 连续失败达到阈值 (%d次): CLOSED -> OPEN (开启熔断保护！)%n", count);
                }
            }
        }

        public State getState() {
            return state.get();
        }
    }
}
