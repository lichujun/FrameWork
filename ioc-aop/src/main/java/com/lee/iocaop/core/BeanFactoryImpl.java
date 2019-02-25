package com.lee.iocaop.core;

import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.iocaop.bean.AspectMethod;
import com.lee.iocaop.bean.ConstructorArg;
import com.lee.iocaop.bean.enums.ComponentEnums;
import com.lee.iocaop.utils.BeanUtils;
import com.lee.iocaop.bean.BeanDefinition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bean管理类
 * @author lichujun
 * @date 2018/12/8 11:41
 */
@Slf4j
public class BeanFactoryImpl implements BeanFactory {

    /** 存放所有扫描到的类 */
    private static Set<Class<?>> classSet = null;
    /** 存放对象数据结构的映射的容器 */
    private static Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();
    /** 存放接口的实现对应关系 */
    private static Map<String, Set<String>> interfaceMap = new HashMap<>();
    /** 存放对象的容器 */
    private static final Map<String, Object> BEAN_MAP = new HashMap<>();
    /** 存放处理Exception的集合 */
    private static final Map<Class<?>, Method> EXCEPTION_MAP = new HashMap<>();
    /** AOP-BEFORE关系集合 */
    private static final Map<Method, List<AspectMethod>> BEFORE_AOP_MAP = new HashMap<>();
    /** AOP-AFTER关系集合 */
    private static final Map<Method, List<AspectMethod>> AFTER_AOP_MAP = new HashMap<>();
    /** bean组件注入获取注入值的方法名 */
    private static final String BEAN_METHOD = "value";

    /**
     * 通过bean名称获取对象
     * @param name bean的名称
     * @return bean名称对应的对象
     */
    @Override
    public Object getBean(String name) {
        Object bean = BEAN_MAP.get(name);
        if (bean != null) {
            return bean;
        }
        return Optional.ofNullable(beanDefinitionMap)
                // 获取bean的注册信息
                .map(it -> it.get(name))
                // 创建bean
                .map(this::createBean)
                .map(it ->{
                    // 把对象存入Map中
                    putBean(name, it);
                    return it;
                }).orElse(null);
    }

    /**
     * 通过类对象获取对象
     * @param tClass 类对象
     * @return 类对象对应的对象
     */
    @Override
    public <T> T getBean(Class<T> tClass) {
        if (tClass == null) {
            return null;
        }
        if (tClass.isInterface()) {
            throw new RuntimeException(tClass + "是接口，不能实例化");
        }
        return Optional.of(tClass)
                .filter(local -> !local.isAnnotation()
                        && existInject(local))
                // 获取注解注入的bean名称
                .map(this::getInjectBeanName)
                // 通过bean名称获取
                .map(this::getBean)
                // Object类型转T类型
                .map(tClass::cast)
                .orElse(null);
    }

    /**
     * 获取接口的所有实现
     * @param interfaceName 接口名称
     * @return 接口实现的bean名称
     */
    @Override
    public Set<String> getInterfaceImpl(String interfaceName) {
        return Optional.ofNullable(interfaceName)
                .map(it -> interfaceMap.get(interfaceName))
                .orElse(null);
    }

    /**
     * 注册bean的信息到容器里，以便之后实例化
     * @param beanDefinition bean的注册信息
     */
    @Override
    public void registerBean(BeanDefinition beanDefinition) {
        // 判断注册的bean信息是否有效
        if (beanDefinition == null
                || StringUtils.isBlank(beanDefinition.getName())
                || StringUtils.isBlank(beanDefinition.getClassName())) {
            return;
        }
        // 将bean名称和与之对应的对象存放到容器中，并校验是否有重复的bean名称
        if (beanDefinitionMap.put(beanDefinition.getName(), beanDefinition) != null) {
            throw new RuntimeException(String.format("存在多个相同的bean名称：%s",
                    beanDefinition.getName()));
        }
    }

    /**
     * 将接口和它所有实现的关系注册到容器里
     * @param beanName bean名称
     * @param interfaces 实现的接口
     */
    @Override
    public void registerInterfaceImpl(String beanName, Set<String> interfaces) {
        if (CollectionUtils.isEmpty(interfaces) || StringUtils.isBlank(beanName)) {
            return;
        }
        for (String imp : interfaces) {
            if (StringUtils.isBlank(imp)) {
                continue;
            }
            Set<String> impSet = interfaceMap.get(imp);
            if (CollectionUtils.isNotEmpty(impSet)) {
                Optional.of(impSet)
                        .filter(set -> set.add(beanName))
                        .orElseGet(() -> {
                            throw new RuntimeException(String.format(
                                    "存在多个相同的bean名称：%s", beanName));
                        });
            } else {
                if (impSet == null) {
                    impSet = new HashSet<>();
                }
                impSet.add(beanName);
                interfaceMap.put(imp, impSet);
            }
        }
    }

    /**
     * 通过bean的注册信息来实例化对象
     * @param beanDefinition bean的注册信息
     * @return 通过bean的注册信息实例化的对象
     */
    private Object createBean(BeanDefinition beanDefinition) {
        Class<?> tClass = Optional.ofNullable(beanDefinition)
                // 获取需要创建的实体的类名
                .map(BeanDefinition::getClassName)
                // 通过反射获取需要创建的实体的Class对象
                .map(ExceptionUtils.handleFunction(Class::forName))
                .orElse(null);
        if (tClass == null) {
            return null;
        }
        List<ConstructorArg> constructorArgs = beanDefinition.getConstructorArgs();
        if (CollectionUtils.isEmpty(constructorArgs)) {
            // 无参构造函数实例化对象
            return BeanUtils.instance(tClass);
        }
        List<Object> objects = new ArrayList<>();
        List<Class<?>> classList = new ArrayList<>();
        // 将参数类型和参数放入到list集合中，方便转换成数组结构
        constructorArgs.forEach(ExceptionUtils.handleConsumer(arg ->
                Optional.ofNullable(getBean(arg.getRef()))
                        .map(ExceptionUtils.handleFunction(bean -> {
                            objects.add(bean);
                            classList.add(Class.forName(arg.getClassName()));
                            return bean;
                        })).orElseGet(() -> {
                            throw new RuntimeException(String.format(
                                    "不存在名称为%s的bean", arg.getRef()));
                })
        ));
        Constructor constructor;
        try {
            constructor = tClass.getDeclaredConstructor(classList.toArray(new Class[0]));
        } catch (Throwable e) {
            log.error("{}不存在注册的bean信息里的构造函数", tClass);
            throw new RuntimeException(e);
        }
        // 构造函数实例化对象
        return BeanUtils.instance(tClass, constructor, objects.toArray());
    }

    /**
     * 判断类是否存在需要进行依赖注入的注解
     * @param tClass 类对象
     * @return 是否需要做依赖注入的操作
     */
    boolean existInject(Class<?> tClass) {
        for (ComponentEnums component : ComponentEnums.values()) {
            if (tClass.getDeclaredAnnotation(component.getComponent()) != null) {
                return true;
            }
        }
        return false;
    }

    /** 获取@Component等注入bean的名称 */
    String getInjectBeanName(Class<?> tClass) {
        String annotationValue = null;
        for (ComponentEnums component : ComponentEnums.values()) {
            Annotation annotation = tClass.getDeclaredAnnotation(component.getComponent());
            if (annotation != null) {
                try {
                    // 获取bean组件注入值的方法
                    Method method = component.getComponent().getDeclaredMethod(BEAN_METHOD);
                    method.setAccessible(true);
                    // 获取注入的值
                    Object beanValueObj = method.invoke(annotation);
                    annotationValue = beanValueObj == null ? null : beanValueObj.toString();
                } catch (Exception e) {
                    log.warn("@Component获取注入的值时发生异常", e);
                    continue;
                }
                if (annotationValue != null) {
                    break;
                }
            }
        }
        // 将注解值为空时将bean的名称设置为首字母小写的简单类名
        return Optional.ofNullable(annotationValue)
                .map(value -> StringUtils.isNotBlank(value) ? value :
                        StringUtils.uncapitalize(tClass.getSimpleName()))
                .orElseGet(() -> {
                    throw new RuntimeException(tClass + "注入组件未标注注解");
                });
    }

    /**
     * 将扫描出的所有Class对象注册到容器中
     * @param classSet Class对象集合
     */
    void setClassSet(Set<Class<?>> classSet) {
        BeanFactoryImpl.classSet = classSet;
    }

    /**
     * 通过注解获取被此注解标记的类集合
     * @param annotation 注解
     * @return 类集合
     */
    public Set<Class<?>> getClassesByAnnotation(Class<? extends Annotation> annotation) {
        return Optional.ofNullable(classSet)
                .filter(CollectionUtils::isNotEmpty)
                .map(set -> set.stream()
                        .filter(it -> it.isAnnotationPresent(annotation))
                        .collect(Collectors.toSet())
                ).orElse(null);
    }

    /**
     * 将配置文件的bean名称和实例对象注册到bean容器中
     * @param tClass 配置文件的类对象
     * @param t 配置文件对象
     */
    void injectConfiguration(Class<?> tClass, Object t) {
        if (tClass == null || t == null) {
            return ;
        }
        String classSimpleName = StringUtils.uncapitalize(tClass.getSimpleName());
        if (BEAN_MAP.put(classSimpleName, t) != null) {
            throw new RuntimeException(tClass + "配置文件的类名存在相同的，注入配置文件失败");
        }
    }

    /**
     * 获取配置文件对象为空，将配置文件的bean名称和默认初始化对象注册到bean容器中
     * @param tClass 配置文件的类对象
     */
    void injectConfiguration(Class<?> tClass) {
        if (tClass == null) {
            return ;
        }
        Object obj;
        try {
            obj = tClass.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(tClass + "注入配置文件失败");
        }
        String classSimpleName = StringUtils.uncapitalize(tClass.getSimpleName());
        if (BEAN_MAP.put(classSimpleName, obj) != null) {
            throw new RuntimeException(tClass + "配置文件的bean名称存在相同的，注入配置文件失败");
        }
    }

    /**
     * 添加统一异常处理的Exception类
     * @param tClass 异常类
     * @param method 异常处理的方法
     */
    void putExceptionHandler(Class<?> tClass, Method method) {
        if (tClass == null || method == null) {
            return;
        }
        if (EXCEPTION_MAP.put(tClass, method) != null) {
            throw new RuntimeException(tClass + "异常捕获存在多个处理方法");
        }
    }

    /**
     * 判断对象是否是异常类生成的，并获取对应的method
     * @param e 异常
     * @return 统一异常处理中此异常执行哪个方法
     */
    public Method getProcessExceptionMethod(Throwable e) {
        if (e == null) {
            return null;
        }
        return Optional.of(EXCEPTION_MAP)
                .map(Map::entrySet)
                .filter(CollectionUtils::isNotEmpty)
                .map(it -> it.stream()
                        .filter(local -> e.getClass().equals(local.getKey()))
                        .findFirst().orElse(null)
                )
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    /**
     * 将bean名称和与之对应的对象之间的关系注册到bean容器中
     * @param beanName bean名称
     * @param bean bean名称所对应的对象
     */
    void putBean(String beanName, Object bean) {
        if (beanName == null || bean == null) {
            return;
        }
        BEAN_MAP.put(beanName, bean);
    }

    /**
     * 注册AOP-BEFORE关系
     * @param method 实体类的方法
     * @param aspectMethod @Aspect注解标注下的类的bean对象和方法
     */
    void registerBeforeAOP(Method method, AspectMethod aspectMethod) {
        registerAOP(method, aspectMethod, BEFORE_AOP_MAP);
    }

    /**
     * 注册AOP-AFTER关系
     * @param method 实体类的方法
     * @param aspectMethod @Aspect注解标注下的类的bean对象和方法
     */
    void registerAfterAOP(Method method, AspectMethod aspectMethod) {
        registerAOP(method, aspectMethod, AFTER_AOP_MAP);
    }

    /**
     * 注册AOP关系
     * @param method 实体类的方法
     * @param aspectMethod @Aspect注解标注下的类的bean对象和方法
     * @param map 存放AOP关系的集合
     */
    private void registerAOP(Method method, AspectMethod aspectMethod, Map<Method, List<AspectMethod>> map) {
        List<AspectMethod> list = map.get(method);
        if (list == null) {
            List<AspectMethod> aspectMethodList = new ArrayList<>();
            aspectMethodList.add(aspectMethod);
            map.put(method, aspectMethodList);
        } else {
            list.add(aspectMethod);
            map.put(method, list);
        }
    }

    /**
     * 获取方法的AOP-BEFORE的关系
     * @param method 方法
     * @return 该方法的AOP-BEFORE的关系
     */
    public List<AspectMethod> getBeforeAOP(Method method) {
        return BEFORE_AOP_MAP.get(method);
    }

    /**
     * 获取方法的AOP-AFTER的关系
     * @param method 方法
     * @return 该方法的AOP-AFTER的关系
     */
    public List<AspectMethod> getAfterAOP(Method method) {
        return AFTER_AOP_MAP.get(method);
    }

    public void releaseResource() {
        classSet = null;
        beanDefinitionMap = null;
        interfaceMap = null;
    }

}
