package com.lee.ioc.test.annotation;

import com.lee.ioc.annotation.Component;

/**
 * @author lichujun
 * @date 2018/12/9 10:35 AM
 */
@Component
public class Hello {
    public void sayHello() {
        System.out.println("world sucks");
    }
}
