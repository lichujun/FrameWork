package com.lee.http.server.vertx.verticle;

import com.lee.http.bean.ControllerInfo;
import com.lee.http.bean.MethodParam;
import com.lee.http.bean.PathInfo;
import com.lee.http.bean.enums.RequestMethod;
import com.lee.http.bean.enums.ContentType;
import com.lee.http.core.ScanController;
import com.lee.http.server.vertx.VertxWebServer;
import com.lee.http.server.vertx.codec.HttpRequest;
import com.lee.http.server.vertx.codec.HttpResponse;
import com.lee.http.server.vertx.parser.Parser;
import com.lee.http.utils.AsyncResultUtils;
import com.lee.http.utils.ParseParamUtils;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import java.util.*;

/**
 * event loop
 * @author lichujun
 * @date 2019/2/20 7:35 PM
 */
public class EventLoopVerticle extends AbstractVerticle {

    private Router router;
    @Override
    public void start() {
        this.router = Router.router(vertx);
        ScanController scanController = ScanController.getInstance();

        // 路由请求
        scanController.routeMessage(this);

        // 监听端口
        vertx.createHttpServer(new HttpServerOptions()
                .setMaxWebsocketFrameSize(1024 * 1024 * 10)
                .setCompressionSupported(true)
                .setTcpKeepAlive(true)
                .setReuseAddress(true))
                .requestHandler(router)
                .listen(VertxWebServer.CONF.getPort());
    }

    /**
     * 路由请求
     */
    public void routeReq(PathInfo path, ControllerInfo controller) {
        if (HttpMethod.GET.name().equals(path.getHttpMethod())) {
            routeGetReq(path, controller);
        } else if (HttpMethod.POST.name().equals(path.getHttpMethod())) {
            routePostReq(path, controller);
        }
    }

    /**
     * 404请求
     */
    public void routeNotFound() {
        router.route().handler(rc -> rc.response()
                .setStatusCode(HttpResponseStatus.NOT_FOUND.code())
                .end());
    }

    /**
     * 路由GET请求
     */
    private void routeGetReq(PathInfo pathInfo, ControllerInfo controllerInfo) {
        router.get(pathInfo.getHttpPath())
                .handler(rc ->
                    processRoute(pathInfo, controllerInfo, rc, RequestMethod.GET)
                );
    }

    /**
     * 路由POST请求
     */
    private void routePostReq(PathInfo pathInfo, ControllerInfo controllerInfo) {
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
        eb.send(path, msg, res -> {
            HttpResponse<String> httpResponse = AsyncResultUtils.transResponse(res);
            if (HttpResponseStatus.OK.equals(httpResponse.getStatus())) {
                rc.response()
                        .putHeader("Content-type", "text/plain;charset=UTF-8")
                        .end(httpResponse.getResponse());
            } else {
                HttpResponseStatus status = httpResponse.getStatus();
                if (status == null) {
                    status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
                }
                rc.response()
                        .setStatusCode(status.code())
                        .end();
            }
        });
    }

    /**
     * 进行请求解析参数和分发请求
     */
    private void processRoute(PathInfo pathInfo, ControllerInfo controllerInfo,
                       RoutingContext rc, RequestMethod requestMethod) {
        EventBus eb = vertx.eventBus();
        // event bus传递消息的路径
        String path = pathInfo.getHttpMethod() + pathInfo.getHttpPath();
        Map<String, MethodParam> paramMap = controllerInfo.getMethodParameter();
        // 入参为空，则无需解析请求参数
        if (MapUtils.isEmpty(paramMap)) {
            sendMessage(eb, path, null, rc);
            return;
        }
        final Map<String, String> params = new HashMap<>();
        List<Object> paramList = null;
        // 获取请求参数
        if (RequestMethod.POST.equals(requestMethod)) {
            for (String param : paramMap.keySet()) {
                Optional.ofNullable(rc.request().getFormAttribute(param))
                        .ifPresent(it -> params.put(param, it));
            }
            // 获取body
            if (MapUtils.isEmpty(params) && paramMap.size() == 1) {
                String contentType = rc.request().getHeader("Content-Type");
                Parser parser = ContentType.getParser(contentType);
                if (parser != null) {
                    String body = rc.getBodyAsString();
                    MethodParam methodParam = paramMap.values().stream()
                            .findFirst()
                            .orElse(null);
                    if (methodParam != null) {
                        paramList = Optional.ofNullable(parser.parse(methodParam, body))
                                .map(Collections::singletonList)
                                .orElse(null);
                    }
                }
            } else {
                paramList = ParseParamUtils.parse(controllerInfo.getMethodParameter(), params);
            }
        } else if (RequestMethod.GET.equals(requestMethod)){
            for (String param : paramMap.keySet()) {
                Optional.ofNullable(rc.request().getParam(param))
                        .ifPresent(it -> params.put(param, it));
            }
            if (MapUtils.isNotEmpty(params)) {
                paramList = ParseParamUtils.parse(controllerInfo.getMethodParameter(), params);
            }
        }
        if (CollectionUtils.isEmpty(paramList)) {
            rc.response()
                    .setStatusCode(HttpResponseStatus.BAD_REQUEST.code())
                    .end();
            return;
        }
        HttpRequest httpRequest = new HttpRequest(paramList);
        sendMessage(eb, path, httpRequest, rc);
    }

}
