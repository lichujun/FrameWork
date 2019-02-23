package com.lee.http.core;

import com.alibaba.fastjson.JSONObject;
import com.lee.http.bean.ServerEnums;
import com.lee.http.conf.ServerConf;
import com.lee.http.conf.ServerConfiguration;
import com.lee.http.utils.TraceIDUtils;
import com.lee.iocaop.core.IocAppContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author lichujun
 * @date 2018/12/15 10:40
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class ApplicationContext {

    /**
     * 启动
     */
    public static void run(Class<?> bootClass) {
        run(ServerConfiguration.builder().bootClass(bootClass).build());
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
            TraceIDUtils.setTraceID("main");
            IocAppContext context = IocAppContext.getInstance();
            // 加载扫描包路径，并获取配置文件
            JSONObject yamlJson = context.init(configuration.getScanPath(),
                    configuration.getScanPackage(),
                    configuration.getBootClass());
            // 设置服务器的配置文件
            ServerConf serverConf;
            try {
                 serverConf = Optional.ofNullable(yamlJson)
                        .map(it -> it.getJSONObject("server"))
                        .map(it -> it.toJavaObject(ServerConf.class))
                        .orElse(new ServerConf());
            } catch (Exception e) {
                log.warn("加载web服务器的配置文件失败", e);
                serverConf = new ServerConf();
            }
            // 扫描http服务
            ScanController scanController = ScanController.getInstance();
            scanController.init(context);
            // 启动服务器
            ServerEnums serverEnums = Optional.of(serverConf)
                    .map(ServerConf::getName)
                    .map(name -> Arrays.stream(ServerEnums.values())
                            .filter(it -> name.equals(it.getServerName()))
                            .findFirst().orElse(null))
                    .orElse(ServerEnums.NETTY);
            serverEnums.getServerClass().newInstance().startServer(serverConf);
        } catch (Exception e) {
            log.error("服务器启动失败", e);
        } finally {
            TraceIDUtils.removeTraceID();
        }
    }
}
