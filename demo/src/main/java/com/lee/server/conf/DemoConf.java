package com.lee.server.conf;

import com.lee.iocaop.annotation.Configuration;
import lombok.Data;

/**
 * @author lichujun
 * @date 2019/2/13 11:20 PM
 */
@Configuration("demo.conf")
@Data
public class DemoConf {
    private String name;
}
