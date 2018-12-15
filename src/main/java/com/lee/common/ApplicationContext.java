package com.lee.common;

import com.lee.conf.ServerConfiguration;
import com.lee.ioc.core.IocAppContext;
import com.lee.mvc.handler.ControllerHandler;
import com.lee.mvc.server.Server;
import com.lee.mvc.server.TomcatServer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lichujun
 * @date 2018/12/15 10:40
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ApplicationContext {

    /**
     * 全局配置
     */
    @Getter
    private static ServerConfiguration CONFIGURATION = ServerConfiguration.builder().build();

    /**
     * 默认服务器
     */
    @Getter
    private static Server SERVER;

    /**
     * 启动
     */
    public static void run(Class<?> bootClass) {
        run(ServerConfiguration.builder().bootClass(bootClass).build());
    }

    /**
     * 启动
     */
    public static void run(Class<?> bootClass, int port) {
        run(ServerConfiguration.builder().bootClass(bootClass).serverPort(port).build());
    }

    /**
     * 启动
     */
    private static void run(ServerConfiguration configuration) {
        new ApplicationContext().start(configuration);
    }

    /**
     * 初始化
     */
    private void start(ServerConfiguration configuration) {
        try {
            ApplicationContext.CONFIGURATION = configuration;
            IocAppContext.initScanPath(configuration.getScanPath());
            IocAppContext context = IocAppContext.getInstance();
            context.init();
            ControllerHandler handler = ControllerHandler.getInstance();
            handler.init(context);
            SERVER = new TomcatServer(configuration);
            SERVER.startServer();
        } catch (Exception e) {
            log.error("Doodle 启动失败", e);
        }
    }
}
