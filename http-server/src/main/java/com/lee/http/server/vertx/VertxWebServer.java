package com.lee.http.server.vertx;

import com.lee.http.conf.ServerConf;
import com.lee.http.server.WebServer;
import com.lee.http.server.vertx.codec.HttpCodec;
import com.lee.http.server.vertx.codec.HttpRequest;
import com.lee.http.server.vertx.verticle.EventLoopVerticle;
import com.lee.http.server.vertx.verticle.WorkVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

/**
 * vertx-web服务器
 */
@Slf4j
public class VertxWebServer implements WebServer {

    public static ServerConf CONF;

    @Override
    public void startServer(ServerConf conf) {
        CONF = conf;
        // 设置最大响应时长30秒
        Vertx vertx = Vertx.vertx();
        // 设置event bus编解码，用于work-verticle解析event bus传递的数据
        vertx.eventBus().registerDefaultCodec(HttpRequest.class, new HttpCodec());

        // 启动event loop线程组
        vertx.deployVerticle(EventLoopVerticle.class, new DeploymentOptions()
                .setInstances(CONF.getBossThread())
                .setMaxWorkerExecuteTime(5L * 1000 * 1000 * 1000));

        // 启动work-verticle线程组
        vertx.deployVerticle(WorkVerticle.class, new DeploymentOptions()
                .setWorker(true)
                .setInstances(CONF.getWorkThread())
                .setWorkerPoolName("work-pool")
                .setMaxWorkerExecuteTime(30L * 1000 * 1000 * 1000));

        log.info("服务启动成功");
    }
}
