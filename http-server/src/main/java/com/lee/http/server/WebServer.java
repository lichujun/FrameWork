package com.lee.http.server;

import com.lee.http.conf.ServerConf;

/**
 * 服务器接口
 * @author lichujun
 * @date 2018/12/15 10:33
 */
public interface WebServer {

    void startServer(ServerConf conf) throws Exception;
}
