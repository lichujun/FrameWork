package com.lee.mvc.handler;

import com.lee.common.ApplicationContext;
import com.lee.mvc.RequestHandlerChain;
import lombok.extern.slf4j.Slf4j;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import java.util.Optional;

/**
 * @author lichujun
 * @date 2018/12/15 14:10
 */
@Slf4j
public class SimpleUrlHandler implements Handler {
    /**
     * tomcat默认RequestDispatcher的名称
     */
    private static final String TOMCAT_DEFAULT_SERVLET = "default";

    /**
     * 默认的RequestDispatcher,处理静态资源
     */
    private RequestDispatcher defaultServlet;

    public SimpleUrlHandler(ServletContext servletContext) {
        defaultServlet = servletContext.getNamedDispatcher(TOMCAT_DEFAULT_SERVLET);

        Optional.ofNullable(defaultServlet)
                .orElseGet(() -> {
                    throw new RuntimeException("没有默认的Servlet");
                });
        log.info("The default servlet for serving static resource is [{}]", TOMCAT_DEFAULT_SERVLET);
    }


    @Override
    public boolean handle(final RequestHandlerChain handlerChain) throws Exception {
        if (isStaticResource(handlerChain.getRequestPath())) {
            defaultServlet.forward(handlerChain.getRequest(), handlerChain.getResponse());
            return false;
        }
        return true;
    }

    /**
     * 是否为静态资源
     */
    private boolean isStaticResource(String url) {
        return url.startsWith(ApplicationContext.getCONFIGURATION().getAssetPath());
    }
}
