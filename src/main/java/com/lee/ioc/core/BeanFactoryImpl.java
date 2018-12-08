package com.lee.ioc.core;

import com.lee.ioc.bean.BeanDefinition;
import com.lee.ioc.utils.BeanUtils;
import com.lee.ioc.utils.ClassUtils;
import com.lee.ioc.utils.ReflectionUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lichujun
 * @date 2018/12/8 11:41
 */
public class BeanFactoryImpl implements BeanFactory {

    /** 存放对象的容器 */
    private static final Map<String, Object> BEAN_MAP = new ConcurrentHashMap<>();
    /** 存放对象数据结构的映射的容器 */
    private static final Map<String, BeanDefinition> BEAN_DEFINITION_MAP = new ConcurrentHashMap<>();

    @Override
    public Object getBean(String name) {
        return Optional.ofNullable(BEAN_MAP.get(name))
                .orElseGet(() -> Optional.ofNullable(createBean(BEAN_DEFINITION_MAP.get(name)))
                        .map(it ->{
                            // 对象创建成功后，注入对象所需要的参数
                            populateBean(it);
                            // 再把对象存入Map中
                            BEAN_MAP.put(name, it);
                            return it;
                        }).orElse(null)
                );
    }

    /** 注册对象 */
    void registerBean(BeanDefinition beanDefinition) {
        BEAN_DEFINITION_MAP.put(beanDefinition.getName(), beanDefinition);
    }

    /** 创建对象 */
    private Object createBean(BeanDefinition beanDefinition) {
        return Optional.ofNullable(beanDefinition)
                .map(BeanDefinition::getClassName)
                .map(ClassUtils::loadClass)
                .map(it ->
                        Optional.ofNullable(beanDefinition.getConstructorArgs())
                                .filter(CollectionUtils::isNotEmpty)
                                .map(args -> {
                                    List<Object> objects = new ArrayList<>();
                                    args.forEach(arg -> objects.add(arg.getRef()));
                                    try {
                                        return BeanUtils.instanceByCglib(it, it.getConstructor(),
                                                objects.toArray());
                                    } catch (NoSuchMethodException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                }).orElse(BeanUtils.instanceByCglib(it, null, null))
                ).orElse(null);
    }

    /** 注入Field返回的对象 */
    private void populateBean(Object bean) {
        Optional.ofNullable(bean)
                .map(Object::getClass)
                .map(Class::getSuperclass)
                .map(Class::getDeclaredFields)
                .filter(ArrayUtils::isNotEmpty)
                .ifPresent(fields -> {
                    for (Field field : fields) {
                        Optional.ofNullable(field)
                                .map(Field::getName)
                                .map(StringUtils::uncapitalize)
                                .filter(BEAN_DEFINITION_MAP.keySet()::contains)
                                .map(this::getBean)
                                .ifPresent(it -> ReflectionUtils.injectField(field, bean, it));
                    }
                });
    }
}
