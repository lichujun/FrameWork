package com.lee.server;
import com.lee.netty.http.conf.ServerConfiguration;
import com.lee.netty.http.core.ApplicationContext;

public class NettyServer {
    public static void main(String[] args) {
        //ApplicationContext.run(NettyServer.class);
        ApplicationContext.run(ServerConfiguration.builder()
                .bootClass(NettyServer.class)
                .scanPath("application.yml")
                .scanPackage("scanPackages")
                .serverPort(9000)
                .build()
        );
    }
}
