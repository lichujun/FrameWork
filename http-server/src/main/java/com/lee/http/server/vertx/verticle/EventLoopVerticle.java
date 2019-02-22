package com.lee.http.server.vertx.verticle;

import com.lee.http.bean.ControllerInfo;
import com.lee.http.bean.PathInfo;
import com.lee.http.bean.RequestMethod;
import com.lee.http.core.ScanController;
import com.lee.http.server.vertx.VertxWebServer;
import com.lee.http.server.vertx.codec.HttpRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * event loop
 * @author lichujun
 * @date 2019/2/20 7:35 PM
 */
@Slf4j
public class EventLoopVerticle extends AbstractVerticle {

    private Router router;
    @Override
    public void start() {
        this.router = Router.router(vertx);
        ScanController scanController = ScanController.getInstance();

        // 路由请求
        scanController.routeMessage(this);

        // 监听端口
        vertx.createHttpServer()
                .requestHandler(router)
                .listen(VertxWebServer.CONF.getPort());
    }

    /**
     * 路由GET请求
     */
    public void routeGetReq(PathInfo pathInfo, ControllerInfo controllerInfo) {
        router.get(pathInfo.getHttpPath())
                .handler(rc ->
                    processRoute(pathInfo, controllerInfo, rc, RequestMethod.GET)
                );
    }

    /**
     * 路由POST请求
     */
    public void routePostReq(PathInfo pathInfo, ControllerInfo controllerInfo) {
        router.post(pathInfo.getHttpPath())
                // 为获取post请求的body，必须要加
                .handler(BodyHandler.create())
                .handler(rc ->
                        processRoute(pathInfo, controllerInfo, rc, RequestMethod.POST)
                );
    }

    /**
     * EventBus发送消息
     */
    private void sendMessage(EventBus eb, String path, Object msg, RoutingContext rc) {
        eb.send(path, msg, res ->
                rc.response().putHeader("Content-type", "text/plain;charset=UTF-8")
                        .end(res.result().body().toString()));
    }

    /**
     * 处理路由
     */
    private void processRoute(PathInfo pathInfo, ControllerInfo controllerInfo,
                       RoutingContext rc, RequestMethod requestMethod) {
        EventBus eb = vertx.eventBus();
        // event bus传递消息的路径
        String path = pathInfo.getHttpMethod() + pathInfo.getHttpPath();
        Map<String, Class<?>> paramMap = controllerInfo.getMethodParameter();
        // 入参为空，则无需解析请求参数
        if (MapUtils.isEmpty(paramMap)) {
            sendMessage(eb, path, null, rc);
            return;
        }
        final Map<String, String> params = new HashMap<>();
        String body = null;
        // 获取请求参数
        if (RequestMethod.POST.equals(requestMethod)) {
            for (String param : paramMap.keySet()) {
                Optional.ofNullable(rc.request().getFormAttribute(param))
                        .ifPresent(it -> params.put(param, it));
            }
            // 获取body
            if (MapUtils.isEmpty(params) && paramMap.size() == 1) {
                body = rc.getBodyAsString();
            }
        } else if (RequestMethod.GET.equals(requestMethod)){
            for (String param : paramMap.keySet()) {
                Optional.ofNullable(rc.request().getParam(param))
                        .ifPresent(it -> params.put(param, it));
            }
        }
        HttpRequest httpRequest = HttpRequest.builder()
                .params(params)
                .body(body)
                .build();
        sendMessage(eb, path, httpRequest, rc);
    }

}
