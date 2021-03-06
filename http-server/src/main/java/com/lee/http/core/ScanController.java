package com.lee.http.core;

import com.lee.http.annotation.RequestMapping;
import com.lee.http.annotation.RequestParam;
import com.lee.http.bean.ControllerInfo;
import com.lee.http.bean.MethodParam;
import com.lee.http.bean.PathInfo;
import com.lee.http.bean.enums.RequestMethod;
import com.lee.http.server.vertx.verticle.EventLoopVerticle;
import com.lee.http.server.vertx.verticle.WorkVerticle;
import com.lee.iocaop.annotation.Controller;
import com.lee.iocaop.core.IocAppContext;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lichujun
 * @date 2018/12/16 11:40 PM
 */
public class ScanController {

    /** 上下文信息和Controller关系集合 */
    private static final Map<PathInfo, ControllerInfo> PATH_CONTROLLER = new HashMap<>();

    public static ScanController getInstance() {
        return ScanComponentHolder.INSTANCE.scanController;
    }

    private enum ScanComponentHolder {
        // 单例
        INSTANCE;

        private ScanController scanController;

        ScanComponentHolder() {
            scanController = new ScanController();
        }
    }

    /** 扫描@RequestMapping进行初始化 */
    void init(IocAppContext context) {
        Optional.ofNullable(context.getClassesByAnnotation(Controller.class))
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(this::putPathController);
        context.releaseResource();
    }

    /**
     * 通过上下文和请求方法获取Controller信息
     * @param path 上下文
     * @param method http请求方法
     * @return Controller信息
     */
    public ControllerInfo getController(String path, String method) {
        return PATH_CONTROLLER.get(new PathInfo(path, method));
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
        if (tClass == null) {
            return;
        }
        // 获取类@RequestMapping注入的值，获取Controller类的上下文
        String basePath = Optional.ofNullable(tClass.getDeclaredAnnotation(
                RequestMapping.class))
                .map(RequestMapping::value)
                .map(this::completeSeparator)
                .orElse("");
        List<Method> methodList = Optional.ofNullable(tClass.getDeclaredMethods())
                // 获取方法上有@RequestMapping注解的方法
                .map(methods -> Stream.of(methods)
                        .filter(method -> method.isAnnotationPresent(RequestMapping.class))
                        .collect(Collectors.toList())
                )
                .orElse(null);
        if (CollectionUtils.isEmpty(methodList)) {
            return;
        }
        // 扫描所有方法的RequestMapping注解
        for (Method method : methodList) {
            RequestMapping reqMapping = Optional.ofNullable(method)
                    .filter(it -> !Void.TYPE.equals(it.getReturnType()))
                    .map(it -> it.getDeclaredAnnotation(RequestMapping.class))
                    .orElse(null);
            if (reqMapping == null) {
                continue;
            }
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
            Map<String, MethodParam> paramMap = null;
            if (ArrayUtils.isNotEmpty(method.getParameters())) {
                paramMap = new LinkedHashMap<>(8);
                for (Parameter parameter : method.getParameters()) {
                    String name = Optional.of(parameter)
                            // 获取@RequestParam注入的值，参数名
                            .map(it -> it.getDeclaredAnnotation(RequestParam.class))
                            .map(RequestParam::value)
                            .filter(StringUtils::isNotBlank)
                            .orElse(StringUtils.uncapitalize(parameter.getType()
                                    .getSimpleName()));
                    Type type = parameter.getParameterizedType();
                    Class<?> paramClass = parameter.getType();
                    MethodParam methodParam = new MethodParam(paramClass, type);
                    if (paramMap.put(name, methodParam) != null) {
                        throw new RuntimeException(String.format(
                                "参数名称不能相同，发生错误的方法：%s.%s",
                                tClass.getName(), method.getName()));
                    }
                }
            }
            if (reqMethod == RequestMethod.ALL) {
                for (RequestMethod reqMethodEnum : RequestMethod.values()) {
                    if (reqMethodEnum == RequestMethod.ALL) {
                        continue;
                    }
                    putControllerInfo(httpPath, reqMethodEnum, tClass, method, paramMap);
                }
            } else {
                putControllerInfo(httpPath, reqMethod, tClass, method, paramMap);
            }
        }
    }

    /**
     * 字符串前面补充/
     * @param origin 传入的字符串
     * @return 处理后的字符串
     */
    private String completeSeparator(String origin) {
        return Optional.ofNullable(origin)
                .map(it -> it.startsWith("/") ? it : "/" + it)
                .orElse("");
    }

    /**
     * 将ControllerInfo信息存放在容器中
     */
    private void putControllerInfo(String httpPath, RequestMethod reqMethod,
                                   Class<?> tClass, Method method,
                                   Map<String, MethodParam> paramMap) {
        PathInfo pathInfo = new PathInfo(httpPath, reqMethod.toString());
        ControllerInfo controllerInfo = new ControllerInfo(tClass, method, paramMap);
        Optional.ofNullable(PATH_CONTROLLER.put(pathInfo, controllerInfo))
                .ifPresent(it -> {
                    throw new RuntimeException(String.format(
                            "存在相同的上下文和http请求方法，controller层的方法在：%s.%s",
                            tClass.getName(), method.getName()));
                });
    }

    /**
     * 接收vertx来自event loop分发过来的请求
     */
    public void processMessage(WorkVerticle work) {
        PATH_CONTROLLER.forEach(work::processReq);
    }

    /**
     * event loop路由请求
     */
    public void routeMessage(EventLoopVerticle loop) {
        PATH_CONTROLLER.forEach(loop::routeReq);
        loop.routeNotFound();
    }

}
