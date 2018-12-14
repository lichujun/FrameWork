package com.lee.mvc.annotation;

import java.lang.annotation.*;

/**
 * http请求路径
 * @author lichujun
 * @date 2018/12/13 11:10 PM
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
public @interface RequestMapping {

    /**
     * 请求路径
     */
    String value() default "";

    /**
     * 请求方法
     */
    String method() default "GET";
}
