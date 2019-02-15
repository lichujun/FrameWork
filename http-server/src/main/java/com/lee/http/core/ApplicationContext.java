package com.lee.http.core;

import com.alibaba.fastjson.JSONObject;
import com.lee.http.conf.ServerConf;
import com.lee.http.conf.ServerConfiguration;
import com.lee.http.server.NettyServer;
import com.lee.http.server.Server;
import com.lee.ioc.core.IocAppContext;
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
    public static void run(ServerConfiguration configuration) {
        new ApplicationContext().start(configuration);
    }

    /**
     * 初始化
     */
    private void start(ServerConfiguration configuration) {
        try {
            ApplicationContext.CONFIGURATION = configuration;
            IocAppContext context = IocAppContext.getInstance();
            // 加载扫描包路径，并获取配置文件
            JSONObject yamlJson = context.init(configuration.getScanPath(),
                    configuration.getScanPackage(),
                    configuration.getBootClass());
            // 设置服务器的配置文件
            Optional.ofNullable(yamlJson)
                    .map(it -> it.getJSONObject("server"))
                    .map(it -> it.toJavaObject(ServerConf.class))
                    .map(ServerConf::getPort)
                    .ifPresent(configuration::setServerPort);
            // 扫描http服务
            ScanController scanController = ScanController.getInstance();
            scanController.init(context);
            // 启动netty服务器
            SERVER = new NettyServer(configuration);
            SERVER.startServer();
        } catch (Exception e) {
            log.error("服务器启动失败", e);
        }
    }
}
