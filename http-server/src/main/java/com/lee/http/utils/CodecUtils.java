package com.lee.http.utils;

import io.vertx.core.buffer.Buffer;
import lombok.extern.slf4j.Slf4j;
import java.io.*;

/**
 * @author lichujun
 * @date 2019/2/25 10:37 PM
 */
@Slf4j
public class CodecUtils {

    public static <T> void encode(Buffer buffer, T t) {
        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o = null;
        try {
            o = new ObjectOutputStream(b);
            o.writeObject(t);

            buffer.appendBytes(b.toByteArray());
        } catch (IOException e) {
            log.warn("将消息实体封装到Buffer用于传输出现异常", e);
        } finally {
            try {
                if (o != null) {
                    o.close();
                }
            } catch (IOException e) {
                log.warn("关闭资源失败");
            }
        }
    }

    public static <T> T decode(Buffer buffer, Class<? extends T> tClass) {
        final ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBytes());
        ObjectInputStream o;
        T msg = null;
        try {
            o = new ObjectInputStream(b);
            msg = tClass.cast(o.readObject());
        } catch (IOException | ClassNotFoundException e) {
            log.warn("从buffer中获取传输的消息实体出现异常", e);
        }
        return msg;
    }
}
