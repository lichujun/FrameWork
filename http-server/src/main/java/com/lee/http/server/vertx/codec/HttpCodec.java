package com.lee.http.server.vertx.codec;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.MessageCodec;
import lombok.extern.slf4j.Slf4j;
import java.io.*;

/**
 * 编解码
 * @author lichujun
 * @date 2019/2/20 10:34 PM
 */
@Slf4j
public class HttpCodec implements MessageCodec<Object, Object> {

    /**
     * 将消息实体封装到Buffer用于传输
     *
     * 实现方式：
     * 使用对象流从对象中获取Byte数组然后追加到Buffer
     */
    @Override
    public void encodeToWire(Buffer buffer, Object s) {
        final ByteArrayOutputStream b = new ByteArrayOutputStream();
        ObjectOutputStream o;
        try {
            o = new ObjectOutputStream(b);
            o.writeObject(s);
            o.close();
            buffer.appendBytes(b.toByteArray());
        } catch (IOException e) {
            log.warn("将消息实体封装到Buffer用于传输出现异常", e);
        }

    }

    /**
     * 从buffer中获取传输的消息实体
     */
    @Override
    public Message decodeFromWire(int pos, Buffer buffer) {
        final ByteArrayInputStream b = new ByteArrayInputStream(buffer.getBytes());
        ObjectInputStream o;
        Message msg = null;
        try {
            o = new ObjectInputStream(b);
            msg = (Message) o.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.warn("从buffer中获取传输的消息实体出现异常", e);
        }
        return msg;
    }

    /**
     * 如果是本地消息则直接返回
     */
    @Override
    public Object transform(Object s) {
        return s;
    }

    /**
     * 编解码器的名称：
     * 必须唯一，用于发送消息时识别编解码器，以及取消编解码器
     */
    @Override
    public String name() {
        return "HttpCodec";
    }

    /**
     * 用于识别是否是用户编码器
     * 自定义编解码器通常使用-1
     */
    @Override
    public byte systemCodecID() {
        return -1;
    }

}
