package com.knowledge.core.datetime;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 现代时间 API 与并发安全：
 * 1. 旧版 SimpleDateFormat 共享 Calendar 内部状态导致多线程并发篡改、时间混乱与 NumberFormatException
 * 2. Java 8 java.time (JSR-310) 不变性设计 (Immutable) 与 DateTimeFormatter 天然线程安全
 * 3. 跨时区计算 (ZoneId / ZonedDateTime) 与 Unix 时间戳无缝转换
 */
public class Java8DateTimeSafeDemo {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("========== 1. 旧版 SimpleDateFormat 多线程并发篡改现场复现 ==========");
        simulateLegacyDateFormatConcurrencyCrash();

        System.out.println("\n========== 2. Java 8 DateTimeFormatter 并发安全实测 ==========");
        benchmarkJava8DateTimeFormatter();

        System.out.println("\n========== 3. 现代日期时间跨时区与时间戳实战 ==========");
        demonstrateZoneAndTimeStamp();
    }

    private static void simulateLegacyDateFormatConcurrencyCrash() throws InterruptedException {
        SimpleDateFormat unsafeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        String dateStr = "2026-11-03 12:00:00";
                        Date parsed = unsafeFormat.parse(dateStr);
                        String formatted = unsafeFormat.format(parsed);
                        if (!dateStr.equals(formatted)) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (ParseException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.err.println("SimpleDateFormat 并发冲突错误次数: " + errorCount.get());
        System.out.println(">>> 根因分析: SimpleDateFormat 内部持有一个共享的 Calendar 对象，多线程并发调用 format/parse 时会相互覆盖计算中间值！");
    }

    private static void benchmarkJava8DateTimeFormatter() throws InterruptedException {
        DateTimeFormatter safeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        String dateStr = "2026-11-03 12:00:00";
                        LocalDateTime parsed = LocalDateTime.parse(dateStr, safeFormatter);
                        String formatted = parsed.format(safeFormatter);
                        if (!dateStr.equals(formatted)) {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();
        System.out.println("Java 8 DateTimeFormatter 并发错误次数: " + errorCount.get() + " (100% 绝对线程安全)");
        System.out.println(">>> 结论: java.time 下的所有核心类（LocalDate, LocalDateTime, Instant 等）全由 final 修饰且不可变！");
    }

    private static void demonstrateZoneAndTimeStamp() {
        // 当前北京时间 (UTC+8)
        ZonedDateTime beijingTime = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        // 转换为东京时间 (UTC+9)
        ZonedDateTime tokyoTime = beijingTime.withZoneSameInstant(ZoneId.of("Asia/Tokyo"));
        // 转换为纽约时间 (UTC-5 / UTC-4)
        ZonedDateTime nyTime = beijingTime.withZoneSameInstant(ZoneId.of("America/New_York"));

        System.out.println("北京时间: " + beijingTime);
        System.out.println("东京时间: " + tokyoTime);
        System.out.println("纽约时间: " + nyTime);

        // 统一毫秒时间戳与 Instant
        Instant instant = beijingTime.toInstant();
        System.out.println("UTC 绝对时间戳 (Epoch Milli): " + instant.toEpochMilli());
    }
}
