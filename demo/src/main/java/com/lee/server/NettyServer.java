package com.lee.server;
import com.lee.netty.http.core.ApplicationContext;

public class NettyServer {
    public static void main(String[] args) {
        ApplicationContext.run(NettyServer.class, 9000);
    }
}
