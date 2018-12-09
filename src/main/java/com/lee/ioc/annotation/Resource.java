package com.lee.ioc.annotation;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.*;

/**
 * @author lichujun
 * @date 2018/12/9 10:17 AM
 */
@Target({CONSTRUCTOR, PARAMETER, FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Resource {
    String value() default "";
}
