package com.lee.mybatis.conf;

import lombok.Data;

/**
 * @author lichujun
 * @date 2019/3/14 14:23
 */
@Data
public class JdbcConf {
    private String driver = "com.mysql.jdbc.Driver";
    private String url = "jdbc:mysql://127.0.0.1:3306/database?useUnicode=true&characterEncoding=UTF8&autoReconnect=true";
    private String username = "username";
    private String password = "password";
    private Integer initialSize = 10;
    private Integer maxActive = 100;
    private Integer minIdle = 10;
    private Integer maxIdle = 10;
    private Integer maxWait = 3000;
    private String validationQuery = "SELECT @@VERSION";
    private Boolean testWhileIdle = true;
    private Boolean testOnBorrow = true;
    private Integer timeBetweenEvictionRunsMillis = 30000;
    private Integer minEvictableIdleTimeMillis = 600000;
    private Integer numTestsPerEvictionRun = 3;
    private Boolean removeAbandoned = true;
    private Integer removeAbandonedTimeout = 180;
    private Integer defaultQueryTimeoutSeconds = 30;
    private Integer validationQueryTimeoutSeconds = 5;
    private Boolean testOnReturn = true;
}
