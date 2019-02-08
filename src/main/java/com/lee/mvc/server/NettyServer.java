package com.lee.mvc.server;

import com.lee.conf.ServerConfiguration;
import com.lee.mvc.handler.netty.NettyServerHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerExpectContinueHandler;

/**
 * 以Netty做http服务器
 * @author lichujun
 * @date 2019/2/8 11:15 AM
 */
public class NettyServer implements Server {

    private int port;

    public NettyServer(ServerConfiguration configuration) {
        port = configuration.getServerPort();
    }

    @Override
    public void startServer() {
        // 服务端接收事件
        EventLoopGroup boss = new NioEventLoopGroup();
        // 服务端处理事件
        EventLoopGroup work = new NioEventLoopGroup();
        // 服务端引导
        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(boss, work)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline cp = ch.pipeline();
                        cp.addLast(new HttpServerCodec());

                        // 处理POST消息体时需要加上
                        cp.addLast(new HttpObjectAggregator(1024*1024));
                        cp.addLast(new HttpServerExpectContinueHandler());

                        cp.addLast(new NettyServerHandler());
                    }
                });
        try {
            // 绑定端口，同步等待成功
            ChannelFuture channelFuture = bootstrap.bind(port).sync();
            // 等待服务端监听端口关闭
            channelFuture.channel().closeFuture().sync();
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            boss.shutdownGracefully();
            work.shutdownGracefully();
        }
    }

    @Override
    public void stopServer() {

    }
}
