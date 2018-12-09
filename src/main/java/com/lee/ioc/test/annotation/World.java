package com.lee.ioc.test.annotation;

import com.lee.ioc.annotation.Component;
import com.lee.ioc.annotation.Resource;

/**
 * @author lichujun
 * @date 2018/12/9 10:37 AM
 */
@Component
public class World {

    @Resource
    private Hello hello;

    public void doSomething() {
        hello.sayHello();
    }
}
