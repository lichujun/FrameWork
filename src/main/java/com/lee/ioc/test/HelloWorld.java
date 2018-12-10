package com.lee.ioc.test;

import com.lee.ioc.annotation.Component;
import com.lee.ioc.annotation.Resource;

/**
 * @author lichujun
 * @date 2018/12/9 11:05 PM
 */
@Component
public class HelloWorld {
    @Resource("test")
    private IHello hello;
    private final World world;

    public HelloWorld(@Resource World world) {
        this.world = world;
    }

    public void doOther() {
        world.doOther();
    }

    public void doSomething() {
        hello.doSomething();
    }
}
