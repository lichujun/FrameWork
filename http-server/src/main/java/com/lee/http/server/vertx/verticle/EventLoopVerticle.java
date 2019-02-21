package com.lee.http.server.vertx.verticle;

import com.lee.http.bean.ControllerInfo;
import com.lee.http.bean.PathInfo;
import com.lee.http.core.ScanController;
import com.lee.http.server.vertx.VertxServer;
import com.lee.http.server.vertx.codec.HttpRequest;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.collections4.MapUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

public class EventLoopVerticle extends AbstractVerticle {

    private Router router;
    private static final AtomicInteger count = new AtomicInteger();

    @Override
    public void start() {
        if (count.incrementAndGet() != 1) {
            return;
        }
        this.router = Router.router(vertx);
        ScanController scanController = ScanController.getInstance();

        // 路由请求
        scanController.routeMessage(this);

        vertx.createHttpServer().requestHandler(router).listen(VertxServer.CONF.getPort());
    }

    /**
     * 路由GET请求
     */
    public void routeGetReq(PathInfo pathInfo, ControllerInfo controllerInfo) {
        EventBus eb = vertx.eventBus();
        String path = pathInfo.getHttpMethod() + pathInfo.getHttpPath();
        router.get(pathInfo.getHttpPath())
                .handler(rc -> {
                    if (MapUtils.isEmpty(controllerInfo.getMethodParameter())) {
                        sendMessage(eb, path, null, rc);
                        return;
                    }
                    Map<String, String> params = new HashMap<>();
                    for (String param : controllerInfo.getMethodParameter().keySet()) {
                        params.put(param, rc.request().getParam(param));
                    }
                    HttpRequest httpRequest = HttpRequest.builder()
                            .params(params)
                            .body(null)
                            .build();
                    sendMessage(eb, path, httpRequest, rc);
                });
    }

    /**
     * 路由POST请求
     */
    public void routePostReq(PathInfo pathInfo, ControllerInfo controllerInfo) {
        EventBus eb = vertx.eventBus();
        String path = pathInfo.getHttpMethod() + pathInfo.getHttpPath();
        router.post(pathInfo.getHttpPath())
                .handler(BodyHandler.create())
                .handler(rc -> {
                    if (MapUtils.isEmpty(controllerInfo.getMethodParameter())) {
                        sendMessage(eb, path, null, rc);
                        return;
                    }
                    Map<String, String> params = new HashMap<>();
                    for (String param : controllerInfo.getMethodParameter().keySet()) {
                        Optional.ofNullable(rc.request().getFormAttribute(param))
                                .ifPresent(it -> params.put(param, it));
                    }
                    String body = null;
                    if (MapUtils.isEmpty(params)) {
                        body = rc.getBodyAsString();
                    }
                    HttpRequest httpRequest = HttpRequest.builder()
                            .params(params)
                            .body(body)
                            .build();
                    sendMessage(eb, path, httpRequest, rc);
                });
    }

    /**
     * EventBus发送消息
     */
    private void sendMessage(EventBus eb, String path, Object msg, RoutingContext rc) {
        eb.send(path, msg, res ->
                rc.response().end(res.result().body().toString()));
    }

}
