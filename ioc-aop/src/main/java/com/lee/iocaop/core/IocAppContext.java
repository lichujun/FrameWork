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
import java.lang.reflect.Parameter;
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
     * 加载yaml配置文件，初始化依赖注入
     * @param path 配置文件路径
     * @param scanPackage 扫描包的配置项
     * @param tClass 启动类
     * @return JSON格式的配置文件
     */
    public JSONObject init(String path, String scanPackage, Class<?> tClass) {
        if (StringUtils.isBlank(path) || tClass == null) {
            return null;
        }
        // 获取配置文件
        InputStream yamlStream = tClass.getClassLoader().getResourceAsStream(path);
        if (yamlStream == null) {
            log.error("未找到配置文件，请检查配置文件");
            return null;
        }
        Yaml yaml = new Yaml();
        // 将yaml文件读取成map
        Map<String, Object> yamlMap = yaml.load(yamlStream);
        // 将map转换成JSON
        JSONObject yamlJson = new JSONObject(yamlMap);
        Set<String> packageSet = null;
        try {
            // 获取需要扫描的包名
            packageSet = Optional.of(yamlJson)
                    .map(it -> it.getJSONArray(scanPackage))
                    .map(arr -> arr.toJavaList(String.class))
                    .map(HashSet::new)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("加载需要扫描的包失败，请检查配置文件", e);
        }
        if (CollectionUtils.isNotEmpty(packageSet)) {
            log.info("正在加载Bean，进行依赖注入...");
            scanPackages(packageSet, yamlJson);
        }
        return yamlJson;
    }

    /**
     * 扫包，进行依赖注入
     * @param packages 包名集合
     * @param yamlJson JSON格式的配置文件
     */
    private void scanPackages(Set<String> packages, JSONObject yamlJson) {
        log.info("扫描的包名为：" + packages);
        // 扫描所有包名获取到的类集合
        Set<Class<?>> classSet = Optional.ofNullable(packages)
                .filter(CollectionUtils::isNotEmpty)
                // 扫描包，将Class对象存放在Set集合
                .map(ExceptionUtils.handleFunction(ScanUtils::getAllClassPathClasses))
                .orElse(null);
        if (CollectionUtils.isEmpty(classSet)) {
            return;
        }
        // 将扫描到的类注册到容器中
        this.setClassSet(classSet);
        for (Class<?> tClass : classSet) {
            if (tClass == null) {
                continue;
            }
            // 处理有aop注解的类
            processAopClass(tClass);
            // 加载被@Configuration注解标记的类的配置文件
            loadConfiguration(tClass, yamlJson);
            // 异常统一处理
            handleException(tClass);
            // 如果类没有被bean注解标记则返回
            if (!existInject(tClass)) {
                continue;
            }
            // 将接口与它的实现类的关系注册到容器中
            if (tClass.getInterfaces().length >0) {
                // 获取注入的值，为空则为首字母小写的简单类名
                String beanName = getInjectBeanName(tClass);
                // 获取该类所实现的所有接口
                Set<String> iSet = Stream.of(tClass.getInterfaces())
                        .map(Class::getName).collect(Collectors.toSet());
                // 将接口和bean的关系注册到容器
                registerInterfaceImpl(beanName, iSet);
            }
            // 将bean的注册信息注册到容器
            Optional.ofNullable(processComponent(tClass))
                    .ifPresent(this::registerBean);
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
    }

    /**
     * 通过类对象获取bean的注册信息
     * @param tClass bean的类对象
     * @return bean的注册信息
     */
    private BeanDefinition processComponent(Class<?> tClass) {
        if (tClass == null || !existInject(tClass)) {
            return null;
        }
        // 获取构造函数的参数
        Parameter[] parameters = Optional.of(tClass)
                // 获取构造函数列表
                .map(Class::getDeclaredConstructors)
                // 获取唯一的构造函数
                .map(cons -> {
                    if (cons.length > 1) {
                        throw new RuntimeException("标记为bean的类不能存在多个构造函数" + tClass);
                    }
                    return cons[0];
                })
                // 获取构造函数的参数列表
                .map(Constructor::getParameters)
                .orElse(null);
        List<ConstructorArg> constructorArgList = null;
        if (parameters != null && ArrayUtils.isNotEmpty(parameters)) {
            constructorArgList = new ArrayList<>();
            for (Parameter parameter : parameters) {
                // 获取注入实例的名称，构造函数里的所有参数都需@Resource注入实例
                Class<?> paramClass = parameter.getType();
                String paramClassName = paramClass.getName();
                // 获取构造函数的参数的注解
                Resource paramResource = parameter.getDeclaredAnnotation(Resource.class);
                if (paramResource == null) {
                    throw new RuntimeException(tClass + "构造函数的参数需要用@Resource注解标记");
                }
                if (paramClass.isInterface()) {
                    // 获取接口的所有实现
                    Set<String> impSet = getInterfaceImpl(paramClass.getName());
                    String value = paramResource.value();
                    // 如果接口只有一个实现或者@Resource有指定bean则注入bean，否则报错
                    if (impSet.size() == 1 || StringUtils.isNotBlank(value)) {
                        constructorArgList.add(new ConstructorArg(value, paramClassName));
                    }  else {
                        throw new RuntimeException(paramClass + "接口有多个实现，请指定bean名称");
                    }
                } else {
                    // 如果参数是类，则直接通过此类的bean名称获取bean
                    String value = getInjectBeanName(paramClass);
                    constructorArgList.add(new ConstructorArg(value, paramClassName));
                }
            }
        }
        // 创建BeanDefinition对象以用来注册实例
        return createBeanDefinition(getInjectBeanName(tClass), tClass, constructorArgList);
    }

    /**
     * 将bean对象的Field填充，进行依赖注入
     * @param tClass 需要进行Field填充的bean对象的类
     */
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
                    throw new RuntimeException(fieldClass + "此接口找不到被Component注解标记的具体实现");
                }
                String fieldBeanName = resource.value();
                // 如果接口只有一个实现或者@Resource有指定bean则注入bean，否则报错
                if (impSet.size() == 1 || StringUtils.isNotBlank(fieldBeanName)) {
                    // 注入Field
                    injectField(tClass, fieldBeanName, field);
                } else {
                    throw new RuntimeException(fieldClass + "接口有多个实现，请指定bean名称");
                }
            } else {
                // 如果Field是类，则直接通过此类的bean名称获取bean
                String fieldBeanName = getInjectBeanName(fieldClass);
                if (fieldBeanName != null) {
                    // 注入Field
                    injectField(tClass, fieldBeanName, field);
                }
            }
        }
    }

    /**
     * 生成bean的注册信息
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

    /**
     * 注入Field
     * @param tClass bean对象的类
     * @param fieldBeanName Field注入的bean名称
     * @param field bean对象的Field字段
     */
    private void injectField(Class<?> tClass, String fieldBeanName, Field field) {
        BeanDefinition beanDefinition = processComponent(tClass);
        if (beanDefinition == null) {
            return;
        }
        String beanName = beanDefinition.getName();
        if (beanName == null) {
            return;
        }
        Object obj = null;
        Object value = null;
        try {
            // 获取当前对象
            obj = getBean(beanName);
            // 获取当前对象的参数所注入的对象
            value = getBean(fieldBeanName);
        } catch (Throwable e) {
            List<String> paramNameList = Optional.of(tClass)
                    .map(Class::getDeclaredConstructors)
                    .map(it -> it[0])
                    .map(Constructor::getParameterTypes)
                    .map(params -> Stream.of(params)
                            .map(Class::getName)
                            .collect(Collectors.toList()))
                    .orElse(null);
            log.error("注入Field出现异常，{}和{}之间可能存在循环依赖，请检查各自所依赖的bean是否有冲突",
                    tClass.getName(), paramNameList);
            System.exit(0);
        }
        // 修改当前对象Field参数的值
        if (obj != null && value != null) {
            // 注入Field
            ReflectionUtils.injectField(field, obj, value);
        } else {
            throw new RuntimeException(String.format("不存在名称为%s的bean", fieldBeanName));
        }
    }

    /**
     * 加载配置文件
     * @param tClass 配置文件对象的类
     * @param yamlJson JSON格式的配置文件
     */
    private void loadConfiguration(Class<?> tClass, JSONObject yamlJson) {
        // 获取Configuration注解
        Configuration configuration = Optional.ofNullable(tClass)
                .map(it -> it.getDeclaredAnnotation(Configuration.class))
                .orElse(null);
        if (configuration == null) {
            return;
        }
        try {
            tClass.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(tClass + "配置文件类必须要存在默认构造函数");
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

    /**
     * 异常统一处理
     * @param tClass 异常统一处理的类
     */
    private void handleException(Class<?> tClass) {
        if (tClass == null || tClass.getDeclaredAnnotation(ControllerAdvice.class) == null) {
            return;
        }
        try {
            String beanName = StringUtils.uncapitalize(tClass.getSimpleName());
            Object bean = tClass.newInstance();
            putBean(beanName, bean);
        } catch (Exception e) {
            throw new RuntimeException(tClass + "Exception的处理类不能存在非默认构造函数");
        }
        Method[] methods = tClass.getDeclaredMethods();
        if (ArrayUtils.isEmpty(methods)) {
            return;
        }
        for (Method method : methods) {
            Optional.ofNullable(method)
                    // 只处理存在ExceptionHandler注解的方法
                    .map(it -> it.getDeclaredAnnotation(ExceptionHandler.class))
                    .map(ExceptionHandler::value)
                    .ifPresent(it -> putExceptionHandler(it, method));
        }
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

    /** 处理注册AOP关系 */
    private void processRegisterAop(List<AopDefinition> aopDefinitionList, Object aspectObj,
                             boolean isBefore) {
        for (AopDefinition aopDefinition : aopDefinitionList) {
            // 如果注解注入了类名，则加载类对象
            if (StringUtils.isNotBlank(aopDefinition.getClassName())) {
                Class<?> tClass;
                try {
                    tClass = Class.forName(aopDefinition.getClassName());
                } catch (Exception e) {
                    throw new RuntimeException(String.format("AOP织入方法失败，不存在名为%s的类",
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
