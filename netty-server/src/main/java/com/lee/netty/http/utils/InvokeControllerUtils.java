package com.lee.netty.http.utils;

import com.lee.ioc.utils.CastUtils;
import com.lee.ioc.core.IocAppContext;
import com.lee.netty.http.bean.ControllerInfo;
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
    public static Object invokeController(ControllerInfo controllerInfo) {
        Object controller = IocAppContext.getInstance()
                .getBean(controllerInfo.getControllerClass());
        Method method = controllerInfo.getInvokeMethod();
        method.setAccessible(true);
        try {
            return method.invoke(controller);
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.error("反射调用Controller类的方法发生异常", e);
            throw new RuntimeException("反射调用Controller类的方法发生异常");
        }
    }

    /**
     * 调用controller的方法
     * @param controllerInfo controller的信息
     * @param paramMap 参数Map
     * @return 返回报文
     */
    public static Object invokeController(ControllerInfo controllerInfo,
                                          Map<String, String> paramMap,
                                          String reqJson) {
        Object controller = IocAppContext.getInstance()
                .getBean(controllerInfo.getControllerClass());
        Method method = controllerInfo.getInvokeMethod();
        method.setAccessible(true);
        Map<String, Class<?>> paramClassMap = controllerInfo.getMethodParameter();
        if (MapUtils.isEmpty(paramClassMap)) {
            return invokeController(controllerInfo);
        } else {
            List<Object> paramList = new ArrayList<>();
            // 如果只有一个参数，将paramMap的参数反序列化成object对象
            // 若object对象为null，则将raw body的数据反序列化成object对象
            if (paramClassMap.size() == 1) {
                paramClassMap.forEach((paramName, paramClass) -> {
                    Object param = getParamObject(paramMap, paramName, paramClass);
                    if (param == null) {
                        try {
                            param = Optional.ofNullable(reqJson)
                                .filter(StringUtils::isNotBlank)
                                .map(it -> CastUtils.convert(it, paramClass))
                                .orElse(null);
                        } catch (Throwable e) {
                            log.warn("参数反序列化出现异常", e);
                        }
                    }
                    paramList.add(param);
                });
            } else {
                paramClassMap.forEach((paramName, paramClass) -> {
                    Object param = getParamObject(paramMap, paramName, paramClass);
                    paramList.add(param);
                });
            }
            try {
                return method.invoke(controller, paramList.toArray());
            } catch (Throwable e) {
                log.warn("controller层反射出现异常", e);
                return null;
            }
        }
    }

    /**
     * 获取参数的对象
     * @param paramMap 参数Map
     * @param paramName 参数名称
     * @param paramClass 参数类名
     * @return 参数对象
     */
    private static Object getParamObject(Map<String, String> paramMap,
                                         String paramName,
                                         Class<?> paramClass) {
        return Optional.ofNullable(paramName)
                .map(paramMap::get)
                .filter(StringUtils::isNotBlank)
                .map(it -> {
                    try {
                        return CastUtils.convert(it, paramClass);
                    } catch (Throwable e) {
                        log.warn("转换参数出现异常", e);
                        return null;
                    }
                })
                .orElse(null);
    }
}
