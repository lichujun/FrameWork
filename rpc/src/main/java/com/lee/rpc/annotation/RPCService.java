package com.lee.rpc.annotation;

import java.lang.annotation.*;

/**
 * 标记提供RPC服务的注解
 * @author lichujun
 * @date 2019/4/2 8:10 PM
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RPCService {

}
