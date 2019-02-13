package com.lee.ioc.annotation;

import java.lang.annotation.*;

/**
 * @author lichujun
 * @date 2019/2/13 10:30 PM
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Configuration {
     String value() default "";
}
