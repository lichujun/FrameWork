package com.lee.ioc.core;

import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.common.utils.ioc.ReflectionUtils;
import com.lee.common.utils.ioc.ScanUtils;
import com.lee.ioc.annotation.Component;
import com.lee.ioc.annotation.Resource;
import com.lee.ioc.bean.BeanDefinition;
import com.lee.ioc.bean.ScanPackage;
import com.lee.ioc.test.annotation.Hello;
import com.lee.ioc.test.annotation.World;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import java.lang.reflect.Field;
import java.util.*;

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
                .map(it -> Thread.currentThread().getContextClassLoader().getResourceAsStream(it))
                .map(is -> yaml.loadAs(is, ScanPackage.class))
                .map(ScanPackage::getScanPackages)
                .map(HashSet::new)
                .orElse(null);

    }

    /** 扫包，进行依赖注入 */
    private void scanPackages(Set<String> packages) {
        Optional.ofNullable(packages)
                .filter(CollectionUtils::isNotEmpty)
                .map(ExceptionUtils.handlerFunction(ScanUtils::getAllClassPathClasses))
                .filter(CollectionUtils::isNotEmpty)
                .ifPresent(classList ->
                        classList.forEach(tClass -> {
                            // 将@Component注册到容器
                            Optional.ofNullable(processComponent(tClass))
                                    .ifPresent(this::registerBean);
                            // 将@Resource依赖注入
                            processResource(tClass);
                        })
                );
    }

    /** 获取@Component的BeanDefinition */
    private BeanDefinition processComponent(Class<?> tClass) {
        return Optional.ofNullable(tClass)
                .map(it -> it.getDeclaredAnnotation(Component.class))
                .map(Component::value)
                .map(it -> Optional.of(it)
                        .filter(StringUtils::isNotBlank)
                        .orElseGet(() -> StringUtils.uncapitalize(tClass.getSimpleName())))
                .map(it -> new BeanDefinition(it, tClass.getName(), null,
                        null, null))
                .orElse(null);
    }

    /** 将@Resource依赖注入 */
    private void processResource(Class<?> tClass) {
        Optional.ofNullable(tClass)
            .map(Class::getDeclaredFields)
            .ifPresent(fields -> {
                for (Field field : fields) {
                    Optional.ofNullable(field)
                        .map(it -> it.getDeclaredAnnotation(Resource.class))
                        .map(Resource::value)
                        .map(fieldBeanName -> StringUtils.isNotBlank(fieldBeanName) ? fieldBeanName
                                : StringUtils.uncapitalize(field.getName()))
                        .ifPresent(fieldBeanName ->
                            Optional.ofNullable(processComponent(tClass))
                                .map(BeanDefinition::getName)
                                .ifPresent(beanName -> {
                                    Object obj = getBean(beanName);
                                    Object value = getBean(fieldBeanName);
                                    ReflectionUtils.injectField(field, obj, value);
                                })
                        );
                }
            });
    }
}
