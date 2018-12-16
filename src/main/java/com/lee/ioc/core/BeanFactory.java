package com.lee.ioc.core;

import com.lee.ioc.bean.BeanDefinition;
import java.util.Set;

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

    /**
     * 注册bean
     * @param beanDefinition bean的信息
     */
    void registerBean(BeanDefinition beanDefinition);

    /**
     * 获取接口的所有实现
     * @param interfaceName 接口名称
     * @return 接口实现的bean名称
     */
    Set<String> getInterfaceImpl(String interfaceName);

    /**
     * 注册接口的所有实现
     * @param beanName bean名称
     * @param interfaces 实现的接口
     */
    void registerInterfaceImpl(String beanName, Set<String> interfaces);
}
