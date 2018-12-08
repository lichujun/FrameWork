package com.lee.ioc.utils;

/**
 * 类加载工具类
 * @author lichujun
 * @date 2018/12/8 11:24
 */
public class ClassUtils {

    /** 通过类名获取Class对象 */
    public static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
