package com.lee.iocaop.core;

import com.alibaba.fastjson.JSONObject;
import com.lee.common.utils.exception.ExceptionUtils;
import com.lee.iocaop.annotation.*;
import com.lee.iocaop.bean.AopDefinition;
import com.lee.iocaop.bean.AspectMethod;
import com.lee.iocaop.utils.ReflectionUtils;
import com.lee.iocaop.utils.ScanUtils;
import com.lee.iocaop.bean.BeanDefinition;
import com.lee.iocaop.bean.ConstructorArg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.yaml.snakeyaml.Yaml;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        Yaml yaml = new Yaml();
        Map<String, Object> yamlMap = yaml.load(yamlStream);
        JSONObject yamlJson = new JSONObject(yamlMap);
        Set<String> packageSet = null;
        try {
             packageSet = Optional.of(yamlJson)
                    .map(it -> it.getJSONArray(scanPackage))
                    .map(arr -> arr.toJavaList(String.class))
                    .map(HashSet::new)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("加载需要扫描的包失败", e);
        }
        log.info("正在加载Bean，进行依赖注入...");
        Optional.ofNullable(packageSet)
                .filter(CollectionUtils::isNotEmpty)
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
                        // 处理有aop注解的类
                        processAopClass(tClass);
                        // 加载配置文件
                        loadConfiguration(tClass, yamlJson);
                        // 处理Exception事件
                        handleException(tClass);
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
        Object confObj = null;
        try {
            for (String name : arr) {
                if (json == null) {
                    break;
                }
                json = json.getJSONObject(name);
            }
            confObj = Optional.ofNullable(json)
                    .map(it -> it.toJavaObject(tClass))
                    .orElse(null);
        } catch (Exception e) {
            log.warn("获取【{}】的配置文件失败", value, e);
        }
        if (confObj != null) {
            injectConfiguration(tClass, confObj);
        } else {
            injectConfiguration(tClass);
        }
    }

    /** 处理Exception事件 */
    private void handleException(Class<?> tClass) {
        Optional.ofNullable(tClass)
            // 只处理存在ControllerAdvice注解的类
            .filter(it -> it.getDeclaredAnnotation(ControllerAdvice.class) != null)
            .map(it -> {
                try {
                    String beanName = StringUtils.uncapitalize(it.getSimpleName());
                    Object bean = it.newInstance();
                    putBean(beanName, bean);
                } catch (Exception e) {
                    throw new RuntimeException(it + "Exception的处理类不能存在非默认构造函数");
                }
                return it.getDeclaredMethods();
            })
            .ifPresent(methods -> {
                for (Method method : methods) {
                    Optional.ofNullable(method)
                        // 只处理存在ExceptionHandler注解的方法
                        .map(it -> it.getDeclaredAnnotation(ExceptionHandler.class))
                        .map(ExceptionHandler::value)
                        .ifPresent(it -> putExceptionHandler(it, method));
                }
            });
    }

    /** 处理有aop注解的类 */
    private void processAopClass(Class<?> tClass) {
        // 获取@Aspect注解标注类的bean对象
        Object aspectObj = Optional.ofNullable(tClass)
                .filter(it -> it.getDeclaredAnnotation(Aspect.class) != null)
                .map(it -> {
                    try {
                        return it.newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(tClass + "@Aspect注解的类只能有默认构造方法");
                    }
                })
                .orElse(null);
        if (aspectObj == null) {
            return;
        }
        // 获取@Aspect注解标注类的所有方法
        Method[] methodArr = tClass.getDeclaredMethods();
        if (ArrayUtils.isEmpty(methodArr)) {
            return;
        }
        // 获取@Aspect注解标注类的所有aop事件
        List<AopDefinition> beforeList = new ArrayList<>();
        List<AopDefinition> afterList = new ArrayList<>();
        for (Method method : methodArr) {
            Before before = method.getDeclaredAnnotation(Before.class);
            if (before != null) {
                addAopEvent(before.packageName(), before.className(),
                        before.methodName(), method, beforeList);
            }
            After after = method.getDeclaredAnnotation(After.class);
            if (after != null) {
                addAopEvent(after.packageName(), after.className(),
                        after.methodName(), method, afterList);
            }
        }

        if (CollectionUtils.isNotEmpty(beforeList)) {
            // 注册AOP-BEFORE关系
            processRegisterAop(beforeList, aspectObj, true);
        }

        if (CollectionUtils.isNotEmpty(afterList)) {
            // 注册AOP-AFTER关系
            processRegisterAop(afterList, aspectObj, false);
        }

    }

    /** 注册AOP关系 */
    private void processRegisterAop(List<AopDefinition> aopDefinitionList, Object aspectObj,
                             boolean isBefore) {
        for (AopDefinition aopDefinition : aopDefinitionList) {
            // 如果注解注入了类名，则加载类对象
            if (StringUtils.isNotBlank(aopDefinition.getClassName())) {
                Class<?> tClass;
                try {
                    tClass = Class.forName(aopDefinition.getClassName());
                } catch (Exception e) {
                    throw new RuntimeException(String.format("不存在名为%s的类",
                            aopDefinition.getClassName()));
                }
                Method[] methods = tClass.getDeclaredMethods();
                if (ArrayUtils.isEmpty(methods)) {
                    continue;
                }
                processMethods(methods, aopDefinition, aspectObj, isBefore);
            } else if (StringUtils.isNotBlank(aopDefinition.getPackageName())) {
                Set<Class<?>> classSet;
                try {
                    classSet = ScanUtils.getClasses(aopDefinition.getPackageName());
                } catch (Exception e) {
                    throw new RuntimeException(String.format("不存在包为%s的类",
                            aopDefinition.getPackageName()));
                }
                for (Class<?> tClass : classSet) {
                    Method[] methods = tClass.getDeclaredMethods();
                    if (ArrayUtils.isEmpty(methods)) {
                        continue;
                    }
                    processMethods(methods, aopDefinition, aspectObj, isBefore);
                }
            }
        }
    }

    /** 处理当前类的所有方法的AOP关系 */
    private void processMethods(Method[] methods, AopDefinition aopDefinition,
                                Object aspectObj, boolean isBefore) {
        // 如果注解没有注入方法名，每次判断是否方法名是否一致
        if (StringUtils.isNotBlank(aopDefinition.getMethodName())) {
            for (Method method : methods) {
                if (aopDefinition.getMethodName().equals(method.getName())) {
                    registerAOP(aspectObj, aopDefinition, method, isBefore);
                }
            }
        } else {
            for (Method method : methods) {
                registerAOP(aspectObj, aopDefinition, method, isBefore);
            }
        }
    }

    /** 注册AOP关系 */
    private void registerAOP(Object aspectObj, AopDefinition aopDefinition,
                             Method method, boolean isBefore) {
        if (!judgeMethodAOP(aopDefinition, method)) {
            return;
        }
        AspectMethod aspectMethod = new AspectMethod(aspectObj,
                aopDefinition.getMethod());
        if (isBefore) {
            registerBeforeAOP(method, aspectMethod);
        } else {
            registerAfterAOP(method, aspectMethod);
        }
    }

    /** 判断是否是该方法的织入方法 */
    private boolean judgeMethodAOP(AopDefinition aopDefinition,
                                   Method method) {
        try {
            int paramCount = Optional.ofNullable(aopDefinition)
                    .map(AopDefinition::getMethod)
                    .map(Method::getParameterCount)
                    .orElse(1);
            if (paramCount == 0) {
                return true;
            }
            Method apMethod = Optional.ofNullable(aopDefinition)
                    .map(AopDefinition::getMethod)
                    .orElse(null);
            return ReflectionUtils.judgeParams(apMethod, method);
        } catch (Exception e) {
            return false;
        }
    }

    /** 添加AOP关系 */
    private void addAopEvent(String packageName, String className, String methodName,
                             Method method, List<AopDefinition> list) {
        if (StringUtils.isAllBlank(packageName, className, methodName)) {
            return;
        }
        AopDefinition aopDefinition = AopDefinition.builder()
                .packageName(packageName)
                .className(className)
                .methodName(methodName)
                .method(method)
                .build();
        list.add(aopDefinition);
    }

}
