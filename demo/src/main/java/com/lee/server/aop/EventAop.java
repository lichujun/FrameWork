package com.lee.server.aop;

import com.lee.ioc.annotation.After;
import com.lee.ioc.annotation.Aspect;
import com.lee.ioc.annotation.Before;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lichujun
 * @date 2019/2/16 4:10 PM
 */
@Slf4j
@Aspect
public class EventAop {

    @Before(packageName = "com.lee.server.controller", methodName = "test")
    public void before() {
        log.info("hello, you know, world sucks！");
    }

    @After(packageName = "com.lee.server.controller", methodName = "test")
    public void after() {
        log.info("you go!");
    }

}
