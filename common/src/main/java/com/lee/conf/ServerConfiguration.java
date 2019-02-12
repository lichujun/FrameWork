package com.lee.conf;

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
     * 资源目录
     */
    @Builder.Default
    private String resourcePath = "src/main/resources/";

    /**
     * jsp目录
     */
    @Builder.Default
    private String viewPath = "/templates/";

    /**
     * 静态文件目录
     */
    @Builder.Default
    private String assetPath = "/static/";

    /**
     * 端口号
     */
    @Builder.Default
    private int serverPort = 8080;

    /**
     * tomcat docBase目录
     */
    @Builder.Default
    private String docBase = "";

    /**
     * tomcat contextPath目录
     */
    @Builder.Default
    private String contextPath = "";

}
