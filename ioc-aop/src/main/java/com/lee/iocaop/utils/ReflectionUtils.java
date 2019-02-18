package com.lee.iocaop.utils;

import com.lee.common.utils.exception.ExceptionUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 通过反射完成对象依赖注入
 * @author lichujun
 * @date 2018/12/8 11:36
 */
public class ReflectionUtils {

    /** 将obj对象的field的值改为value */
    public static void injectField(Field field, Object obj, Object value) {
        Optional.ofNullable(field)
                .ifPresent(ExceptionUtils.handleConsumer(it -> {
                    // 设置为可以修改private的field变量
                    it.setAccessible(true);
                    it.set(obj, value);
                }));
    }

    /** 判断两个方法的参数是否相同 */
    public static boolean judgeParams(Method apMethod, Method method) {
        if (apMethod == null || method == null) {
            return false;
        }
        if (apMethod.getParameterCount() != method.getParameterCount()) {
            return false;
        }
        Class<?>[] apParams = apMethod.getParameterTypes();
        if (ArrayUtils.isEmpty(apParams)) {
            return true;
        }
        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null || !params[i].equals(apParams[i])) {
                return false;
            }
        }
        return true;
    }
}
