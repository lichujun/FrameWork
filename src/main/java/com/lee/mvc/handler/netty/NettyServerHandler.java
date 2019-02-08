package com.lee.mvc.handler.netty;

import com.alibaba.fastjson.JSON;
import com.lee.common.utils.ioc.InvokeControllerUtils;
import com.lee.mvc.bean.ControllerInfo;
import com.lee.mvc.core.ScanMvcComponent;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.MemoryAttribute;
import io.netty.util.CharsetUtil;
import org.apache.commons.collections4.MapUtils;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 处理http请求的handler
 * @author lichujun
 * @date 2019/2/8 10:34 AM
 */
public class NettyServerHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            String content;
            // 获取请求
            HttpRequest request = (HttpRequest) msg;
            // 获取post请求的raw body
            String reqJson = ((HttpContent) msg).content().toString(Charset.forName("UTF-8"));
            // 过滤浏览器的请求
            if(request.uri().equals("/favicon.ico")){
                return;
            }
            // 请求路径
            String path = Optional.ofNullable(request.uri())
                    .map(it -> it.split("\\?")[0])
                    .orElse("");
            FullHttpResponse httpResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
            String method = request.method().name();
            ControllerInfo controllerInfo = ScanMvcComponent.getInstance()
                    .getController(path, method);
            // 如果没有controller层的信息则返回异常内容
            if (controllerInfo == null) {
                content = "error path, check your path and method...";
            } else {
                if (MapUtils.isEmpty(controllerInfo.getMethodParameter())) {
                    // 无参controller层方法调用
                    content = JSON.toJSONString(InvokeControllerUtils.invokeController(controllerInfo));
                } else {
                    // 有参controller层方法调用
                    Map<String, String> paramMap = parse(request);
                    content = JSON.toJSONString(InvokeControllerUtils.invokeController(
                            controllerInfo, paramMap, reqJson));
                }
            }
            // 写入响应
            httpResponse.content().writeBytes(content.getBytes());
            httpResponse.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            httpResponse.headers().setInt(HttpHeaderNames.CONTENT_LENGTH, httpResponse.content().readableBytes());
            boolean keepAlive = HttpUtil.isKeepAlive(request);
            if (keepAlive) {
                httpResponse.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
                ctx.writeAndFlush(httpResponse);
            } else {
                ctx.writeAndFlush(httpResponse).addListener(ChannelFutureListener.CLOSE);
            }
        }
    }

    /**
     * 解析请求参数
     * @return 包含所有请求参数的键值对, 如果没有参数, 则返回空Map
     */
    private Map<String, String> parse(HttpRequest request) throws Exception {
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
            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(
                    new DefaultHttpDataFactory(false), request);
            try{
                List<InterfaceHttpData> postList = decoder.getBodyHttpDatas();
                // 读取从客户端传过来的参数
                for (InterfaceHttpData data : postList) {
                    String name = data.getName();
                    String value = null;
                    if (InterfaceHttpData.HttpDataType.Attribute == data.getHttpDataType()) {
                        MemoryAttribute attribute = (MemoryAttribute) data;
                        attribute.setCharset(CharsetUtil.UTF_8);
                        value = attribute.getValue();
                    }
                    paramMap.put(name, value);
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        } else {
            // 不支持其它方法
            throw new Exception("not support method...");
        }

        return paramMap;
    }
}
