package com.lee.ioc;

import com.lee.ioc.core.YamlAppContext;
import com.lee.ioc.test.yaml.Robot;

import java.util.Optional;

/**
 * @author lichujun
 * @date 2018/12/8 10:57
 */
public class YamlApplication {

    public static void main(String[] args) {
        YamlAppContext applicationContext = new YamlAppContext("bean.yml");
        applicationContext.init();
        Robot aiRobot = (Robot) applicationContext.getBeanForYaml("robot");
        Optional.ofNullable(aiRobot).ifPresent(Robot::show);
    }
}
