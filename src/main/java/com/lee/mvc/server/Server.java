package com.lee.mvc.server;

/**
 * 服务器接口
 * @author lichujun
 * @date 2018/12/15 10:33
 */
public interface Server {

    void startServer() throws Exception;

    void stopServer() throws Exception;
}