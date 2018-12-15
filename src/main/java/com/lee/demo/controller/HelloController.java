package com.lee.demo.controller;

import com.lee.ioc.annotation.Controller;
import com.lee.ioc.annotation.Resource;
import com.lee.mvc.annotation.RequestMapping;
import com.lee.mvc.annotation.RequestParam;
import com.lee.mvc.annotation.ResponseBody;
import com.lee.demo.interfaces.IHello;
import com.lee.demo.service.WorldService;

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
    @ResponseBody
    public String test() {
        return "world sucks";
    }

    @RequestMapping("/helloWorld")
    @ResponseBody
    public String test(@RequestParam("hello") String str) {
        return str;
    }

    @RequestMapping("/")
    public String sayHello() {
        worldService.doSomething();
        helloService.sayHello();
        return null;
    }
}
