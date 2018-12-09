package com.lee.ioc.core;

/**
 * @author lichujun
 * @date 2018/12/8 11:39
 */
public interface BeanFactory {
    /**
     * 注解形式通过类名称获取实例
     * @param name 类名称
     * @return 对象实例
     */
    Object getBean(String name);

    /**
     * 注解形式通过类对象获取实例
     * @param tClass 类对象
     * @return 对象实例
     */
    <T> T getBean(Class<T> tClass);
}
