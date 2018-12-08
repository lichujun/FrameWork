package com.lee.common.throwable;

/**
 * Function异常接口
 * @author lichujun
 * @date 2018/12/9 12:45 AM
 */
@FunctionalInterface
public interface ThrowFunction<T, K, E extends Exception> {

    /**
     * Function异常接口方法
     * @param t T对象
     * @return 通过T对象得到的K对象
     * @throws E 异常
     */
    K apply(T t) throws E;
}
