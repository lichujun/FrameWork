package com.lee.iocaop.annotation;

import java.lang.annotation.*;

/**
 * @author lichujun
 * @date 2018/12/11 10:31 PM
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Repository {
    String value() default "";
}
