package com.lee.ioc.utils;

/**
 * 类加载工具类
 * @author lichujun
 * @date 2018/12/8 11:24
 */
public class ClassUtils {

    private static ClassLoader getDefaultClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static Class<?> loadClass(String className) {
        try {
            return getDefaultClassLoader().loadClass(className);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
