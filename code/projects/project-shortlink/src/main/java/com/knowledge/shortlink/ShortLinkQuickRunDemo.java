package com.knowledge.shortlink;

import com.knowledge.shortlink.service.ShortLinkService;
import com.knowledge.shortlink.util.Base62Converter;
import com.knowledge.shortlink.util.HashUtil;

/**
 * 短链服务快速演示闭环运行类
 * 包含独立的 main 方法，可直接一键运行，完整演示：
 * 1. Base62 进制定点编码与解码验证
 * 2. MurmurHash3 高性能散列计算
 * 3. 长链生成 6 位短链码全流程
 * 4. 同一长链的幂等性保证
 * 5. 短码 302 跳转还原与访问统计
 * 6. Guava 布隆过滤器防穿透攻击拦截效果
 */
public class ShortLinkQuickRunDemo {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("            >>> 百亿级高性能短链服务核心算法与链路演示 <<<");
        System.out.println("================================================================================\n");

        // ----------------------------------------------------
        // 环节 1：Base62 进制定点互转测试
        // ----------------------------------------------------
        System.out.println("【环节 1】Base62 进制转换测试：");
        long testNum = 20261103001L;
        String base62Code = Base62Converter.toBase62(testNum, 6);
        long restoredNum = Base62Converter.fromBase62(base62Code);
        System.out.println(String.format("  原始数值: %d -> 62进制编码: %s -> 解码还原: %d (一致性校验: %b)",
                testNum, base62Code, restoredNum, testNum == restoredNum));
        System.out.println();

        // ----------------------------------------------------
        // 环节 2：MurmurHash3 散列计算演示
        // ----------------------------------------------------
        System.out.println("【环节 2】MurmurHash3 散列计算演示：");
        String sampleUrl = "https://item.jd.com/100083485421.html?utm_source=spring_sale&utm_medium=sms";
        long hash32 = HashUtil.murmur32AsUnsigned(sampleUrl);
        String directShortCode = Base62Converter.toBase62(hash32, 6);
        System.out.println("  输入长链接: " + sampleUrl);
        System.out.println("  MurmurHash3 (32位无符号整数): " + hash32);
        System.out.println("  生成 6 位短码: " + directShortCode);
        System.out.println();

        // ----------------------------------------------------
        // 环节 3：短链服务完整生成与还原闭环
        // ----------------------------------------------------
        System.out.println("【环节 3】短链业务服务全链路闭环：");
        ShortLinkService service = new ShortLinkService();
        service.init();

        String[] testUrls = {
                "https://cloud.tencent.com/act/pro/promotion-spring?from=10001",
                "https://developer.aliyun.com/article/1458921?spm=a2c6h.12873639.article-detail",
                "https://spring.io/projects/spring-boot#learn"
        };

        System.out.println("  1. 批量生成短链：");
        String[] shortCodes = new String[testUrls.length];
        for (int i = 0; i < testUrls.length; i++) {
            shortCodes[i] = service.createShortLink(testUrls[i]);
            System.out.println(String.format("    [长链 %d] %s\n      └──> 短码: %s (访问路径: http://t.cn/%s)",
                    i + 1, testUrls[i], shortCodes[i], shortCodes[i]));
        }
        System.out.println();

        // ----------------------------------------------------
        // 环节 4：幂等性测试（同一长链接重复请求）
        // ----------------------------------------------------
        System.out.println("【环节 4】幂等性测试（重复生成同一长链接）：");
        String duplicateTestUrl = testUrls[0];
        String repeatCode = service.createShortLink(duplicateTestUrl);
        boolean isIdempotent = repeatCode.equals(shortCodes[0]);
        System.out.println(String.format("  原始短码: %s, 重复生成短码: %s, 幂等校验结果: %s",
                shortCodes[0], repeatCode, isIdempotent ? "【通过】同一长链保持单值短码" : "【失败】产生多余短码"));
        System.out.println();

        // ----------------------------------------------------
        // 环节 5：短码还原重定向与访问计数 (PV)
        // ----------------------------------------------------
        System.out.println("【环节 5】短码还原与访问计数：");
        String testCode = shortCodes[0];
        // 模拟 5 次用户点击访问
        for (int i = 1; i <= 5; i++) {
            String origin = service.getOriginUrl(testCode);
            System.out.println(String.format("  第 %d 次访问短码 [%s] -> 302跳转至: %s", i, testCode, origin));
        }
        long pv = service.getAccessCount(testCode);
        System.out.println("  短码 [" + testCode + "] 累计统计 PV: " + pv + " 次");
        System.out.println();

        // ----------------------------------------------------
        // 环节 6：Guava 布隆过滤器防穿透攻击拦截验证
        // ----------------------------------------------------
        System.out.println("【环节 6】布隆过滤器防黑客恶意穿透拦截测试：");
        String[] maliciousCodes = {"h4ck99", "xxXX12", "nonExi", "fake00"};
        for (String maliciousCode : maliciousCodes) {
            String result = service.getOriginUrl(maliciousCode);
            System.out.println(String.format("  探测不存在的短码: [%s] -> 拦截结果: %s",
                    maliciousCode, result == null ? "【布隆过滤器命中拦截】判定不存在，阻断缓存/数据库穿透！" : "【异常穿透】" + result));
        }

        System.out.println("\n================================================================================");
        System.out.println("  >>> 高性能短链服务演示全部顺利通过！<<<");
        System.out.println("================================================================================");
    }
}
