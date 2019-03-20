package com.lee.http.utils;

import com.lee.iocaop.annotation.Configuration;
import com.lee.iocaop.core.IocAppContext;

import java.util.Optional;

/**
 * @author lichujun
 * @date 2019/3/18 19:25
 */
public class ConfigurationUtils {

    private static final IocAppContext CONTEXT = IocAppContext.getInstance();

    @SuppressWarnings("unchecked")
    public static <T> T getConf(String name) {
        return (T) CONTEXT.getBean(name);
    }

    public static <T> T getConf(Class<T> tClass) {
        return Optional.ofNullable(tClass)
                .map(it -> it.getDeclaredAnnotation(Configuration.class))
                .map(Configuration::value)
                .map(CONTEXT::getBean)
                .map(tClass::cast)
                .orElse(null);
    }
}
