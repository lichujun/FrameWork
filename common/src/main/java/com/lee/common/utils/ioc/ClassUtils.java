package com.lee.common.utils.ioc;

/**
 * 类加载工具类
 * @author lichujun
 * @date 2018/12/8 11:24
 */
public class ClassUtils {

    /** 通过类名获取Class对象 */
    public static Class<?> loadClass(String className) throws Exception {
        return Class.forName(className);
    }
}
