package com.lee.server.handler.tomcat;

import com.lee.server.core.RequestHandlerChain;

/**
 * @author lichujun
 * @date 2018/12/15 13:56
 */
public interface Handler {

    /**
     * 请求执行器
     */
    boolean handle(final RequestHandlerChain handlerChain) throws Exception;
}
