package com.lee.mvc.handler;

import com.lee.mvc.core.RequestHandlerChain;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lichujun
 * @date 2018/12/15 14:08
 */
@Slf4j
public class PreRequestHandler implements Handler {
    @Override
    public boolean handle(RequestHandlerChain handlerChain) throws Exception {
        // 设置请求编码方式
        handlerChain.getRequest().setCharacterEncoding("UTF-8");
        String requestPath = handlerChain.getRequestPath().replaceAll("[/]+$", "");
        handlerChain.setRequestPath(requestPath);
        log.info("[请求] {} {}", handlerChain.getRequestMethod(), handlerChain.getRequestPath());
        return true;
    }
}
