package com.lee.http.server;

import com.lee.http.conf.ServerConf;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;

public class VertxServer extends AbstractVerticle implements Server {

    private ServerConf conf;

    public VertxServer(ServerConf conf) {
        this.conf = conf;
    }

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        // 监听/index地址
        router.route("/index").handler(request -> {
            request.response().end("INDEX SUCCESS");
        });

        // 监听/index地址
        router.route("/")
                .handler(request -> {
                    try {
                        Thread.sleep(30000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    request.response().end("hello");
                })
                .blockingHandler(request -> {
                    System.out.println("blocking。。。。");
                    request.response().end("err");
                })
                .failureHandler(request -> {
                    request.response().end("err");
                });

        server.requestHandler(router::accept).listen(conf.getPort());
    }

    @Override
    public void startServer() {
        // 设置最大响应时长30秒
        VertxOptions vertxOptions = new VertxOptions()
                .setMaxEventLoopExecuteTime(30L * 1000 * 1000 * 1000)
                .setWorkerPoolSize(2);
        this.vertx = Vertx.vertx(vertxOptions);
        vertx.deployVerticle(this, new DeploymentOptions().setWorker(true)
                .setWorkerPoolName("work-pool")
                .setWorkerPoolSize(1)
                .setMaxWorkerExecuteTime(1L * 1000 * 1000 * 1000));
    }
}
