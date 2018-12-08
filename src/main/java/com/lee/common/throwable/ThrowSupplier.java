package com.lee.common.throwable;

/**
 * @author lichujun
 * @date 2018/12/9 12:52 AM
 */
@FunctionalInterface
public interface ThrowSupplier<T, E extends Exception> {

    T get() throws E;
}
