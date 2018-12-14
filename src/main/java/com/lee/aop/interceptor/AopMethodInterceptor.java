package com.lee.aop.interceptor;

import com.lee.aop.invocation.MethodInvocation;

/**
 * AOP拦截器
 * @author lichujun
 * @date 2018/12/12 12:37
 */
public interface AopMethodInterceptor {

    Object invoke(MethodInvocation mi) throws Throwable;
}
