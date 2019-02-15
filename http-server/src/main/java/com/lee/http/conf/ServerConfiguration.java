package com.lee.http.conf;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * 服务器相关配置
 * @author lichujun
 * @date 2018/12/15 10:27
 */
@Builder
@Getter
@Setter
public class ServerConfiguration {

    private Class<?> bootClass;

    @Builder.Default
    private String scanPath = "application.yml";

    /**
     * 端口号
     */
    @Builder.Default
    private int serverPort = 8080;

    /**
     * 扫描路径
     */
    @Builder.Default
    private String scanPackage = "scanPackages";

}
