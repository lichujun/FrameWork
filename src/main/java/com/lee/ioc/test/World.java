package com.lee.ioc.test;

import com.lee.ioc.annotation.Component;
import com.lee.ioc.annotation.Resource;

/**
 * @author lichujun
 * @date 2018/12/9 10:37 AM
 */
@Component
public class World {

    @Resource("test")
    private Hello hello;

    public void doSomething() {
        hello.sayHello();
    }

    public void doOther() {
        System.out.println("大声说，请大声说：");
    }
}
