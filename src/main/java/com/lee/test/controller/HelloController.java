package com.lee.test.controller;

import com.lee.ioc.annotation.Controller;
import com.lee.ioc.annotation.Resource;
import com.lee.mvc.annotation.RequestMapping;
import com.lee.mvc.annotation.RequestParam;
import com.lee.test.interfaces.IHello;
import com.lee.test.service.WorldService;

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

    @RequestMapping("/hello")
    public String test() {
        return null;
    }

    @RequestMapping("/")
    public String sayHello() {
        worldService.doSomething();
        helloService.sayHello();
        return null;
    }
}
