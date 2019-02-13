package com.lee.server.conf;

import com.lee.ioc.annotation.Configuration;
import lombok.Data;

/**
 * @author lichujun
 * @date 2019/2/13 11:57 PM
 */
@Configuration("server")
@Data
public class ServerConf {

    private int port;
}
