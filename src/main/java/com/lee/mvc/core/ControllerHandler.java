package com.lee.mvc.core;

import com.lee.ioc.annotation.Controller;
import com.lee.ioc.core.IocAppContext;
import com.lee.mvc.annotation.RequestMapping;
import com.lee.mvc.annotation.RequestParam;
import com.lee.mvc.bean.ControllerInfo;
import com.lee.mvc.bean.PathInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author lichujun
 * @date 2018/12/14 13:06
 */
public class ControllerHandler {

    /** 上下文信息和Controller关系集合 */
    private static Map<PathInfo, ControllerInfo> PATH_CONTROLLER  = new HashMap<>();
    /** 路径集合 */
    private static Set<String> PATH_SET = new HashSet<>();

    public ControllerHandler() {
        Optional.ofNullable(IocAppContext.getClassesByAnnotation(Controller.class))
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(this::putPathController);
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
        Optional.ofNullable(tClass)
            .ifPresent(controller -> {
                String basePath = Optional.ofNullable(controller.getDeclaredAnnotation(
                        RequestMapping.class))
                    .map(RequestMapping::value)
                    .map(this::completeSeparator)
                    .orElse("");
                Optional.ofNullable(controller.getDeclaredMethods())
                    .map(methods -> Stream.of(methods)
                        .filter(method -> method.isAnnotationPresent(RequestMapping.class))
                        .collect(Collectors.toList()))
                    .filter(CollectionUtils::isNotEmpty)
                    .ifPresent(methods -> methods.forEach(method -> {
                        Optional<RequestMapping> reqMappingOptional = Optional.ofNullable(
                                method.getDeclaredAnnotation(RequestMapping.class));
                        String httpPath = reqMappingOptional
                            .map(RequestMapping::value)
                            .map(this::completeSeparator)
                            .orElse("") + basePath;
                        RequestMethod reqMethod = reqMappingOptional
                                .map(RequestMapping::method)
                                .orElse(RequestMethod.GET);
                        Map<String, Class<?>> paramMap = Optional.ofNullable(method.getParameters())
                            .filter(ArrayUtils::isNotEmpty)
                            .map(parameters -> {
                                Map<String, Class<?>> params = new HashMap<>(8);
                                for (Parameter parameter : parameters) {
                                    String name = Optional.of(parameter)
                                        .map(it -> it.getDeclaredAnnotation(RequestParam.class))
                                        .map(RequestParam::value)
                                        .filter(StringUtils::isNotBlank)
                                        .orElse(parameter.getName());
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
                        PathInfo pathInfo = new PathInfo(httpPath, reqMethod);
                        ControllerInfo controllerInfo = new ControllerInfo(controller, method, paramMap);
                        Optional.ofNullable(PATH_CONTROLLER.put(pathInfo, controllerInfo))
                               ;
                    }));
            });
    }

    /**
     * 字符串前面补充/，后面去除/
     * @param origin 传入的字符串
     * @return 处理后的字符串
     */
    private String completeSeparator(String origin) {
        return Optional.ofNullable(origin)
                .map(it -> it.replaceAll("^[/]+", "/"))
                .map(it -> it.startsWith("/") ? it : "/" + it)
                .map(it -> it.replaceAll("[/]+$", ""))
                .orElse("");
    }
}
