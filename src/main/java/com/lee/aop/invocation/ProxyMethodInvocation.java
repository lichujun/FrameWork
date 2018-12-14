package com.lee.aop.invocation;

/**
 * 方法代理调用
 * @author lichujun
 * @date 2018/12/12 12:33
 */
public interface ProxyMethodInvocation extends MethodInvocation {

    /**
     * 获取代理
     * @return 代理
     */
    Object getProxy();
}
