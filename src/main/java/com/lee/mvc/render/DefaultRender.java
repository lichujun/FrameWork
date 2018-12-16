package com.lee.mvc.render;

import com.lee.mvc.core.RequestHandlerChain;

/**
 * @author lichujun
 * @date 2018/12/15 14:24
 */
public class DefaultRender implements Render {
    @Override
    public void render(RequestHandlerChain handlerChain) throws Exception {
        int status = handlerChain.getResponseStatus();
        handlerChain.getResponse().setStatus(status);
    }
}
