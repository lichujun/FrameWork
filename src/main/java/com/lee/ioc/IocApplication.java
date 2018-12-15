package com.lee.ioc;

import com.lee.ioc.core.IocAppContext;
import com.lee.demo.controller.HelloController;

/**
 * @author lichujun
 * @date 2018/12/9 11:05 AM
 */
public class IocApplication {

    public static void main(String[] args) {
        IocAppContext.initScanPath("scan.yml");
        IocAppContext context = IocAppContext.getInstance();
        context.init();
        HelloController helloController = context.getBean(HelloController.class);
        helloController.sayHello();
    }
}
