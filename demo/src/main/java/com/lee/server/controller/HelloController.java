package com.lee.server.controller;

import com.lee.http.annotation.RequestMapping;
import com.lee.http.annotation.RequestParam;
import com.lee.http.bean.RequestMethod;
import com.lee.ioc.annotation.Controller;
import com.lee.ioc.annotation.Resource;
import com.lee.server.common.CommonResponse;
import com.lee.server.conf.DemoConf;
import com.lee.server.entity.Hello;
import com.lee.server.exception.BusinessException;
import com.lee.server.interfaces.IHello;
import com.lee.server.service.WorldService;

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
            h.setWord(helloService.sayHello());
        } else {
            h.setWord(word);
        }
        return CommonResponse.buildOkRes(h);
    }

    @RequestMapping(value = "/helloWorld", method = RequestMethod.POST)
    public CommonResponse<Hello> test(Hello hello) {
        System.out.println(worldService.doSomething());
        return CommonResponse.buildOkRes(hello);
    }

    @RequestMapping("/")
    public CommonResponse<String> sayHello() throws BusinessException {
        throw new BusinessException("world sucks!");
    }
}
