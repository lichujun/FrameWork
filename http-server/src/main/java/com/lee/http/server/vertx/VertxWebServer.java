package com.lee.http.server.vertx;

import com.lee.http.conf.ServerConf;
import com.lee.http.server.WebServer;
import com.lee.http.server.vertx.codec.HttpRequestCodec;
import com.lee.http.server.vertx.codec.HttpRequest;
import com.lee.http.server.vertx.codec.HttpResponse;
import com.lee.http.server.vertx.codec.HttpResponseCodec;
import com.lee.http.server.vertx.verticle.EventLoopVerticle;
import com.lee.http.server.vertx.verticle.WorkVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * vertx-web服务器
 */
@Slf4j
public class VertxWebServer implements WebServer {

    public static ServerConf CONF;

    @Override
    public void startServer(ServerConf conf) {
        CONF = conf;
        Vertx vertx = Vertx.vertx();
        // 设置event bus编解码，用于work-verticle解析event-loop通过event bus传递的数据
        vertx.eventBus().registerDefaultCodec(HttpRequest.class, new HttpRequestCodec());
        // 设置event bus编解码，用于event-loop解析work-verticle通过event bus传递的数据
        vertx.eventBus().registerDefaultCodec(HttpResponse.class, new HttpResponseCodec());

        // 启动work-verticle线程组
        vertx.deployVerticle(WorkVerticle.class, new DeploymentOptions()
                .setWorker(true)
                .setInstances(CONF.getWorkThread())
                .setWorkerPoolName("work-pool")
                .setMaxWorkerExecuteTimeUnit(TimeUnit.SECONDS)
                .setMaxWorkerExecuteTime(20));

        // 启动event loop线程组
        vertx.deployVerticle(EventLoopVerticle.class, new DeploymentOptions()
                .setInstances(CONF.getBossThread()));
    }
}
