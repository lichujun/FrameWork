package com.lee.server.dispatcher;

import com.lee.server.core.RequestHandlerChain;
import com.lee.server.handler.tomcat.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lichujun
 * @date 2018/12/14 10:53 PM
 */
public class DispatcherServlet extends HttpServlet {

    /**
     * 请求执行链
     */
    private final List<Handler> HANDLER = new ArrayList<>();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        RequestHandlerChain handlerChain = new RequestHandlerChain(HANDLER, req, resp);
        handlerChain.doHandlerChain();
        handlerChain.doRender();
    }

    @Override
    public void init() {
        HANDLER.add(new PreRequestHandler());
        HANDLER.add(new SimpleUrlHandler(getServletContext()));
        HANDLER.add(new JspHandler(getServletContext()));
        HANDLER.add(new JsonHandler());
    }
}
