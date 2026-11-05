package com.knowledge.shortlink;

import com.knowledge.shortlink.service.ShortLinkService;
import com.knowledge.shortlink.util.Base62Converter;
import com.knowledge.shortlink.util.HashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 短链服务单元测试套件
 */
public class ShortLinkServiceTest {

    private ShortLinkService shortLinkService;

    @BeforeEach
    public void setup() {
        shortLinkService = new ShortLinkService();
        shortLinkService.init();
    }

    @Test
    @DisplayName("测试 Base62 编码与解码双向一致性")
    public void testBase62Bidirectional() {
        long[] testValues = {0L, 1L, 61L, 62L, 1000000L, 4294967295L, 9876543210123L};
        for (long val : testValues) {
            String encoded = Base62Converter.toBase62(val, 6);
            assertNotNull(encoded);
            assertTrue(encoded.length() >= 6);
            long decoded = Base62Converter.fromBase62(encoded);
            assertEquals(val, decoded, "解码还原值必须与原数值完全相等");
        }
    }

    @Test
    @DisplayName("测试 MurmurHash3 算法输出在 32 位无符号范围内")
    public void testMurmur32() {
        String testUrl = "https://spring.io/projects/spring-boot";
        long hash = HashUtil.murmur32AsUnsigned(testUrl);
        assertTrue(hash >= 0 && hash <= 0xFFFFFFFFL);
    }

    @Test
    @DisplayName("测试短链生成、幂等性、302还原与PV计数")
    public void testCreateAndRedirect() {
        String longUrl = "https://www.example.com/products/detail?id=998877";

        // 1. 生成短码
        String code1 = shortLinkService.createShortLink(longUrl);
        assertNotNull(code1);
        assertEquals(6, code1.length());

        // 2. 幂等性测试
        String code2 = shortLinkService.createShortLink(longUrl);
        assertEquals(code1, code2, "相同长链接生成的短码必须一致");

        // 3. 还原长链接
        String originUrl = shortLinkService.getOriginUrl(code1);
        assertEquals(longUrl, originUrl, "还原出的长链接必须与原始链接完全一致");

        // 4. 再次访问并校验 PV 统计
        shortLinkService.getOriginUrl(code1);
        assertEquals(2L, shortLinkService.getAccessCount(code1), "PV 累计访问数必须等于 2");
    }

    @Test
    @DisplayName("测试布隆过滤器对未知短链的拦截能力")
    public void testBloomFilterInterception() {
        String nonExistentCode = "ZZZZ99";
        String result = shortLinkService.getOriginUrl(nonExistentCode);
        assertNull(result, "不存在的短码必须返回 null，不进行穿透");
    }

    @Test
    @DisplayName("完整运行 ShortLinkQuickRunDemo 闭环控制台演示")
    public void testQuickRunDemo() {
        assertDoesNotThrow(() -> ShortLinkQuickRunDemo.main(new String[0]));
    }
}
