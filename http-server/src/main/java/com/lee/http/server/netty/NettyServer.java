package com.lee.http.server.netty;

import com.lee.http.conf.ServerConf;
import com.lee.http.server.Server;
import com.lee.http.server.netty.handler.NettyServerHandler;
import com.lee.http.utils.TraceIDUtils;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
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

    private ServerConf conf;

    public NettyServer(ServerConf conf) {
        this.conf = conf;
    }

    @Override
    public void startServer() {
        TraceIDUtils.setTraceID("main");
        // 服务端接收事件
        EventLoopGroup boss = new NioEventLoopGroup(conf.getBossThread(),
                new DefaultThreadFactory("boss", true));
        // 服务端处理事件
        EventLoopGroup work = new NioEventLoopGroup(conf.getWorkThread(),
                new DefaultThreadFactory("work"));
        // 服务端引导
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(boss, work)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                // 编解码
                                .addLast(new HttpServerCodec())
                                // 处理POST消息体时需要加上
                                .addLast(new HttpObjectAggregator(1024 * 1024))
                                .addLast(new HttpServerExpectContinueHandler())
                                // 业务处理的handler
                                .addLast(new NettyServerHandler());
                    }
                });
        try {
            // 绑定端口，同步等待成功
            ChannelFuture channelFuture = bootstrap.bind(conf.getPort())
                    .addListener(future -> {
                        if (future.isSuccess()) {
                            log.info("服务启动成功...");
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
