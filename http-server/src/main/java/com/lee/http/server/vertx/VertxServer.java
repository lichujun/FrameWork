package com.lee.http.server.vertx;

import com.alibaba.fastjson.JSON;
import com.lee.http.bean.ControllerInfo;
import com.lee.http.bean.PathInfo;
import com.lee.http.bean.RequestMethod;
import com.lee.http.conf.ServerConf;
import com.lee.http.core.ScanController;
import com.lee.http.server.Server;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import org.apache.commons.collections4.MapUtils;
import java.util.Map;
import java.util.stream.Collectors;

public class VertxServer extends AbstractVerticle implements Server {

    private ServerConf conf;

    public VertxServer(ServerConf conf) {
        this.conf = conf;
    }

    @Override
    public void start() {
        EventBus eb = vertx.eventBus();
        Router router = Router.router(vertx);

        ScanController scanController = ScanController.getInstance();

        router.route().handler(rc -> {
            String path = rc.request().path();
            String method = rc.request().method().name();
            //ControllerInfo controllerInfo = scanController.getController(path)
        });

        Map<PathInfo, ControllerInfo> pathControllerMap = ScanController
                .getInstance().getPathController();
        pathControllerMap.forEach((path, controller) -> {
            if (path == null || controller == null) {
                return;
            }
            if (RequestMethod.GET.name().equals(path.getHttpMethod())) {
                router.get(path.getHttpPath())
                        .handler(rc -> {
                            Object msg = null;
                            if (MapUtils.isNotEmpty(controller.getMethodParameter())) {
                                msg = controller.getMethodParameter()
                                        .keySet().stream()
                                        .collect(Collectors.toMap(it -> it,
                                                it -> rc.request().getParam(it)));
                            }
                            eb.send(path.getHttpPath(), msg, res ->
                                    rc.response().end(res.result().body().toString()));
                        });
            } /*else if (RequestMethod.POST.name().equals(path.getHttpMethod())) {
                router.post(path.getHttpPath())
                        .handler(rc -> {

                        });
            } else if (RequestMethod.ALL.name().equals(path.getHttpMethod())) {
                router.route(path.getHttpPath())
                        .handler(rc -> {

                        });
            }*/
        });


        router.route("/index")
                .handler(rc -> {
                    eb.send("/index", rc.request(), res -> {
                        rc.response().end(JSON.toJSONString(res.result().body()));
                    });
                })
                .blockingHandler(request -> {
                    System.out.println("blocking。。。。");
                    request.response().end("err");
                });

        router.route("/")
                .handler(request -> {
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    request.response().end("hello");
                });
        vertx.createHttpServer().requestHandler(router).listen(conf.getPort());
    }

    @Override
    public void startServer() {
        // 设置最大响应时长30秒
        vertx = Vertx.vertx();
        //vertx.eventBus().registerDefaultCodec(Object.class, new HttpCodec());
        vertx.deployVerticle(this, new DeploymentOptions().setWorker(true)
                .setWorkerPoolName("work-pool")
                .setWorkerPoolSize(2)
                .setMaxWorkerExecuteTime(5L * 1000 * 1000 * 1000));
        vertx.deployVerticle(WorkVerticle.class, new DeploymentOptions().setWorker(true)
                .setWorkerPoolName("work-pool")
                .setWorkerPoolSize(8)
                .setMaxWorkerExecuteTime(30L * 1000 * 1000 * 1000));
    }
}
