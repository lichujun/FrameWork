package com.lee.common.throwable;

/**
 * Supplier异常接口
 * @author lichujun
 * @date 2018/12/9 12:52 AM
 */
@FunctionalInterface
public interface ThrowSupplier<T, E extends Exception> {

    /**
     * Supplier异常接口
     * @return T对象
     * @throws E 异常
     */
    T get() throws E;
}
