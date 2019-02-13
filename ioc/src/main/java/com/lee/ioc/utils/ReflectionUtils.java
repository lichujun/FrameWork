package com.lee.ioc.utils;

import com.lee.common.utils.exception.ExceptionUtils;

import java.lang.reflect.Field;
import java.util.Optional;

/**
 * 通过反射完成对象依赖注入
 * @author lichujun
 * @date 2018/12/8 11:36
 */
public class ReflectionUtils {

    /** 将obj对象的field的值改为value */
    public static void injectField(Field field, Object obj, Object value) {
        Optional.ofNullable(field)
                .ifPresent(ExceptionUtils.handleConsumer(it -> {
                    // 设置为可以修改private的field变量
                    it.setAccessible(true);
                    it.set(obj, value);
                }));
    }
}
