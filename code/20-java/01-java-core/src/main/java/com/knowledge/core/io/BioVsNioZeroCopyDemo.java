package com.knowledge.core.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;

/**
 * 深入 IO 底层原理：
 * 1. 传统 BIO 阻塞流读写（4次上下文切换 + 4次数据拷贝：2次 DMA + 2次 CPU 拷贝）
 * 2. NIO 零拷贝 FileChannel.transferTo() (底层基于 Linux sendfile / Windows TransmitFile，0次 CPU 拷贝)
 * 3. 内存映射文件 MappedByteBuffer (mmap) 直接绕过 JVM 用户态缓存操作 OS 物理页
 */
public class BioVsNioZeroCopyDemo {

    public static void main(String[] args) throws Exception {
        System.out.println("========== 1. 创建 20MB 测试文件 ==========");
        File sourceFile = File.createTempFile("io_test_src_", ".dat");
        File bioDestFile = File.createTempFile("io_test_bio_", ".dat");
        File nioDestFile = File.createTempFile("io_test_nio_", ".dat");
        sourceFile.deleteOnExit();
        bioDestFile.deleteOnExit();
        nioDestFile.deleteOnExit();

        byte[] chunk = new byte[1024 * 1024]; // 1MB
        try (FileOutputStream fos = new FileOutputStream(sourceFile)) {
            for (int i = 0; i < 20; i++) {
                fos.write(chunk);
            }
        }
        System.out.println("源文件大小: " + sourceFile.length() / (1024 * 1024) + " MB");

        System.out.println("\n========== 2. 传统 BIO 读写流测试 (FileInputStream/FileOutputStream) ==========");
        long t1 = System.currentTimeMillis();
        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(bioDestFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = fis.read(buf)) != -1) {
                fos.write(buf, 0, len);
            }
        }
        long t2 = System.currentTimeMillis();
        System.out.printf("传统 BIO 耗时: %d ms (数据流转: 磁盘->内核缓冲区->JVM用户堆缓冲区->内核缓冲区->磁盘)%n", (t2 - t1));

        System.out.println("\n========== 3. NIO 零拷贝 FileChannel.transferTo() 测试 ==========");
        long t3 = System.currentTimeMillis();
        try (FileChannel srcChannel = new FileInputStream(sourceFile).getChannel();
             FileChannel destChannel = new FileOutputStream(nioDestFile).getChannel()) {
            srcChannel.transferTo(0, srcChannel.size(), destChannel);
        }
        long t4 = System.currentTimeMillis();
        System.out.printf("NIO 零拷贝耗时: %d ms (底层执行 sendfile/TransmitFile DMA 直传，零 CPU 拷贝)%n", (t4 - t3));

        System.out.println("\n========== 4. MappedByteBuffer (mmap) 内存映射高效写入演示 ==========");
        File mmapFile = File.createTempFile("io_test_mmap_", ".dat");
        mmapFile.deleteOnExit();
        try (RandomAccessFile raf = new RandomAccessFile(mmapFile, "rw");
             FileChannel channel = raf.getChannel()) {
            MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_WRITE, 0, 1024 * 1024);
            buffer.put("Hello Zero Copy MMAP World!".getBytes());
            buffer.force(); // 刷入磁盘页缓存
            System.out.println("mmap 写入完成，通过虚拟地址空间直接操作文件，无 JVM 堆开销与额外上下文切换！");
        }

        Files.deleteIfExists(sourceFile.toPath());
        Files.deleteIfExists(bioDestFile.toPath());
        Files.deleteIfExists(nioDestFile.toPath());
        Files.deleteIfExists(mmapFile.toPath());
    }
}
