package com.lee.http.utils;

import com.lee.http.server.vertx.codec.HttpResponse;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;

/**
 * event bus异步数据转换工具类
 */
public class AsyncResultUtils {

    /**
     * 获取异步的响应数据
     * @param msg event bus消息消费后返回的消息数据
     * @param <T> 响应数据的泛型
     * @return 响应数据
     */
    @SuppressWarnings("unchecked")
    public static <T> HttpResponse<T> transResponse(AsyncResult<Message<Object>> msg) {
        return (HttpResponse<T>) msg.result().body();
    }
}
