package com.lee.mvc.render;

import com.lee.mvc.core.RequestHandlerChain;

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
