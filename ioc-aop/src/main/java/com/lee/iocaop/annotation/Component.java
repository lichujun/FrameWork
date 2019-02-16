package com.lee.iocaop.annotation;

import java.lang.annotation.*;

/**
 * @author lichujun
 * @date 2018/12/9 10:16 AM
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {
    String value() default "";
}
