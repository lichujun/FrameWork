package com.lee.ioc;

import com.lee.ioc.core.IocAppContext;
import com.lee.ioc.test.controller.HelloController;

/**
 * @author lichujun
 * @date 2018/12/9 11:05 AM
 */
public class IocApplication {

    public static void main(String[] args) {
        IocAppContext context = new IocAppContext("scan.yml");
        context.init();
        HelloController helloController = context.getBean(HelloController.class);
        helloController.sayHello();
    }
}
