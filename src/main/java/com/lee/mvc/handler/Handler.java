package com.lee.mvc.handler;

import com.lee.mvc.core.RequestHandlerChain;

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
