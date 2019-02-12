package com.lee.server.render;

import com.lee.server.core.RequestHandlerChain;

/**
 * @author lichujun
 * @date 2018/12/15 14:24
 */
public class DefaultRender implements Render {
    @Override
    public void render(RequestHandlerChain handlerChain) {
        int status = handlerChain.getResponseStatus();
        handlerChain.getResponse().setStatus(status);
    }
}
