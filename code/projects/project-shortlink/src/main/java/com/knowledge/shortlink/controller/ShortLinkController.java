package com.knowledge.shortlink.controller;

import com.knowledge.shortlink.service.ShortLinkService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * 高性能短链核心 REST 控制器
 * 提供：
 * 1. POST /api/shortlink/create : 生成短链
 * 2. GET /{shortCode}            : 302 临时重定向跳转至原始长链接并记录统计
 * 3. GET /api/shortlink/stats/{shortCode} : 查询短链访问统计
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class ShortLinkController {

    private final ShortLinkService shortLinkService;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateShortLinkRequest {
        private String originUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApiResponse<T> {
        private Integer code;
        private String message;
        private T data;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder().code(200).message("success").data(data).build();
        }

        public static <T> ApiResponse<T> fail(Integer code, String message) {
            return ApiResponse.<T>builder().code(code).message(message).build();
        }
    }

    /**
     * 生成短链接口
     * POST /api/shortlink/create
     * 请求体: { "originUrl": "https://example.com/item/100023?utm_source=sms" }
     */
    @PostMapping("/api/shortlink/create")
    public ApiResponse<Map<String, Object>> createShortLink(@RequestBody CreateShortLinkRequest request,
                                                           HttpServletRequest httpRequest) {
        if (request.getOriginUrl() == null || request.getOriginUrl().trim().isEmpty()) {
            return ApiResponse.fail(400, "参数错误：originUrl 不能为空");
        }

        String shortCode = shortLinkService.createShortLink(request.getOriginUrl());

        // 构造完整可点击短链 URL
        String baseUrl = httpRequest.getScheme() + "://" + httpRequest.getServerName() + ":" + httpRequest.getServerPort();
        String fullShortUrl = baseUrl + "/" + shortCode;

        Map<String, Object> data = new HashMap<>();
        data.put("shortCode", shortCode);
        data.put("shortUrl", fullShortUrl);
        data.put("originUrl", request.getOriginUrl());

        return ApiResponse.success(data);
    }

    /**
     * 短链 302 核心跳转接口
     * GET /{shortCode}
     *
     * 架构考量：使用 HTTP 302 (Found) 临时重定向而非 301 (Moved Permanently)。
     * 若使用 301，浏览器会在本地产生长久强缓存，后续点击将跳过服务器，导致 PV/UV 丢失统计；
     * 302 确保每次跳转都会打回短链服务器进行访问审计与分析。
     */
    @GetMapping("/{shortCode:[0-9a-zA-Z]{1,16}}")
    public ResponseEntity<Void> redirect(@PathVariable("shortCode") String shortCode,
                                         HttpServletResponse response) throws IOException {
        String originUrl = shortLinkService.getOriginUrl(shortCode);
        if (originUrl == null) {
            // 短码不存在或被布隆过滤器拦截
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(originUrl));
        // 设置禁用客户端强缓存，确保每次访问都会请求短链服务以准确统计 PV
        headers.setCacheControl("private, max-age=0, no-cache");

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    /**
     * 查询短链点击量统计接口
     * GET /api/shortlink/stats/{shortCode}
     */
    @GetMapping("/api/shortlink/stats/{shortCode}")
    public ApiResponse<Map<String, Object>> getStats(@PathVariable("shortCode") String shortCode) {
        String originUrl = shortLinkService.getOriginUrl(shortCode);
        if (originUrl == null) {
            return ApiResponse.fail(404, "短链码不存在");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("shortCode", shortCode);
        stats.put("originUrl", originUrl);
        stats.put("totalPv", shortLinkService.getAccessCount(shortCode));
        stats.put("globalShortLinksCount", shortLinkService.getTotalShortLinks());
        stats.put("globalCollisionCount", shortLinkService.getCollisionCount());

        return ApiResponse.success(stats);
    }
}
