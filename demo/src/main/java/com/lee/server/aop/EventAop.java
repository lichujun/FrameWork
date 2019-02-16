package com.lee.server.aop;

import com.lee.iocaop.annotation.After;
import com.lee.iocaop.annotation.Aspect;
import com.lee.iocaop.annotation.Before;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lichujun
 * @date 2019/2/16 4:10 PM
 */
@Slf4j
@Aspect
public class EventAop {

    @Before(className = "com.lee.server.controller.HelloController", methodName = "test")
    public void before() {
        log.info("hello, you know, world sucks！");
    }

    @After(className = "com.lee.server.controller.HelloController", methodName = "sayHello")
    public void after() {
        log.info("you go!");
    }

}
