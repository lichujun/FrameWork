package com.lee.ioc.annotation;

import java.lang.annotation.*;

/**
 * @author lichujun
 * @date 2018/12/11 9:44 PM
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Controller {
    String value() default "";
}
