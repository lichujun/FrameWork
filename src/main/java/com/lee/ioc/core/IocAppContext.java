package com.lee.ioc.core;

import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.common.utils.ioc.ReflectionUtils;
import com.lee.common.utils.ioc.ScanUtils;
import com.lee.ioc.annotation.Component;
import com.lee.ioc.annotation.Controller;
import com.lee.ioc.annotation.Resource;
import com.lee.ioc.bean.BeanDefinition;
import com.lee.ioc.bean.ConstructorArg;
import com.lee.ioc.bean.ScanPackage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
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
public class IocAppContext extends BeanFactoryImpl {

    private String fileName;

    public IocAppContext(String fileName) {
        this.fileName = fileName;
    }

    /** 初始化自定义注解的依赖注入 */
    public void init() {
        Optional.ofNullable(loadPackages())
                .ifPresent(this::scanPackages);
    }

    /** 加载yaml配置文件，获取包名 */
    private Set<String> loadPackages() {
        Yaml yaml = new Yaml();
        return Optional.ofNullable(fileName)
                .filter(StringUtils::isNotBlank)
                // 通过加载器将yaml文件加载成流
                .map(it -> Thread.currentThread().getContextClassLoader().getResourceAsStream(it))
                .map(is -> yaml.loadAs(is, ScanPackage.class))
                // 获取包名集合
                .map(ScanPackage::getScanPackages)
                // 包名Set集合
                .map(HashSet::new)
                .orElse(null);

    }

    /** 扫包，进行依赖注入 */
    private void scanPackages(Set<String> packages) {
        Optional.ofNullable(packages)
                .filter(CollectionUtils::isNotEmpty)
                // 扫描包，将Class对象存放在Set集合
                .map(ExceptionUtils.handlerFunction(ScanUtils::getAllClassPathClasses))
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(classSet -> {
                    classSet.forEach(tClass -> Optional.ofNullable(tClass)
                            .filter(it -> it.getInterfaces().length > 0)
                            // 有@Component组件的才将接口和bean的关系注册到容器中
                            .map(this::existInject)
                            .ifPresent(it -> {
                                String beanName = getValue(tClass);
                                // 获取该类所实现的所有接口
                                Set<String> iSet = Stream.of(tClass.getInterfaces())
                                        .map(Class::getName).collect(Collectors.toSet());
                                // 将接口和bean的关系注册到容器
                                registerInterfaceImpl(beanName, iSet);
                            }));
                    classSet.forEach(tClass -> {
                        // 将@Component注册到容器
                        Optional.ofNullable(processComponent(tClass))
                                .ifPresent(this::registerBean);
                    });
                    // 将@Resource依赖注入到Field
                    classSet.forEach(this::processFieldResource);
                    // 加载所有bean
                    classSet.forEach(tClass -> Optional.ofNullable(tClass)
                            .filter(this::existInject)
                            .ifPresent(this::getBean)
                    );
                });
    }

    /** 获取@Component的BeanDefinition */
    private BeanDefinition processComponent(Class<?> tClass) {
        return Optional.ofNullable(tClass)
            // 获取@Component注解
            .filter(this::existInject)
            .map(originClass -> {
                Optional.of(originClass)
                        // 过滤有多个构造方法并且要注入成组件的类，否则报错
                        .filter(local -> local.getDeclaredConstructors().length == 1)
                        .orElseGet(() -> {
                            throw new RuntimeException("实例不能存在多个构造方法");
                        });
                List<ConstructorArg> constructorArgList = Optional.of(originClass)
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
                    }).orElse(null);
                // 创建BeanDefinition对象以用来注册实例
                return createBeanDefinition(getValue(originClass), originClass, constructorArgList);
            }).orElse(null);
    }

    /** 将@Resource依赖注入到Field */
    private void processFieldResource(Class<?> tClass) {
        Optional.ofNullable(tClass)
            .filter(this::existInject)
            // 获取Field参数列表
            .map(Class::getDeclaredFields)
            .ifPresent(fields -> {
                for (Field field : fields) {
                    Optional.ofNullable(field)
                        // 获取@Resource注解
                        .map(it -> it.getDeclaredAnnotation(Resource.class))
                        .ifPresent(resource ->
                            // 获取Field的类名
                            Optional.of(field).map(Field::getType)
                                .ifPresent(fieldClass ->
                                    Optional.of(fieldClass)
                                        // 判断Field是否为接口
                                        .filter(Class::isInterface)
                                        .map(it -> {
                                            // 查找接口的多个实现
                                            Set<String> impSet = getInterfaceImpl(fieldClass.getName());
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
                                            return it;
                                        }).orElseGet(() -> {
                                            Optional.of(fieldClass)
                                                // 如果Field是类，则直接通过此类的bean名称获取bean
                                                .map(it -> getValue(fieldClass))
                                                .ifPresent(fieldBeanName ->
                                                        // 注入Field
                                                        injectField(tClass, fieldBeanName, field)
                                                );
                                            return null;
                                        })
                                )
                        );
                }
            });
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

}
