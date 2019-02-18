package com.lee.iocaop.utils;

import com.lee.iocaop.proxy.ProxyInterceptor;
import net.sf.cglib.proxy.Enhancer;
import java.lang.reflect.Constructor;

/**
 * cglib动态代理生成bean
 * @author lichujun
 * @date 2019/2/16 2:43 PM
 */
class CglibProxyUtils {

    /**
     * 通过bean的类对象、构造函数和参数生成bean
     * @param beanClass 类对象
     * @param ctr 构造函数
     * @param args 参数
     * @param <T> bean的类型
     * @return bean
     */
    static <T> T newInstance(Class<T> beanClass, Constructor ctr, Object[] args) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(beanClass);
        enhancer.setCallback(new ProxyInterceptor());
        if(ctr == null){
            return beanClass.cast(enhancer.create());
        }
        return beanClass.cast(enhancer.create(ctr.getParameterTypes(), args));
    }

    /**
     * 默认构造函数生成bean
     * @param beanClass 类对象
     * @param <T> bean的类型
     * @return bean
     */
    static <T> T newInstance(Class<T> beanClass) {
        return newInstance(beanClass, null, null);
    }
}
