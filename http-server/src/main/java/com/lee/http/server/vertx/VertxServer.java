package com.lee.http.server.vertx;

import com.lee.http.conf.ServerConf;
import com.lee.http.server.Server;
import com.lee.http.server.vertx.codec.HttpCodec;
import com.lee.http.server.vertx.codec.HttpRequest;
import com.lee.http.server.vertx.verticle.EventLoopVerticle;
import com.lee.http.server.vertx.verticle.WorkVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class VertxServer implements Server {

    public static ServerConf CONF;

    public VertxServer(ServerConf conf) {
        CONF = conf;
    }

    @Override
    public void startServer() {
        // 设置最大响应时长30秒
        Vertx vertx = Vertx.vertx();
        vertx.eventBus().registerDefaultCodec(HttpRequest.class, new HttpCodec());

        vertx.deployVerticle(EventLoopVerticle.class, new DeploymentOptions()
                .setInstances(CONF.getBossThread())
                .setMaxWorkerExecuteTime(5L * 1000 * 1000 * 1000));

        vertx.deployVerticle(WorkVerticle.class, new DeploymentOptions()
                .setWorker(true)
                .setInstances(CONF.getWorkThread())
                .setWorkerPoolName("work-pool")
                .setMaxWorkerExecuteTime(30L * 1000 * 1000 * 1000));
    }
}
