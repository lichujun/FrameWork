package com.lee.common.throwable;

/**
 * Function异常接口
 * @author lichujun
 * @date 2018/12/9 12:45 AM
 */
@FunctionalInterface
public interface ThrowFunction<T, K, E extends Exception> {

    K apply(T t) throws E;
}
