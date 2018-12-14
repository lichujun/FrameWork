package com.lee.mvc.render;

import com.alibaba.fastjson.JSON;
import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.mvc.annotation.ResponseBody;
import com.lee.mvc.bean.ControllerInfo;
import com.lee.mvc.bean.ModelAndView;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author lichujun
 * @date 2018/12/14 9:08 PM
 */
public class ResultRender {

    /** 执行Controller方法 */
    public void invokeController(HttpServletRequest req, HttpServletResponse resp,
                                 ControllerInfo controllerInfo) {
        Map<String, String> reqParam = getRequestParam(req);
        Map<String, Class<?>> paramsClass = controllerInfo.getMethodParameter();
        List<Object> methodParams = processMethodArgs(paramsClass, reqParam);
        Class<?> controllerClass = controllerInfo.getControllerClass();
        Method method = controllerInfo.getInvokeMethod();
        method.setAccessible(true);
        Object result = Optional.ofNullable(paramsClass)
                .filter(MapUtils::isNotEmpty)
                .map(ExceptionUtils.handlerFunction(classList ->
                        method.invoke(controllerClass, methodParams.toArray())))
                .orElseGet(ExceptionUtils.handleSupplier(() ->
                        method.invoke(controllerClass)));
        resultResolve(controllerInfo, result, req, resp);
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
                            .map(it -> (Object) JSON.parseObject(it, entry.getValue()))
                            .orElse(null))
                        .collect(Collectors.toList())
                ).orElse(null);
    }

    /** 返回值解析 */
    private void resultResolve(ControllerInfo controllerInfo, Object result,
                               HttpServletRequest req, HttpServletResponse resp) {
        Optional.ofNullable(result)
                .ifPresent(res ->
                    Optional.ofNullable(controllerInfo)
                        .map(ControllerInfo::getInvokeMethod)
                        .filter(it -> it.isAnnotationPresent(ResponseBody.class))
                        .map(it -> {
                            resp.setContentType("application/json");
                            resp.setCharacterEncoding("utf-8");
                            try (PrintWriter printWriter = resp.getWriter()) {
                                printWriter.write(JSON.toJSONString(res));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            return it;
                        }).orElseGet(() -> {
                            String path = Optional.of(res)
                                    .filter(it -> it instanceof ModelAndView)
                                    .map(ModelAndView.class::cast)
                                    .map(it -> {
                                        Optional.of(it)
                                                .map(ModelAndView::getModel)
                                                .filter(MapUtils::isNotEmpty)
                                                .ifPresent(mv ->
                                                    mv.forEach(req::setAttribute)
                                                );
                                        return it.getView();
                                    }).orElseGet(() ->
                                        Optional.of(res)
                                                .filter(it -> it instanceof String)
                                                .map(String.class::cast)
                                                .orElseGet(() -> {
                                                    throw new RuntimeException("返回类型不合法");
                                                })
                                    );
                            Optional.of(path)
                                    .filter(StringUtils::isNotBlank)
                                    .ifPresent(it -> {
                                        try {
                                            req.getRequestDispatcher("/" + path).forward(req, resp);
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    });
                            return null;
                        })
                );
    }
}
