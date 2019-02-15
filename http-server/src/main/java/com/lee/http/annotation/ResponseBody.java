package com.lee.http.annotation;

import java.lang.annotation.*;

/**
 * 用来标记返回json
 * @author lichujun
 * @date 2018/12/13 11:17 PM
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ResponseBody {
}
