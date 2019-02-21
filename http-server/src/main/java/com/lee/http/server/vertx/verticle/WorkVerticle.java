package com.lee.http.server.vertx.verticle;

import com.alibaba.fastjson.JSON;
import com.lee.http.bean.ControllerInfo;
import com.lee.http.bean.PathInfo;
import com.lee.http.core.ScanController;
import com.lee.http.server.vertx.codec.HttpRequest;
import com.lee.http.utils.InvokeControllerUtils;
import com.lee.http.utils.TraceIDUtils;
import com.lee.iocaop.core.IocAppContext;
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
 * @author lichujun
 * @date 2019/2/20 7:35 PM
 */
@Slf4j
public class WorkVerticle extends AbstractVerticle {

    private static final IocAppContext CONTEXT = IocAppContext.getInstance();

    private static AtomicInteger count = new AtomicInteger();


    @Override
    public void start() {
        if (count.incrementAndGet() != 1) {
            return;
        }
        // 处理event loop分发过来的请求
        ScanController.getInstance().processMessage(this);
    }

    /**
     * 处理get请求
     *
     * @param path       上下文信息
     * @param controller controller信息
     */
    public void processReq(PathInfo path, ControllerInfo controller) {
        vertx.eventBus().consumer(path.getHttpMethod() + path.getHttpPath(), message -> {
            Object res;
            try {
                // 设置traceID，方便追踪日志
                String traceID = UUID.randomUUID().toString()
                        .replace("-", "")
                        .toLowerCase();
                TraceIDUtils.setTraceID(traceID);
                if (MapUtils.isEmpty(controller.getMethodParameter())) {
                    res = InvokeControllerUtils.invokeController(controller);
                } else {
                    Optional<HttpRequest> httpRequest = Optional.of(message)
                            .map(Message::body)
                            .map(it -> (HttpRequest) it);
                    Map<String, String> params = httpRequest.map(HttpRequest::getParams)
                            .orElse(null);
                    String reqJson = httpRequest.map(HttpRequest::getBody)
                            .orElse(null);
                    res = InvokeControllerUtils.invokeController(controller, params, reqJson);
                }
            } catch (Throwable e) {
                res = processException(e);
            } finally {
                // 防止ThreadLocal内存泄露
                TraceIDUtils.removeTraceID();
            }
            message.reply(JSON.toJSONString(res));
        });
    }

    /**
     * 统一处理异常
     */
    private Object processException(Throwable e) {
        Object res;
        Method method = CONTEXT.getMethod(e);
        if (method != null) {
            Class<?> tClass = method.getDeclaringClass();
            Object obj = CONTEXT.getBean(StringUtils.uncapitalize(tClass.getSimpleName()));
            try {
                res = method.invoke(obj, e);
                log.info("请求出参：【{}】", res);
            } catch (Exception exception) {
                log.warn("统一捕获异常处理发生异常", e);
                res = "内部错误";
            }
        } else {
            log.warn("unhandled exception：", e);
            res = "未知异常";
        }
        return res;
    }
}