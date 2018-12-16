package com.lee.mvc.render;

import com.lee.mvc.core.RequestHandlerChain;

import javax.servlet.http.HttpServletResponse;

/**
 * @author lichujun
 * @date 2018/12/15 14:25
 */
public class NotFoundRender implements Render {
    @Override
    public void render(RequestHandlerChain handlerChain) throws Exception {
        handlerChain.getResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
    }
}
