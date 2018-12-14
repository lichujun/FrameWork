package com.lee.mvc;

import com.lee.ioc.core.IocAppContext;
import com.lee.mvc.handler.ControllerHandler;

/**
 * @author lichujun
 * @date 2018/12/13 11:10 PM
 */
public class MvcApplication {

    public static void main(String[] args) {
        IocAppContext.initScanPath("scan.yml");
        IocAppContext context = IocAppContext.getInstance();
        context.init();
        ControllerHandler handler = ControllerHandler.getInstance();
        handler.init(context);
    }
}
