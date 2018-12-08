package com.lee.common.throwable;

/**
 * Consumer异常接口
 * @author lichujun
 * @date 2018/12/9 12:49 AM
 */
@FunctionalInterface
public interface ThrowConsumer<T, E extends Exception> {

    void accept(T t) throws E;
}
