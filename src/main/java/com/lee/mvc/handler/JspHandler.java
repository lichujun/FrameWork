package com.lee.mvc.handler;

import com.lee.mvc.core.MvcApplicationContext;
import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.mvc.core.RequestHandlerChain;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import java.util.Optional;

/**
 * jsp请求处理
 * 主要负责jsp资源请求
 * @author lichujun
 */
public class JspHandler implements Handler {
    /**
     * jsp请求的RequestDispatcher的名称
     */
    private static final String JSP_SERVLET = "jsp";

    /**
     * jsp的RequestDispatcher,处理jsp资源
     */
    private RequestDispatcher jspServlet;

    public JspHandler(ServletContext servletContext) {
        jspServlet = servletContext.getNamedDispatcher(JSP_SERVLET);
        Optional.ofNullable(jspServlet)
                .orElseGet(() -> {
                    throw new RuntimeException("没有jsp Servlet");
                });
    }

    @Override
    public boolean handle(final RequestHandlerChain handlerChain) {
        return Optional.of(handlerChain.getRequestPath())
                .filter(this::isPageView)
                .map(ExceptionUtils.handleFunction(it -> {
                    jspServlet.forward(handlerChain.getRequest(), handlerChain.getResponse());
                    return false;
                })).orElse(true);
    }

    /**
     * 是否为jsp资源
     */
    private boolean isPageView(String url) {
        return url.startsWith(MvcApplicationContext.getCONFIGURATION().getViewPath());
    }
}
