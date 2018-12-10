package com.lee.ioc;

import com.lee.ioc.core.AnnotationAppContext;
import com.lee.ioc.test.HelloWorld;
import com.lee.ioc.test.World;

/**
 * @author lichujun
 * @date 2018/12/9 11:05 AM
 */
public class AnnotationApplication {

    public static void main(String[] args) {
        AnnotationAppContext context = new AnnotationAppContext("scan.yml");
        context.init();
        World world = context.getBean(World.class);
        world.doOther();
        world.doSomething();
        HelloWorld helloWorld = context.getBean(HelloWorld.class);
        helloWorld.doOther();
        helloWorld.doSomething();
    }
}
