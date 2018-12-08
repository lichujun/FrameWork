package com.lee.ioc.utils;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.NoOp;

import java.lang.reflect.Constructor;
import java.util.Optional;

/**
 * 处理对象的实例化
 * @author lichujun
 * @date 2018/12/8 11:27
 */
public class BeanUtils {

    /** 通过cglib实例化对象 */
    public static <T> T instanceByCglib(Class<T> tClass, Constructor ctr, Object[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(tClass);
        enhancer.setCallback(NoOp.INSTANCE);
        return Optional.ofNullable(ctr)
                .map(it -> tClass.cast(enhancer.create()))
                .orElse(tClass.cast(enhancer.create(ctr.getParameterTypes(),args)));
    }
}
