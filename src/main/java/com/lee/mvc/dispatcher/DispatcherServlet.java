package com.lee.mvc.dispatcher;

import com.lee.mvc.bean.ControllerInfo;
import com.lee.mvc.handler.ControllerHandler;
import com.lee.mvc.render.ResultRender;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author lichujun
 * @date 2018/12/14 10:53 PM
 */
public class DispatcherServlet extends HttpServlet {

    private ControllerHandler controllerHandler = ControllerHandler.getInstance();

    private ResultRender resultRender = new ResultRender();

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置请求编码方式
        req.setCharacterEncoding("UTF-8");
        //获取请求方法和请求路径
        String requestMethod = req.getMethod();
        String requestPath = req.getPathInfo();
        if (requestPath.endsWith("/")) {
            requestPath = requestPath.substring(0, requestPath.length() - 1);
        }

        ControllerInfo controllerInfo = controllerHandler.getController(requestPath, requestMethod);
        if (null == controllerInfo) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        resultRender.invokeController(req, resp, controllerInfo);
    }
}
