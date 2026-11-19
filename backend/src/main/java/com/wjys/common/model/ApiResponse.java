package com.wjys.common.model;

import com.wjys.common.trace.TraceIdContext;

public record ApiResponse<T>(int code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data, newTraceId());
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data, newTraceId());
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null, newTraceId());
    }

    private static String newTraceId() {
        return TraceIdContext.current();
    }
}
