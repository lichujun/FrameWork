package com.lee.http.utils;

/**
 * TraceID工具类，日志追踪
 * @author lichujun
 * @date 2019/2/15 10:35 PM
 */
public class TraceIDUtils {

    private static final ThreadLocal<String> TRACE_ID_LOCAL = ThreadLocal.withInitial(() -> null);

    public static void setTraceID(String traceID) {
        TRACE_ID_LOCAL.set(traceID);
    }

    public static String getTraceID() {
        return TRACE_ID_LOCAL.get();
    }

    public static void removeTraceID() {
        TRACE_ID_LOCAL.remove();
    }
}
