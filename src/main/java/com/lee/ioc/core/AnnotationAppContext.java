package com.lee.ioc.core;

import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.common.utils.ioc.ReflectionUtils;
import com.lee.common.utils.ioc.ScanUtils;
import com.lee.ioc.annotation.Component;
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
import java.util.stream.Stream;

/**
 * 注解依赖注入
 * @author lichujun
 * @date 2018/12/9 10:46 AM
 */
public class AnnotationAppContext extends BeanFactoryImpl {

    private String fileName;

    public AnnotationAppContext(String fileName) {
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
                .ifPresent(classSet ->
                        classSet.forEach(tClass -> {
                            // 将@Component注册到容器
                            Optional.ofNullable(processComponent(tClass))
                                    .ifPresent(this::registerBean);
                            // 将@Resource依赖注入到Field
                            processFieldResource(tClass);
                        })
                );
    }

    /** 获取@Component的BeanDefinition */
    private BeanDefinition processComponent(Class<?> tClass) {
        return Optional.ofNullable(tClass)
            // 不扫描没有@Component注解的类
            .filter(it -> it.getDeclaredAnnotation(Component.class) != null)
            .map(it -> Optional.of(it)
                // 过滤有多个构造方法并且要注入成组件的类，否则报错
                .filter(local -> local.getDeclaredConstructors().length == 1)
                .orElseGet(() -> {
                    throw new RuntimeException("实例不能存在多个构造方法");
                }))
            .map(originClass ->
                Optional.of(originClass)
                    // 获取@Component注解
                    .map(local -> local.getDeclaredAnnotation(Component.class))
                    // 读取@Component注解的值
                    .map(Component::value)
                    // 获取注入实例的名称
                    .map(beanName -> Optional.of(beanName)
                            .filter(StringUtils::isNotBlank)
                            .orElseGet(() -> StringUtils.uncapitalize(originClass.getSimpleName())))
                    .map(beanName ->
                            Optional.of(originClass)
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
                                    String annotationValue = Optional.ofNullable(parameter)
                                        .map(p -> p.getDeclaredAnnotation(Resource.class))
                                        .map(Resource::value)
                                        .orElseGet(() -> {
                                            throw new RuntimeException("构造函数的参数必须使用@Resource");
                                        });
                                    String value = StringUtils.isNotBlank(annotationValue) ? annotationValue
                                            : StringUtils.uncapitalize(parameter.getType().getSimpleName());
                                    // 获取构造函数参数的类名
                                    String className = parameter.getParameterizedType().getTypeName();
                                    constructorArgs.add(new ConstructorArg(value, className));
                                });
                                return constructorArgs;
                            })
                            // 创建BeanDefinition对象以用来注册实例
                            .map(cons -> new BeanDefinition(beanName, originClass.getName(),
                                    null, cons))
                            .orElseGet(() -> new BeanDefinition(beanName, originClass.getName(),
                                    null, null))
                    ).orElse(null)
            ).orElse(null);

    }

    /** 将@Resource依赖注入到Field */
    private void processFieldResource(Class<?> tClass) {
        Optional.ofNullable(tClass)
            .filter(it -> it.getDeclaredAnnotation(Component.class) != null)
            // 获取Field参数列表
            .map(Class::getDeclaredFields)
            .ifPresent(fields -> {
                for (Field field : fields) {
                    Optional.ofNullable(field)
                        // 获取Field参数注入的值
                        .map(it -> it.getDeclaredAnnotation(Resource.class))
                        .map(Resource::value)
                        // 获取注入实例的名称
                        .map(fieldBeanName -> StringUtils.isNotBlank(fieldBeanName) ? fieldBeanName
                                : StringUtils.uncapitalize(field.getName()))
                        .ifPresent(fieldBeanName ->
                            Optional.ofNullable(processComponent(tClass))
                                .map(BeanDefinition::getName)
                                .ifPresent(beanName -> {
                                    // 获取当前对象
                                    Object obj = getBean(beanName);
                                    // 获取当前对象的参数所注入的对象
                                    Object value = getBean(fieldBeanName);
                                    // 修改当前对象Field参数的值
                                    ReflectionUtils.injectField(field, obj, value);
                                })
                        );
                }
            });
    }
}
