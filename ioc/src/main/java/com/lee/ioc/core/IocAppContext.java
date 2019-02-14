package com.lee.ioc.core;

import com.alibaba.fastjson.JSONObject;
import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.ioc.annotation.Configuration;
import com.lee.ioc.utils.ReflectionUtils;
import com.lee.ioc.utils.ScanUtils;
import com.lee.ioc.annotation.Resource;
import com.lee.ioc.bean.BeanDefinition;
import com.lee.ioc.bean.ConstructorArg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 注解依赖注入
 * @author lichujun
 * @date 2018/12/9 10:46 AM
 */
@Slf4j
public class IocAppContext extends BeanFactoryImpl {

    private enum IocAppHolder {
        // 单例
        INSTANCE;

        private IocAppContext context;

        IocAppHolder() {
            context = new IocAppContext();
        }
    }

    public static IocAppContext getInstance() {
        return IocAppHolder.INSTANCE.context;
    }

    /**
     * 初始化自定义注解的依赖注入
     * 加载yaml配置文件，获取包名
     */
    public JSONObject init(String path, String scanPackage, Class<?> tClass) {
        if (StringUtils.isBlank(path) || tClass == null) {
            return null;
        }
        InputStream yamlStream = tClass.getClassLoader().getResourceAsStream(path);
        if (yamlStream == null) {
            return null;
        }
        log.info("正在加载Bean，进行依赖注入...");
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(yamlStream);
        JSONObject yamlJson = new JSONObject(yamlMap);
        Optional.of(yamlJson)
                .map(it -> it.getJSONArray(scanPackage))
                .map(arr -> arr.toJavaList(String.class))
                .map(HashSet::new)
                .ifPresent(it -> scanPackages(it, yamlJson));
        return yamlJson;
    }

    /** 扫包，进行依赖注入 */
    private void scanPackages(Set<String> packages, JSONObject yamlJson) {
        log.info("扫描的包名为：" + packages);
        Optional.ofNullable(packages)
                .filter(CollectionUtils::isNotEmpty)
                // 扫描包，将Class对象存放在Set集合
                .map(ExceptionUtils.handleFunction(ScanUtils::getAllClassPathClasses))
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(classSet -> {
                    this.setClassSet(classSet);
                    for (Class<?> tClass : classSet) {
                        if (tClass == null) {
                            continue;
                        }
                        Optional.of(tClass)
                                .filter(it -> it.getInterfaces().length > 0)
                                .filter(local -> !local.isAnnotation())
                                // 有@Component组件的才将接口和bean的关系注册到容器中
                                .filter(this::existInject)
                                .ifPresent(it -> {
                                    // 获取注入的值，为空则为首字母小写的简单类名
                                    String beanName = getValue(tClass);
                                    // 获取该类所实现的所有接口
                                    Set<String> iSet = Stream.of(tClass.getInterfaces())
                                            .map(Class::getName).collect(Collectors.toSet());
                                    // 将接口和bean的关系注册到容器
                                    registerInterfaceImpl(beanName, iSet);
                                });
                        // 将@Component注册到容器
                        Optional.ofNullable(processComponent(tClass))
                                .ifPresent(this::registerBean);
                        // 加载配置文件
                        loadConfiguration(tClass, yamlJson);
                    }
                    // 将@Resource依赖注入到Field
                    classSet.forEach(this::processFieldResource);
                    log.info("已经将所有依赖注入到Field...");
                    // 加载所有bean
                    classSet.forEach(tClass -> Optional.ofNullable(tClass)
                            .filter(this::existInject)
                            .ifPresent(this::getBean)
                    );
                    log.info("完成所有bean对象的加载...");
                });
    }

    /** 获取@Component的BeanDefinition */
    private BeanDefinition processComponent(Class<?> tClass) {
        if (tClass == null || !existInject(tClass)) {
            return null;
        }
        Optional.of(tClass)
                // 过滤有多个构造方法并且要注入成组件的类，否则报错
                .filter(local -> local.getDeclaredConstructors().length == 1)
                .orElseGet(() -> {
                    throw new RuntimeException("实例不能存在多个构造方法");
                });
        List<ConstructorArg> constructorArgList = Optional.of(tClass)
                // 获取构造函数列表
                .map(Class::getDeclaredConstructors)
                // 获取唯一的构造函数
                .map(cons -> cons[0])
                // 获取构造函数的参数列表
                .map(Constructor::getParameters)
                .filter(ArrayUtils::isNotEmpty)
                .map(parameters -> {
                    // 获取构造函数的参数和注入的值
                    List<ConstructorArg> constructorArgs = new ArrayList<>();
                    Stream.of(parameters).forEach(parameter -> {
                        // 获取注入实例的名称，构造函数里的所有参数都需@Resource注入实例
                        Class<?> paramClass = parameter.getType();
                        String paramClassName = paramClass.getName();
                        // 获取构造函数的参数的注解
                        Resource paramResource = parameter.getDeclaredAnnotation(Resource.class);
                        Optional.of(paramClass)
                            // 判断是参数否是接口
                            .filter(Class::isInterface)
                            .map(pClass -> {
                                // 获取接口的所有实现
                                Set<String> impSet = getInterfaceImpl(paramClass.getName());
                                Optional.of(paramResource.value())
                                        // 如果接口只有一个实现或者@Resource有指定bean则注入bean，否则报错
                                    .filter(value -> impSet.size() == 1 || StringUtils.isNotBlank(value))
                                    .map(value -> {
                                        constructorArgs.add(new ConstructorArg(value, paramClassName));
                                        return value;
                                    }).orElseGet(() -> {
                                        throw new RuntimeException(pClass
                                                + "接口有多个实现，请指定bean名称");
                                    });
                                return pClass;
                            }).orElseGet(() -> {
                                // 如果参数是类，则直接通过此类的bean名称获取bean
                                String value = getValue(paramClass);
                                constructorArgs.add(new ConstructorArg(value, paramClassName));
                                return null;
                            });
                    });
                    return constructorArgs;
                })
                .orElse(null);
        // 创建BeanDefinition对象以用来注册实例
        return createBeanDefinition(getValue(tClass), tClass, constructorArgList);
    }

    /** 将@Resource依赖注入到Field */
    private void processFieldResource(Class<?> tClass) {
        if (tClass == null || !existInject(tClass)) {
            return;
        }
        Field[] fields = tClass.getDeclaredFields();
        if (fields == null || ArrayUtils.isEmpty(fields)) {
            return;
        }
        for (Field field : fields) {
            Resource resource = field.getDeclaredAnnotation(Resource.class);
            if (field.getDeclaredAnnotation(Resource.class) == null) {
                continue;
            }
            // 获取Field的类名
            Class<?> fieldClass = field.getType();
            if (fieldClass == null) {
                continue;
            }
            // 判断Field是否为接口
            if (fieldClass.isInterface()) {
                // 查找接口的多个实现
                Set<String> impSet = getInterfaceImpl(fieldClass.getName());
                if (CollectionUtils.isEmpty(impSet)) {
                    throw new RuntimeException(fieldClass
                            + "接口没有实现，请对该接口的类进行实例化");
                }
                Optional.of(resource.value())
                        // 如果接口只有一个实现或者@Resource有指定bean则注入bean，否则报错
                        .filter(v -> impSet.size() == 1 || StringUtils.isNotBlank(v))
                        .map(fieldBeanName -> {
                            // 注入Field
                            injectField(tClass, fieldBeanName, field);
                            return fieldBeanName;
                        }).orElseGet(() -> {
                    throw new RuntimeException(fieldClass
                            + "接口有多个实现，请指定bean名称");
                });
            } else {
                Optional.of(fieldClass)
                        // 如果Field是类，则直接通过此类的bean名称获取bean
                        .map(it -> getValue(fieldClass))
                        .ifPresent(fieldBeanName ->
                                // 注入Field
                                injectField(tClass, fieldBeanName, field)
                        );
            }
        }
    }

    /**
     * 生成BeanDefinition
     * @param beanName bean名称
     * @param tClass Class对象
     * @param cons 构造函数参数列表
     * @return BeanDefinition
     */
    private BeanDefinition createBeanDefinition(String beanName, Class<?> tClass,
                                                List<ConstructorArg> cons) {
        List<String> interfaceList =  Optional.ofNullable(tClass)
                // 获取类实现的所有接口
                .map(Class::getInterfaces)
                .filter(ArrayUtils::isNotEmpty)
                .map(list -> Stream.of(list)
                        .map(Class::getName).collect(Collectors.toList()))
                .orElse(null);
        return Optional.ofNullable(tClass)
                .map(it -> new BeanDefinition(beanName, it.getName(), interfaceList, cons))
                .orElse(null);
    }

    /** 注入Field */
    private void injectField(Class<?> tClass, String fieldBeanName, Field field) {
        Optional.ofNullable(processComponent(tClass))
                // 获取注入实例的名称
                .map(BeanDefinition::getName)
                .ifPresent(beanName -> {
                    // 获取当前对象
                    Object obj = getBean(beanName);
                    // 获取当前对象的参数所注入的对象
                    Object value = getBean(fieldBeanName);
                    // 修改当前对象Field参数的值
                    Optional.of(obj != null && value != null)
                            .filter(it -> it)
                            .map(it ->  {
                                // 注入Field
                                ReflectionUtils.injectField(field, obj, value);
                                return true;
                            }).orElseGet(() -> {
                                throw new RuntimeException(String.format(
                                        "不存在名称为%s的bean", fieldBeanName));
                            });
                });
    }

    /** 加载配置文件 */
    private void loadConfiguration(Class<?> tClass, JSONObject yamlJson) {
        // 获取Configuration注解
        Configuration configuration = Optional.ofNullable(tClass)
                .map(it -> it.getDeclaredAnnotation(Configuration.class))
                .orElse(null);
        if (configuration == null) {
            return;
        }
        // 获取注解注入的值
        String value = Optional.of(configuration)
                .map(Configuration::value)
                .filter(StringUtils::isNotBlank)
                .orElse(null);
        if (value == null) {
            injectConfiguration(tClass);
            return;
        }
        String[] arr = value.split("\\.");
        // 获取json配置文件
        JSONObject json = yamlJson;
        try {
            for (String name : arr) {
                json = json.getJSONObject(name);
            }
        } catch (Exception e) {
            throw new RuntimeException(tClass + "获取配置文件发生异常，请检查参数");
        }
        Object confObj = Optional.ofNullable(json)
                .map(it -> it.toJavaObject(tClass))
                .orElse(null);
        if (confObj != null) {
            injectConfiguration(tClass, confObj);
        } else {
            injectConfiguration(tClass);
        }

    }

}
