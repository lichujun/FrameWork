package com.lee.common.throwable;

/**
 * Predicate异常接口
 * @author lichujun
 * @date 2018/12/9 12:54 AM
 */
@FunctionalInterface
public interface ThrowPredicate<T, E extends Exception> {

    boolean test(T t);
}
