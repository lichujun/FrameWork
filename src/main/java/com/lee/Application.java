package com.lee;

import com.lee.ioc.core.ApplicationContext;
import com.lee.ioc.test.Robot;

import java.util.Optional;

/**
 * @author lichujun
 * @date 2018/12/8 10:57
 */
public class Application {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new ApplicationContext("bean.yml");
        applicationContext.init();
        Robot aiRobot = (Robot) applicationContext.getBean("robot");
        Optional.ofNullable(aiRobot).ifPresent(Robot::show);
    }
}
