package com.lee.http.server;

import com.lee.http.conf.ServerConfiguration;
import com.lee.http.handler.NettyServerHandler;
import com.lee.http.utils.TraceIDUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 以Netty做http服务器
 * @author lichujun
 * @date 2019/2/8 11:15 AM
 */
@Slf4j
public class NettyServer implements Server {

    private int port;

    public NettyServer(ServerConfiguration configuration) {
        port = configuration.getServerPort();
    }

    @Override
    public void startServer() {
        TraceIDUtils.setTraceID("main");
        // 服务端接收事件
        EventLoopGroup boss = new NioEventLoopGroup(8,
                new DefaultThreadFactory("boss"));
        // 服务端处理事件
        EventLoopGroup work = new NioEventLoopGroup(16,
                new DefaultThreadFactory("work"));
        // 服务端引导
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(boss, work)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new HttpServerCodec())

                            // 处理POST消息体时需要加上
                            .addLast(new HttpObjectAggregator(1024 * 1024))
                            .addLast(new HttpServerExpectContinueHandler())

                            .addLast(new NettyServerHandler());
                    }
                });
        try {
            // 绑定端口，同步等待成功
            ChannelFuture channelFuture = bootstrap.bind(port)
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            log.info("服务启动成功...");
                        } else {
                            log.error("端口已经被占用...");
                        }
                    }).sync();
            // 等待服务端监听端口关闭
            channelFuture.channel().closeFuture().sync();
        } catch (Throwable e) {
            log.error("绑定端口出现异常", e);
        } finally {
            TraceIDUtils.removeTraceID();
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }
    }
}
