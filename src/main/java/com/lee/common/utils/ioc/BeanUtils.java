package com.lee.common.utils.ioc;

import com.lee.common.utils.exception.ExceptionUtils;

import java.lang.reflect.Constructor;
import java.util.Optional;

/**
 * 处理对象的实例化
 * @author lichujun
 * @date 2018/12/8 11:27
 */
public class BeanUtils {

    /** 通过反射实例化对象 */
    public static Object instance(Class<?> tClass, Constructor ctr, Object[] args) {
        return Optional.ofNullable(tClass)
                .map(it -> Optional.ofNullable(ctr)
                        .map(ExceptionUtils.handlerFunction(constructor ->
                                constructor.newInstance(args)))
                        .orElseGet(ExceptionUtils.handleSupplier(it::newInstance)))
                .orElse(null);
    }

}
