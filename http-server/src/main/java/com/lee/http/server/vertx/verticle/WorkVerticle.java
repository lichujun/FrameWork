package com.lee.http.server.vertx.verticle;

import com.alibaba.fastjson.JSON;
import com.lee.http.bean.ControllerInfo;
import com.lee.http.bean.PathInfo;
import com.lee.http.core.ScanController;
import com.lee.http.server.vertx.codec.HttpRequest;
import com.lee.http.server.vertx.codec.HttpResponse;
import com.lee.http.utils.InvokeControllerUtils;
import com.lee.http.utils.TraceIDUtils;
import com.lee.iocaop.core.IocAppContext;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * work-verticle
 * @author lichujun
 * @date 2019/2/20 7:35 PM
 */
@Slf4j
public class WorkVerticle extends AbstractVerticle {

    private static final IocAppContext CONTEXT = IocAppContext.getInstance();

    private static AtomicInteger count = new AtomicInteger();

    @Override
    public void start() {
        // 防止多次初始化
        if (count.incrementAndGet() != 1) {
            return;
        }
        // 处理event loop分发过来的请求
        ScanController.getInstance().processMessage(this);
    }

    /**
     * 处理event bus分发过来的请求
     *
     * @param path 上下文信息
     * @param controller controller信息
     */
    public void processReq(PathInfo path, ControllerInfo controller) {
        vertx.eventBus().consumer(path.getHttpMethod() + path.getHttpPath(), message -> {
            HttpResponse httpResponse = null;
            try {
                // 设置traceID，方便追踪日志
                String traceID = UUID.randomUUID().toString()
                        .replace("-", "")
                        .toLowerCase();
                TraceIDUtils.setTraceID(traceID);
                Object res;
                // 如果无参，直接调用
                if (MapUtils.isEmpty(controller.getMethodParameter())) {
                    log.info("请求路径：【{}】，无需请求参数", path.getHttpPath());
                    res = InvokeControllerUtils.invokeController(controller);
                } else {
                    // 获取event bus传递过来的参数
                    Optional<HttpRequest> httpRequest = Optional.of(message)
                            .map(Message::body)
                            .map(it -> (HttpRequest) it);
                    Map<String, String> params = httpRequest.map(HttpRequest::getParams)
                            .orElse(null);
                    String request = httpRequest.map(HttpRequest::getBody)
                            .orElse(null);
                    String reqJson = StringUtils.deleteWhitespace(request);
                    log.info("请求路径：【{}】，请求参数：【{}】", path.getHttpPath(),
                            MapUtils.isEmpty(params) ? reqJson : params);
                    res = InvokeControllerUtils.invokeController(controller, params, reqJson);
                }
                httpResponse = HttpResponse.builder()
                        .status(HttpResponseStatus.OK)
                        .response(JSON.toJSONString(res))
                        .build();
            } catch (Throwable e) {
                httpResponse = processException(e);
            } finally {
                String resStr = Optional.ofNullable(httpResponse)
                        .map(HttpResponse::getResponse)
                        .orElse(null);
                message.reply(httpResponse);
                log.info("请求出参：【{}】", resStr);
                // 防止ThreadLocal内存泄露
                TraceIDUtils.removeTraceID();
            }
        });
    }

    /**
     * 统一处理异常
     */
    private HttpResponse processException(Throwable e) {
        HttpResponse httpResponse;
        Method method = CONTEXT.getProcessExceptionMethod(e);
        if (method != null) {
            Class<?> tClass = method.getDeclaringClass();
            Object obj = CONTEXT.getBean(StringUtils.uncapitalize(tClass.getSimpleName()));
            try {
                Object res = method.invoke(obj, e);
                httpResponse = HttpResponse.builder()
                        .status(HttpResponseStatus.OK)
                        .response(JSON.toJSONString(res))
                        .build();
            } catch (Exception exception) {
                log.warn("统一捕获异常处理发生异常", e);
                httpResponse = HttpResponse.builder()
                        .status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                        .build();
            }
        } else {
            log.warn("unhandled exception：", e);
            httpResponse = HttpResponse.builder()
                    .status(HttpResponseStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
        return httpResponse;
    }
}
