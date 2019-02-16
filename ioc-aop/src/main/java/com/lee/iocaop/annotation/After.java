package com.lee.iocaop.annotation;

import java.lang.annotation.*;

/**
 * @author lichujun
 * @date 2019/2/16 4:08 PM
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface After {
    String packageName() default "";
    String className() default "";
    String methodName() default "";
}
