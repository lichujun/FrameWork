package com.lee.ioc.core;

import com.lee.ioc.annotation.Component;
import com.lee.ioc.bean.BeanDefinition;
import com.lee.common.utils.ioc.BeanUtils;
import com.lee.common.utils.ioc.ClassUtils;
import com.lee.common.utils.exception.ExceptionUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bean管理类
 * @author lichujun
 * @date 2018/12/8 11:41
 */
public class BeanFactoryImpl implements BeanFactory {

    /** 存放对象的容器 */
    private static Map<String, Object> BEAN_MAP = new ConcurrentHashMap<>();
    /** 存放对象数据结构的映射的容器 */
    private static Map<String, BeanDefinition> BEAN_DEFINITION_MAP = new ConcurrentHashMap<>();

    @Override
    public Object getBean(String name) {
        return Optional.ofNullable(BEAN_MAP.get(name))
                .orElseGet(() -> Optional.ofNullable(createBean(BEAN_DEFINITION_MAP.get(name)))
                        .map(it ->{
                            // 把对象存入Map中
                            BEAN_MAP.put(name, it);
                            return it;
                        }).orElse(null)
                );
    }

    @Override
    public <T> T getBean(Class<T> tClass) {
        return Optional.ofNullable(tClass)
            .map(it ->
                // 获取当前类注入的值
                Optional.ofNullable(it.getDeclaredAnnotation(Component.class))
                    .map(Component::value)
                    // 获取注入实例的名称
                    .map(value -> StringUtils.isNotBlank(value) ? value
                            : StringUtils.uncapitalize(it.getSimpleName()))
                    // 获取实例
                    .map(this::getBean)
                    .map(it::cast)
                    .orElse(null))
            .orElse(null);
    }

    /** 注册对象 */
    void registerBean(BeanDefinition beanDefinition) {
        Optional.ofNullable(beanDefinition)
                .filter(it -> StringUtils.isNotBlank(it.getName()))
                .filter(it -> StringUtils.isNotBlank(it.getClassName()))
                .ifPresent(it -> BEAN_DEFINITION_MAP.put(it.getName(), it));
    }

    /** 实例化对象 */
    private Object createBean(BeanDefinition beanDefinition) {
        return Optional.ofNullable(beanDefinition)
            // 获取需要创建的实体的类名
            .map(BeanDefinition::getClassName)
            // 通过反射获取需要创建的实体的Class对象
            .map(ExceptionUtils.handlerFunction(ClassUtils::loadClass))
            // 如果有构造函数，就反射获取构造函数创建实例，如果不是就通过Class对象创建实例
            .map(it -> Optional.ofNullable(beanDefinition.getConstructorArgs())
                // 过滤构造函数参数为空的构造函数
                .filter(CollectionUtils::isNotEmpty)
                // 通过获取构造函数、参数实例化对象
                .map(ExceptionUtils.handlerFunction(args -> {
                    List<Object> objects = new ArrayList<>();
                    List<Class<?>> classList = new ArrayList<>();
                    // 将参数类型和参数放入到list集合中，方便转换成数组结构
                    args.forEach(ExceptionUtils.handlerConsumer(arg -> {
                        objects.add(getBean(arg.getRef()));
                        classList.add(Class.forName(arg.getClassName()));
                    }));
                    // 构造函数实例化对象
                    return BeanUtils.instance(it, it.getConstructor(
                            classList.toArray(new Class<?>[0])),
                            objects.toArray());
                }))
                // 通过Class对象实例化对象
                .orElseGet(() -> BeanUtils.instance(it, null, null))
            ).orElse(null);
    }
}
