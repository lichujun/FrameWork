package com.lee.aop.invocation;

import java.lang.reflect.Method;

/**
 * 方法调用
 * @author lichujun
 * @date 2018/12/12 12:29
 */
public interface MethodInvocation {

    /**
     * 获取方法本身
     * @return 方法
     */
    Method getMethod();

    /**
     * 获取方法参数
     * @return 方法参数
     */
    Object[] getArguments();

    /**
     * 执行方法
     * @return 执行方法
     * @throws Throwable 异常
     */
    Object proceed() throws Throwable;
}
