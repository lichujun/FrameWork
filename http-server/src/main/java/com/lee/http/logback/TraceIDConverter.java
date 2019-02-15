package com.lee.http.logback;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.lee.http.utils.TraceIDUtils;

/**
 * @author lichujun
 * @date 2019/2/15 11:12 PM
 */
public class TraceIDConverter extends ClassicConverter {
    @Override
    public String convert(ILoggingEvent iLoggingEvent) {
        return TraceIDUtils.getTraceID();
    }
}
