package com.lee.http.server.netty.handler;

import com.alibaba.fastjson.JSON;
import com.lee.http.bean.ControllerInfo;
import com.lee.http.core.ScanController;
import com.lee.http.utils.InvokeControllerUtils;
import com.lee.http.utils.TraceIDUtils;
import com.lee.iocaop.core.IocAppContext;
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
import java.util.*;

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
        String content = null;
        HttpResponseStatus status = HttpResponseStatus.OK;
        try {
            // 过滤浏览器的请求
            if(request.uri().equals(FAVICON_ICO)){
                ctx.close();
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
                status = HttpResponseStatus.NOT_FOUND;
            } else {
                // 设置traceID，方便追踪日志
                String traceID = UUID.randomUUID().toString()
                        .replace("-", "")
                        .toLowerCase();
                TraceIDUtils.setTraceID(traceID);
                if (MapUtils.isEmpty(controllerInfo.getMethodParameter())) {
                    log.info("请求入参为空，请求路径为：【{}】", path);
                    // 无参controller层方法调用
                    content = JSON.toJSONString(InvokeControllerUtils.invokeController(controllerInfo));
                } else {
                    String reqJson = null;
                    // 有参controller层方法调用
                    Map<String, String> paramMap = parse(request);
                    if (controllerInfo.getMethodParameter().size() == 1
                            && MapUtils.isEmpty(paramMap)) {
                        // 获取post请求的raw body
                        reqJson = Optional.of((HttpContent) request)
                                .map(HttpContent::content)
                                .map(it -> it.toString(StandardCharsets.UTF_8))
                                .map(StringUtils::deleteWhitespace)
                                .orElse(null);
                    }
                    log.info("请求路径为：【{}】，请求入参：【{}】", path,
                            MapUtils.isNotEmpty(paramMap) ? paramMap : reqJson);
                    content = JSON.toJSONString(InvokeControllerUtils.invokeController(
                            controllerInfo, paramMap, reqJson));
                    log.info("请求出参：【{}】", content);
                }
            }
        } catch (Throwable e) {
            Method method = CONTEXT.getProcessExceptionMethod(e);
            if (method != null) {
                Class<?> tClass = method.getDeclaringClass();
                Object obj = CONTEXT.getBean(StringUtils.uncapitalize(tClass.getSimpleName()));
                try {
                    content = JSON.toJSONString(method.invoke(obj, e));
                    log.info("请求出参：【{}】", content);
                } catch (Exception exception) {
                    log.warn("统一捕获异常处理发生异常", e);
                    status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
                }
            } else {
                log.warn("unhandled exception：", e);
                status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
            }
        } finally {
            // 移除traceID，防止内存泄露
            TraceIDUtils.removeTraceID();
        }

        // 写入响应
        FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, status);
        if (HttpResponseStatus.OK.equals(status) && StringUtils.isNotBlank(content)) {
            httpResponse.content().writeBytes(content.getBytes());
        }
        httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        httpResponse.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
        boolean keepAlive = HttpUtil.isKeepAlive(request);
        if (keepAlive) {
            httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
            ctx.write(httpResponse);
        } else {
            ctx.write(httpResponse).addListener(ChannelFutureListener.CLOSE);
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
