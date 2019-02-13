package com.lee.netty.http.conf;

import lombok.Builder;
import lombok.Getter;

/**
 * 服务器相关配置
 * @author lichujun
 * @date 2018/12/15 10:27
 */
@Builder
@Getter
public class ServerConfiguration {

    private Class<?> bootClass;

    @Builder.Default
    private String scanPath = "scan.yml";

    /**
     * 端口号
     */
    @Builder.Default
    private int serverPort = 8080;

}
