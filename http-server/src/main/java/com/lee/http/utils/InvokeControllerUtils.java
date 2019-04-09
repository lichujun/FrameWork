package com.lee.http.utils;

import com.lee.http.bean.ControllerInfo;
import com.lee.http.bean.MethodParam;
import com.lee.iocaop.core.IocAppContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author lichujun
 * @date 2019/2/8 3:03 PM
 */
@Slf4j
public class InvokeControllerUtils {

    /**
     * 调用controller的方法
     * @param controllerInfo controller的信息
     * @return 返回报文
     */
    public static Object invokeController(ControllerInfo controllerInfo) throws Throwable {
        Object controller = IocAppContext.getInstance()
                .getBean(controllerInfo.getControllerClass());
        Method method = controllerInfo.getInvokeMethod();
        method.setAccessible(true);
        try {
            return method.invoke(controller);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * 调用controller的方法
     * @param controllerInfo controller的信息
     * @param paramList 参数集合
     * @return 返回报文
     */
    public static Object invokeController(ControllerInfo controllerInfo, List<Object> paramList) throws Throwable {
        Object controller = IocAppContext.getInstance()
                .getBean(controllerInfo.getControllerClass());
        Map<String, MethodParam> paramClassMap = controllerInfo.getMethodParameter();
        if (MapUtils.isEmpty(paramClassMap)) {
            return invokeController(controllerInfo);
        }
        return invokeControllerMethod(controllerInfo, controller, paramList);
    }

    /**
     * 调用controller的方法
     * @param controllerInfo controller的信息
     * @param paramMap 参数Map
     * @return 返回报文
     */
    public static Object invokeController(ControllerInfo controllerInfo,
                                          Map<String, String> paramMap,
                                          String reqJson) throws Throwable {
        Object controller = IocAppContext.getInstance()
                .getBean(controllerInfo.getControllerClass());
        Map<String, MethodParam> paramClassMap = controllerInfo.getMethodParameter();
        if (MapUtils.isEmpty(paramClassMap)) {
            return invokeController(controllerInfo);
        } else {
            List<Object> paramList = new ArrayList<>();
            // 若paramMap为空，则将raw body的数据反序列化成object对象
            if (MapUtils.isEmpty(paramMap) && paramClassMap.size() == 1) {
                MethodParam methodParam = paramClassMap.entrySet().stream()
                        .findFirst()
                        .map(Map.Entry::getValue)
                        .orElse(null);
                if (methodParam != null) {
                    try {
                        Object param = Optional.ofNullable(reqJson)
                                .filter(StringUtils::isNotBlank)
                                .map(it -> CastUtils.convert(it, methodParam.getType(),
                                        methodParam.getParamClass()))
                                .orElse(null);
                        paramList.add(param);
                    } catch (Throwable e) {
                        log.warn("参数反序列化出现异常", e);
                    }
                } else {
                    paramList.add(null);
                }
            } else {
                paramClassMap.forEach((paramName, methodParam) -> {
                    Object param = getParamObject(paramMap, paramName, methodParam);
                    paramList.add(param);
                });
            }
            return invokeControllerMethod(controllerInfo, controller, paramList);
        }
    }

    private static Object invokeControllerMethod(ControllerInfo controllerInfo,
                                                 Object controller,
                                                 List<Object> paramList) throws Throwable {
        try {
            Method method = controllerInfo.getInvokeMethod();
            method.setAccessible(true);
            return method.invoke(controller, paramList.toArray());
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }

    /**
     * 获取参数的对象
     * @param paramMap 参数Map
     * @param paramName 参数名称
     * @param methodParam 参数类型
     * @return 参数对象
     */
    private static Object getParamObject(Map<String, String> paramMap,
                                         String paramName,
                                         MethodParam methodParam) {
        return Optional.ofNullable(paramName)
                .map(paramMap::get)
                .filter(StringUtils::isNotBlank)
                .map(it -> {
                    try {
                        return CastUtils.convert(it, methodParam.getType(),
                                methodParam.getParamClass());
                    } catch (Throwable e) {
                        log.warn("转换参数出现异常", e);
                        return null;
                    }
                })
                .orElse(null);
    }
}
