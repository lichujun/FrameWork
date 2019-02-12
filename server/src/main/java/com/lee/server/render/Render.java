package com.lee.server.render;

import com.lee.server.core.RequestHandlerChain;

/**
 * @author lichujun
 * @date 2018/12/15 13:56
 */
public interface Render {

    /**
     * 执行渲染
     */
    void render(RequestHandlerChain handlerChain) throws Exception;
}
