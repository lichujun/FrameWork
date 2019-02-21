package com.lee.http.bean;

import com.lee.http.server.WebServer;
import com.lee.http.server.netty.NettyWebServer;
import com.lee.http.server.vertx.VertxWebServer;
import lombok.Getter;

/**
 * web服务器枚举
 * @author lichujun
 * @date 2019/2/21 10:09 PM
 */
@Getter
public enum ServerEnums {

    NETTY("netty", NettyWebServer.class),
    VERTX("vertx", VertxWebServer.class),
    ;

    private String serverName;
    private Class<? extends WebServer> serverClass;

    ServerEnums(String serverName, Class<? extends WebServer> serverClass) {
        this.serverName = serverName;
        this.serverClass = serverClass;
    }
}
