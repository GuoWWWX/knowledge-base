package com.knowledge.seckill.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 秒杀统一响应结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeckillResult<T> {

    private Integer code;
    private String message;
    private T data;

    public static <T> SeckillResult<T> success(T data) {
        return SeckillResult.<T>builder()
                .code(200)
                .message("抢购成功，排队处理中")
                .data(data)
                .build();
    }

    public static <T> SeckillResult<T> success(String message, T data) {
        return SeckillResult.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }

    public static <T> SeckillResult<T> fail(Integer code, String message) {
        return SeckillResult.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
}
