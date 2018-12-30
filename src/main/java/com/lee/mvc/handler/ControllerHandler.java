package com.lee.mvc.handler;

import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.ioc.core.IocAppContext;
import com.lee.mvc.core.RequestHandlerChain;
import com.lee.mvc.annotation.ResponseBody;
import com.lee.mvc.bean.ControllerInfo;
import com.lee.mvc.core.ScanMvcComponent;
import com.lee.mvc.render.JsonRender;
import com.lee.mvc.render.NotFoundRender;
import com.lee.mvc.render.Render;
import com.lee.mvc.render.ViewRender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author lichujun
 * @date 2018/12/14 13:06
 */
@Slf4j
public class ControllerHandler implements Handler {


    @Override
    public boolean handle(RequestHandlerChain handlerChain) {
        String method = handlerChain.getRequestMethod();
        String path = handlerChain.getRequestPath();
        ControllerInfo controllerInfo = ScanMvcComponent.getInstance()
                .getController(path, method);
        return Optional.ofNullable(controllerInfo)
                .map(info -> {
                    Object result = invokeController(controllerInfo, handlerChain.getRequest());
                    setRender(result, controllerInfo, handlerChain);
                    return false;
                }).orElseGet(() -> {
                    handlerChain.setRender(new NotFoundRender());
                    return true;
                });
    }

    /**
     * 设置请求结果执行器
     */
    private void setRender(Object result, ControllerInfo controllerInfo, RequestHandlerChain handlerChain) {
        Optional.ofNullable(result)
                .ifPresent(res -> {
                    Render render = Optional.of(controllerInfo.getInvokeMethod())
                            .filter(it -> it.isAnnotationPresent(ResponseBody.class))
                            .map(it -> (Render) new JsonRender(res))
                            .orElseGet(() -> new ViewRender(res));
                    handlerChain.setRender(render);
                });
    }

    /** 执行Controller方法 */
    private Object invokeController(ControllerInfo controllerInfo, HttpServletRequest request) {
        Map<String, String> reqParam = getRequestParam(request);
        Map<String, Class<?>> paramsClass = controllerInfo.getMethodParameter();
        List<Object> methodParams = processMethodArgs(paramsClass, reqParam);
        Object controller = IocAppContext.getInstance().getBean(controllerInfo.getControllerClass());
        Method method = controllerInfo.getInvokeMethod();
        method.setAccessible(true);
        Object result;
        try {
            result = MapUtils.isNotEmpty(paramsClass)
                    ? method.invoke(controller, methodParams.toArray())
                    :  method.invoke(controller);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("反射调用Controller类的方法发生异常", e);
            throw new RuntimeException("反射调用Controller类的方法发生异常");
        }
        return result;
    }

    /** 获取请求参数 */
    private Map<String, String> getRequestParam(HttpServletRequest request) {
        // TODO 支持header, body
        return Optional.ofNullable(request)
                .map(req -> req.getParameterMap().entrySet().stream()
                        .filter(entry -> ArrayUtils.isNotEmpty(entry.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, it -> it.getValue()[0])))
                .orElse(null);
    }

    /** 将请求参数映射到方法参数 */
    private List<Object> processMethodArgs(Map<String, Class<?>> paramClass,
                                           Map<String, String> reqParam) {
        return Optional.ofNullable(paramClass)
                .filter(MapUtils::isNotEmpty)
                .map(Map::entrySet)
                .map(paramEntry ->
                        paramEntry.stream().map(entry -> Optional.ofNullable(reqParam)
                                .map(it -> it.get(entry.getKey()))
                                // TODO 序列化
                                .map(ExceptionUtils.handleFunction(it -> (Object) it))
                                .orElse(null))
                                .collect(Collectors.toList())
                ).orElse(null);
    }
}
