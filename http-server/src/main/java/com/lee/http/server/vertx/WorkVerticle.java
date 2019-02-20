package com.lee.http.server.vertx;

import com.lee.http.bean.ControllerInfo;
import com.lee.http.bean.PathInfo;
import com.lee.http.bean.RequestMethod;
import com.lee.http.core.ScanController;
import com.lee.http.utils.InvokeControllerUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import org.apache.commons.collections4.MapUtils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author lichujun
 * @date 2019/2/20 7:35 PM
 */
public class WorkVerticle extends AbstractVerticle {

    @Override
    public void start() {
        EventBus eb = vertx.eventBus();
        eb.consumer("/index", message -> {
            Object obj = message.body();
            /*try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }*/
            message.reply("who are you?");
        });

        Map<PathInfo, ControllerInfo> pathControllerMap = ScanController
                .getInstance().getPathController();

        pathControllerMap.forEach((path, controller) -> {
            if (path == null || controller == null) {
                return;
            }
            if (RequestMethod.GET.name().equals(path.getHttpMethod())) {
                eb.consumer(path.getHttpPath(), message -> {
                    Object res = null;
                    Object params = message.body();
                    if (params == null) {
                        try {
                            res = InvokeControllerUtils.invokeController(controller, null, null);
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    } else {
                        try {
                            res = InvokeControllerUtils.invokeController(controller, (Map<String, String>)params, null);
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }
                    message.reply(res);
                });
            }
        });
    }
}
