package com.wjys.common.trace;

import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdContext {
    public static final String HEADER = "X-Trace-Id";
    private static final String MDC_KEY = "traceId";

    private TraceIdContext() {
    }

    public static String current() {
        String traceId = MDC.get(MDC_KEY);
        return traceId == null || traceId.isBlank() ? newTraceId() : traceId;
    }

    public static void set(String traceId) {
        MDC.put(MDC_KEY, traceId);
    }

    public static void clear() {
        MDC.remove(MDC_KEY);
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
