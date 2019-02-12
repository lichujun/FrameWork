package com.lee.server.core;

import com.lee.server.handler.tomcat.Handler;
import com.lee.server.render.DefaultRender;
import com.lee.server.render.InternalErrorRender;
import com.lee.server.render.Render;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

/**
 * @author lichujun
 * @date 2018/12/15 13:57
 */
@Slf4j
@Data
public class RequestHandlerChain {

    /**
     * Handler迭代器
     */
    private List<Handler> handlers;

    /**
     * 请求request
     */
    private HttpServletRequest request;

    /**
     * 请求response
     */
    private HttpServletResponse response;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求路径
     */
    private String requestPath;

    /**
     * 请求状态码
     */
    private int responseStatus;

    /**
     * 请求结果处理器
     */
    private Render render;

    public RequestHandlerChain(List<Handler> handlers, HttpServletRequest request, HttpServletResponse response) {
        this.handlers = handlers;
        this.request = request;
        this.response = response;
        this.requestMethod = request.getMethod();
        this.requestPath = request.getPathInfo();
        this.responseStatus = HttpServletResponse.SC_OK;
    }

    /**
     * 执行请求链
     */
    public void doHandlerChain() {
        try {
            Optional.ofNullable(handlers)
                    .filter(list -> list.stream().anyMatch(it -> {
                        try {
                            return !it.handle(this);
                        } catch (Exception e) {
                            log.error("handler处理请求失败", e);
                            return false;
                        }
                    })).orElseGet(() -> {
                        log.error("找不到相应的handler处理请求：{} {}",
                                requestMethod, requestPath);
                        return null;
                    });
        } catch (Exception e) {
            log.error("doHandlerChain error", e);
            render = new InternalErrorRender();
        }
    }

    /**
     * 执行处理器
     */
    public void doRender() {
        try {
            Optional.ofNullable(render)
                    .orElseGet(DefaultRender::new)
                    .render(this);
        } catch (Exception e) {
            log.error("doRender", e);
            throw new RuntimeException(e);
        }
    }
}
