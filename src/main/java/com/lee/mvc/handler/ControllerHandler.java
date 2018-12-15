package com.lee.mvc.handler;

import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.ioc.annotation.Controller;
import com.lee.ioc.core.IocAppContext;
import com.lee.mvc.RequestHandlerChain;
import com.lee.mvc.annotation.RequestMapping;
import com.lee.mvc.annotation.RequestParam;
import com.lee.mvc.annotation.ResponseBody;
import com.lee.mvc.bean.ControllerInfo;
import com.lee.mvc.bean.PathInfo;
import com.lee.mvc.bean.RequestMethod;
import com.lee.mvc.render.JsonRender;
import com.lee.mvc.render.NotFoundRender;
import com.lee.mvc.render.Render;
import com.lee.mvc.render.ViewRender;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lichujun
 * @date 2018/12/14 13:06
 */
@Slf4j
public class ControllerHandler implements Handler {

    /** 上下文信息和Controller关系集合 */
    private static Map<PathInfo, ControllerInfo> PATH_CONTROLLER = new HashMap<>();

    private ControllerHandler() {

    }

    public static ControllerHandler getInstance() {
        return ControllerHandlerHolder.INSTANCE.handler;
    }

    private enum ControllerHandlerHolder {
        // 单例
        INSTANCE;

        private ControllerHandler handler;

        ControllerHandlerHolder() {
            handler = new ControllerHandler();
        }
    }

    /** 扫描@RequestMapping进行初始化 */
    public void init(IocAppContext context) {
        Optional.ofNullable(context.getClassesByAnnotation(Controller.class))
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(this::putPathController);
    }

    /**
     * 通过上下文和请求方法获取Controller信息
     * @param path 上下文
     * @param method http请求方法
     * @return Controller信息
     */
    private ControllerInfo getController(String path, String method) {
        return PATH_CONTROLLER.get(new PathInfo(path, method));
    }

    @Override
    public boolean handle(RequestHandlerChain handlerChain) {
        String method = handlerChain.getRequestMethod();
        String path = handlerChain.getRequestPath();
        ControllerInfo controllerInfo = getController(path, method);
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
                                .map(ExceptionUtils.handlerFunction(it -> (Object) it))
                                .orElse(null))
                                .collect(Collectors.toList())
                ).orElse(null);
    }

    /**
     * 操作Controller的Class对象集合，将上下文信息和Controller关系进行绑定
     * @param classSet Controller的Class对象
     */
    private void putPathController(Set<Class<?>> classSet) {
        Optional.ofNullable(classSet)
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(set ->
                        set.forEach(this::processPathController));
    }

    /**
     * 将单个Controller的Class对象的上下文信息和Controller关系进行绑定
     * @param tClass Controller的Class对象
     */
    private void processPathController(Class<?> tClass) {
        Optional.ofNullable(tClass).ifPresent(controller -> {
            // 获取类@RequestMapping注入的值，获取Controller类的上下文
            String basePath = Optional.ofNullable(controller.getDeclaredAnnotation(
                    RequestMapping.class))
                .map(RequestMapping::value)
                .map(this::completeSeparator)
                .orElse("");
            Optional.ofNullable(controller.getDeclaredMethods())
                // 获取方法上有@RequestMapping注解的方法
                .map(methods -> Stream.of(methods)
                            .filter(method -> method.isAnnotationPresent(RequestMapping.class))
                            .collect(Collectors.toList()))
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(methods -> methods.forEach(method ->
                    Optional.ofNullable(method)
                        // 过滤返回void的方法
                        .filter(it -> !Void.TYPE.equals(it.getReturnType()))
                        .map(it -> it.getDeclaredAnnotation(RequestMapping.class))
                        .ifPresent(reqMapping -> {
                            // 获取方法上@RequestMapping注入的值，拼接上下文
                            String httpPath = basePath + Optional.of(reqMapping)
                                .map(RequestMapping::value)
                                .map(this::completeSeparator)
                                .orElse("");
                            // 请求方法
                            RequestMethod reqMethod = Optional.of(reqMapping)
                                .map(RequestMapping::method)
                                .orElse(RequestMethod.GET);
                            // 获取方法的参数
                            Map<String, Class<?>> paramMap = Optional.ofNullable(method.getParameters())
                                .filter(ArrayUtils::isNotEmpty)
                                .map(parameters -> {
                                    Map<String, Class<?>> params = new HashMap<>(8);
                                    for (Parameter parameter : parameters) {
                                        String name = Optional.of(parameter)
                                            // 获取@RequestParam注入的值，参数名
                                            .map(it -> it.getDeclaredAnnotation(RequestParam.class))
                                            .map(RequestParam::value)
                                            .filter(StringUtils::isNotBlank)
                                            .orElse(StringUtils.uncapitalize(parameter.getType()
                                                    .getSimpleName()));
                                        Class<?> paramClass = parameter.getType();
                                        Optional.ofNullable(params.put(name, paramClass))
                                            .ifPresent(it -> {
                                                throw new RuntimeException(String.format(
                                                        "参数名称不能相同，发生错误的方法：%s.%s",
                                                        controller.getName(), method.getName()));
                                            });
                                    }
                                    return params;
                                }).orElse(null);
                            PathInfo pathInfo = new PathInfo(httpPath, reqMethod.toString());
                            ControllerInfo controllerInfo = new ControllerInfo(controller, method, paramMap);
                            Optional.ofNullable(PATH_CONTROLLER.put(pathInfo, controllerInfo))
                                .ifPresent(it -> {
                                    throw new RuntimeException(String.format(
                                            "存在相同的上下文和http请求方法，controller层的方法在：%s.%s",
                                            controller.getName(), method.getName()));
                                });
                        })

                ));
        });
    }

    /**
     * 字符串前面补充/，后面去除/
     * @param origin 传入的字符串
     * @return 处理后的字符串
     */
    private String completeSeparator(String origin) {
        return Optional.ofNullable(origin)
                .map(it -> it.startsWith("/") ? it : "/" + it)
                .map(it -> it.replaceAll("[/]+$", ""))
                .orElse("");
    }
}
