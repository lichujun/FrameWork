package com.lee.iocaop.core;

import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.iocaop.annotation.*;
import com.lee.iocaop.bean.AspectMethod;
import com.lee.iocaop.utils.BeanUtils;
import com.lee.iocaop.bean.BeanDefinition;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bean管理类
 * @author lichujun
 * @date 2018/12/8 11:41
 */
public class BeanFactoryImpl implements BeanFactory {

    /** 存放所有扫描到的类 */
    private static Set<Class<?>> CLASS_SET = null;
    /** 存放对象的容器 */
    private static Map<String, Object> BEAN_MAP = new HashMap<>();
    /** 存放对象数据结构的映射的容器 */
    private static Map<String, BeanDefinition> BEAN_DEFINITION_MAP = new HashMap<>();
    /** 存放接口的实现对应关系 */
    private static Map<String, Set<String>> INTERFACE_MAP = new HashMap<>();
    /** 存放处理Exception的集合 */
    private static Map<Class<?>, Method> EXCEPTION_MAP = new HashMap<>();
    /** AOP-BEFORE关系集合 */
    private static Map<Method, List<AspectMethod>> BEFORE_AOP_MAP = new HashMap<>();
    /** AOP-AFTER关系集合 */
    private static Map<Method, List<AspectMethod>> AFTER_AOP_MAP = new HashMap<>();

    @Override
    public Object getBean(String name) {
        return Optional.ofNullable(BEAN_MAP.get(name))
                .orElseGet(() ->
                        Optional.ofNullable(BEAN_DEFINITION_MAP)
                            .map(it -> it.get(name))
                            .map(this::createBean)
                            .map(it ->{
                                // 把对象存入Map中
                                putBean(name, it);
                                return it;
                            }).orElse(null)
                );
    }

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
                        && existInject(local)
                )
                .map(this::getValue)
                .map(this::getBean)
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
                .map(it -> INTERFACE_MAP.get(interfaceName))
                .orElse(null);
    }

    /** 注册对象 */
    @Override
    public void registerBean(BeanDefinition beanDefinition) {
        if (beanDefinition == null) {
            return;
        }
        Optional.of(beanDefinition)
            // 过滤没有bean名称或类名的BeanDefinition
            .filter(it ->
                    StringUtils.isNotBlank(it.getName())
                    && StringUtils.isNotBlank(it.getClassName())
                    && BEAN_DEFINITION_MAP.put(it.getName(), it) == null
            )
            .orElseGet(() -> {
                throw new RuntimeException(String.format("存在多个相同的bean名称：%s",
                        beanDefinition.getName()));
            });
    }

    /**
     * 注册接口的所有实现
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
            Set<String> impSet = INTERFACE_MAP.get(imp);
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
                // 存放到接口实现容器
                INTERFACE_MAP.put(imp, impSet);
            }
        }
    }

    /** 实例化对象 */
    private Object createBean(BeanDefinition beanDefinition) {
        return Optional.ofNullable(beanDefinition)
            // 获取需要创建的实体的类名
            .map(BeanDefinition::getClassName)
            // 通过反射获取需要创建的实体的Class对象
            .map(ExceptionUtils.handleFunction(Class::forName))
            // 如果有构造函数，就反射获取构造函数创建实例，如果不是就通过Class对象创建实例
            .map(it -> Optional.of(beanDefinition)
                .map(BeanDefinition::getConstructorArgs)
                // 过滤构造函数参数为空的构造函数
                .filter(CollectionUtils::isNotEmpty)
                // 通过获取构造函数、参数实例化对象
                .map(ExceptionUtils.handleFunction(args -> {
                    List<Object> objects = new ArrayList<>();
                    List<Class<?>> classList = new ArrayList<>();
                    // 将参数类型和参数放入到list集合中，方便转换成数组结构
                    args.forEach(ExceptionUtils.handleConsumer(arg ->
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
                    // 构造函数实例化对象
                    return BeanUtils.instance(it, it.getConstructor(
                            classList.toArray(new Class<?>[0])),
                            objects.toArray());
                }))
                // 通过Class对象实例化对象
                .orElseGet(() -> BeanUtils.instance(it, null, null))
            ).orElse(null);
    }

    /** 判断是否存在要注入的注解 */
    boolean existInject(Class<?> tClass) {
        // 是否存在Component注解
        return Optional.ofNullable(tClass.getDeclaredAnnotation(Component.class))
            .map(it -> true)
            // 是否存在Controller注解
            .orElseGet(() -> Optional.ofNullable(tClass.getDeclaredAnnotation(Controller.class))
                .map(it -> true)
                // 是否存在Service注解
                .orElseGet(() -> Optional.ofNullable(tClass.getDeclaredAnnotation(Service.class))
                    .map(it -> true)
                    // 是否存在Repository注解
                    .orElseGet(() -> tClass.getDeclaredAnnotation(Repository.class) != null)
                )
            );
    }

    /** 获取@Component等注入bean的名称 */
    String getValue(Class<?> tClass) {
        // 获取注解注入的值
        String annotationValue =  Optional.ofNullable(tClass.getDeclaredAnnotation(Component.class))
            // 获取Component注解注入的值
            .map(Component::value)
            .orElseGet(() -> Optional.ofNullable(tClass.getDeclaredAnnotation(Controller.class))
                // 获取Controller注解注入的值
                .map(Controller::value)
                .orElseGet(() -> Optional.ofNullable(tClass.getDeclaredAnnotation(Service.class))
                    // 获取Service注解注入的值
                    .map(Service::value)
                    .orElseGet(() -> Optional.ofNullable(tClass.getDeclaredAnnotation(Repository.class))
                        // 获取Repository注解注入的值
                        .map(Repository::value)
                        .orElseGet(() -> Optional.ofNullable(tClass.getDeclaredAnnotation(Configuration.class))
                        .map(it -> StringUtils.uncapitalize(tClass.getSimpleName()))
                        .orElse(null)
                        )
                    )
                ));
        // 将注解值为空时将bean的名称设置为首字母小写的简单类名
        return Optional.ofNullable(annotationValue)
                .map(value -> StringUtils.isNotBlank(value) ? value :
                        StringUtils.uncapitalize(tClass.getSimpleName()))
                .orElseGet(() -> {
                    throw new RuntimeException(tClass + "注入组件未标注注解");
                });
    }

    /**
     * 将扫描出的所有Class对象存在容器
     * @param classSet Class对象集合
     */
    void setClassSet(Set<Class<?>> classSet) {
        CLASS_SET = classSet;
    }

    /**
     * 通过注解获取类集合
     * @param annotation 注解Class对象
     * @return 类集合
     */
    public Set<Class<?>> getClassesByAnnotation(Class<? extends Annotation> annotation) {
        return Optional.ofNullable(CLASS_SET)
                .filter(CollectionUtils::isNotEmpty)
                .map(set -> set.stream()
                        .filter(it -> it.isAnnotationPresent(annotation))
                        .collect(Collectors.toSet())
                ).orElse(null);
    }

    /**
     * 注入配置文件
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
     * 注入配置文件
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
            throw new RuntimeException(tClass + "配置文件的类名存在相同的，注入配置文件失败");
        }
    }

    /** 添加Exception */
    void putExceptionHandler(Class<?> tClass, Method method) {
        if (tClass == null || method == null) {
            return;
        }
        if (EXCEPTION_MAP.put(tClass, method) != null) {
            throw new RuntimeException(tClass + "存在多个处理方法");
        }
    }

    /** 判断对象是否是异常类生成的，并获取对应的method */
    public Method getMethod(Throwable e) {
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

    /** 注入bean对象 */
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
        CLASS_SET = null;
        BEAN_DEFINITION_MAP = null;
        INTERFACE_MAP = null;
    }

}
