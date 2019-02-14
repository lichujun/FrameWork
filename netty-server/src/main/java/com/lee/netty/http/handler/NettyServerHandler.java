package com.lee.netty.http.handler;

import com.alibaba.fastjson.JSON;
import com.lee.ioc.core.IocAppContext;
import com.lee.netty.http.bean.ControllerInfo;
import com.lee.netty.http.core.ScanController;
import com.lee.netty.http.utils.InvokeControllerUtils;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 处理http请求的handler
 * @author lichujun
 * @date 2019/2/8 10:34 AM
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<HttpRequest> {

    private static final String FAVICON_ICO = "/favicon.ico";
    private static final IocAppContext CONTEXT = IocAppContext.getInstance();

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpRequest request) {
        try {
            String content;
            // 过滤浏览器的请求
            if(request.uri().equals(FAVICON_ICO)){
                return;
            }
            // 请求路径
            String path = Optional.ofNullable(request.uri())
                    .map(it -> it.split("\\?")[0])
                    .orElse("");
            String method = request.method().name();
            ControllerInfo controllerInfo = ScanController.getInstance()
                    .getController(path, method);
            // 如果没有controller层的信息则返回异常内容
            if (controllerInfo == null) {
                content = "error path, check your path and method...";
            } else {
                if (MapUtils.isEmpty(controllerInfo.getMethodParameter())) {
                    // 无参controller层方法调用
                    content = JSON.toJSONString(InvokeControllerUtils.invokeController(controllerInfo));
                } else {
                    // 获取post请求的raw body
                    String reqJson = ((HttpContent) request).content().toString(StandardCharsets.UTF_8);
                    // 有参controller层方法调用
                    Map<String, String> paramMap = parse(request);
                    content = JSON.toJSONString(InvokeControllerUtils.invokeController(
                            controllerInfo, paramMap, reqJson));
                }
            }
            // 写入响应
            FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            httpResponse.content().writeBytes(content.getBytes());
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            httpResponse.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.write(httpResponse);
            } else {
                ctx.write(httpResponse).addListener(ChannelFutureListener.CLOSE);
            }
        } catch (Exception e) {
            Method method = CONTEXT.getMethod(e);
            if (method != null) {
                Class<?> tClass = method.getClass();
                Object obj = CONTEXT.getBean(StringUtils.uncapitalize(tClass.getSimpleName()));
                try {
                    method.invoke(obj, e);
                } catch (Exception exception) {
                    log.warn(method + "参数只能存在Exception的对象");
                }
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // 出现异常时关闭连接。
        log.error("出现异常时关闭连接，出错信息", cause);
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    /**
     * 解析请求参数
     * @return 包含所有请求参数的键值对, 如果没有参数, 则返回空Map
     */
    private Map<String, String> parse(HttpRequest request) {
        HttpMethod method = request.method();
        Map<String, String> paramMap = new HashMap<>();

        if (HttpMethod.GET == method) {
            // 是GET请求
            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            decoder.parameters().forEach((key, value) -> {
                // entry.getValue()是一个List, 只取第一个元素
                paramMap.put(key, value.get(0));
            });
        } else if (HttpMethod.POST == method) {
            // 是POST请求
            HttpPostRequestDecoder decoder = null;
            try {
                decoder = new HttpPostRequestDecoder(
                        new DefaultHttpDataFactory(false), request, StandardCharsets.UTF_8);
                List<InterfaceHttpData> postData = decoder.getBodyHttpDatas();
                for(InterfaceHttpData data:postData){
                    if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.Attribute) {
                        MemoryAttribute attribute = (MemoryAttribute) data;
                        paramMap.put(attribute.getName(), attribute.getValue());
                    }
                }
            }catch (Exception e){
                log.warn("获取post的参数发生异常", e);
                return null;
            } finally {
                // 解决ByteBuf泄露的问题
                Optional.ofNullable(decoder)
                        .ifPresent(HttpPostRequestDecoder::destroy);
            }
        }
        return paramMap;
    }
}
