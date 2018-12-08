package com.lee.ioc.core;

import com.lee.ioc.bean.BeanDefinition;
import com.lee.ioc.bean.ConstructorArg;
import com.lee.common.utils.ioc.BeanUtils;
import com.lee.common.utils.ioc.ClassUtils;
import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.common.utils.ioc.ReflectionUtils;
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

    /** 实例化对象 */
    private Object createBean(BeanDefinition beanDefinition) {
        return Optional.ofNullable(beanDefinition)
            .map(BeanDefinition::getClassName)
            .map(ExceptionUtils.handlerFunction(ClassUtils::loadClass))
            .map(it ->
                Optional.ofNullable(beanDefinition.getConstructorArgs())
                    .filter(CollectionUtils::isNotEmpty)
                    .map(ExceptionUtils.handlerFunction(args -> {
                        List<Object> objects = new ArrayList<>();
                        List<Class<?>> classList = new ArrayList<>();
                        args.stream().sorted(Comparator.comparing(ConstructorArg::getIndex))
                                .forEach(ExceptionUtils.handlerConsumer(arg -> {
                                    objects.add(getBean(arg.getRef()));
                                    classList.add(Class.forName(arg.getClassName()));
                                }));
                        return BeanUtils.instance(it, it.getConstructor(
                                classList.toArray(new Class<?>[0])),
                                objects.toArray());
                    })).orElse(BeanUtils.instance(it, null, null))
            ).orElse(null);
    }

    /** 注入Field返回的对象 */
    private void populateBean(Object bean) {
        Optional.ofNullable(bean)
            .map(Object::getClass)
            .map(Class::getDeclaredFields)
            .filter(ArrayUtils::isNotEmpty)
            .ifPresent(fields -> {
                for (Field field : fields) {
                    Optional.ofNullable(field)
                        .map(Field::getName)
                        .map(StringUtils::uncapitalize)
                        .filter(BEAN_DEFINITION_MAP.keySet()::contains)
                        .map(this::getBean)
                        .ifPresent(it ->
                                ReflectionUtils.injectField(field, bean, it));
                }
            });
    }
}
