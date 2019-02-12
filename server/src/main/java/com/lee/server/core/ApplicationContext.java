package com.lee.server.core;

import com.lee.conf.ServerConfiguration;
import com.lee.ioc.core.IocAppContext;
import com.lee.server.server.NettyServer;
import com.lee.server.server.Server;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

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
            IocAppContext context = IocAppContext.getInstance();
            // 加载扫描包路径
            Optional.ofNullable(configuration.getScanPath())
                    .map(it -> configuration.getBootClass().getClassLoader().getResourceAsStream(it))
                    .ifPresent(context::init);
            ScanMvcComponent scanMvcComponent = ScanMvcComponent.getInstance();
            scanMvcComponent.init(context);
            // SERVER = new TomcatServer(configuration);
            SERVER = new NettyServer(configuration);
            SERVER.startServer();
        } catch (Exception e) {
            log.error("服务器启动失败", e);
        }
    }
}
