package com.lee.server.controller;

import com.lee.server.common.CommonResponse;
import com.lee.server.conf.DemoConf;
import com.lee.server.entity.Hello;
import com.lee.server.interfaces.IHello;
import com.lee.server.service.WorldService;
import com.lee.ioc.annotation.Controller;
import com.lee.ioc.annotation.Resource;
import com.lee.netty.http.annotation.RequestMapping;
import com.lee.netty.http.annotation.RequestParam;
import com.lee.netty.http.bean.RequestMethod;

/**
 * @author lichujun
 * @date 2018/12/11 10:39 PM
 */
@Controller
public class HelloController {

    @Resource
    private DemoConf demoConf;

    @Resource("helloService")
    private IHello helloService;
    private final IHello worldService;

    public HelloController(@Resource WorldService worldService) {
        this.worldService = worldService;
    }

    @RequestMapping("/hello")
    public CommonResponse<Hello> test(@RequestParam("word") String word) {
        Hello h = new Hello();
        if (word == null) {
            h.setWord(demoConf.getName());
        } else {
            h.setWord(word);
        }
        return CommonResponse.buildOkRes(h);
    }

    @RequestMapping(value = "/helloWorld", method = RequestMethod.POST)
    public CommonResponse<Hello> test(Hello hello) {
        return CommonResponse.buildOkRes(hello);
    }

    @RequestMapping("/")
    public CommonResponse<String> sayHello() {
        return CommonResponse.buildOkRes(worldService.doSomething()
                + "--" + helloService.sayHello());
    }
}
