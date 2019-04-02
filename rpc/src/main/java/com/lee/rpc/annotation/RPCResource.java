package com.lee.rpc.annotation;

import com.lee.rpc.constants.RPCCommonConstants;

import java.lang.annotation.*;

/**
 * 标记需要进行RPC调用的Filed的注解
 * @author lichujun
 * @date 2019/4/2 8:20 PM
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RPCResource {

    String[] providers() default {RPCCommonConstants.LOCALHOST_PORT};
}
