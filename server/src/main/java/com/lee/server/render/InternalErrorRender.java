package com.lee.server.render;

import com.lee.server.core.RequestHandlerChain;

import javax.servlet.http.HttpServletResponse;

/**
 * @author lichujun
 * @date 2018/12/15 14:25
 */
public class InternalErrorRender implements Render {
    @Override
    public void render(RequestHandlerChain handlerChain) throws Exception {
        handlerChain.getResponse().sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
}