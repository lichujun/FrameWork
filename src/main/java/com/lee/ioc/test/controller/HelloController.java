package com.lee.ioc.test.controller;

import com.lee.ioc.annotation.Controller;
import com.lee.ioc.annotation.Resource;
import com.lee.ioc.test.interfaces.IHello;
import com.lee.ioc.test.service.WorldService;

/**
 * @author lichujun
 * @date 2018/12/11 10:39 PM
 */
@Controller
public class HelloController {
    @Resource("helloService")
    private IHello helloService;
    private final IHello worldService;

    public HelloController(@Resource WorldService worldService) {
        this.worldService = worldService;
    }

    public void sayHello() {
        worldService.doSomething();
        helloService.sayHello();
    }
}
