package com.lee.mybatis.conf;

import lombok.Data;

/**
 * @author lichujun
 * @date 2019/3/14 14:23
 */
@Data
public class JdbcConf {
    private String driver;
    private String url;
    private String username;
    private String password;
    private Boolean poolPingEnabled;
    private String poolPingQuery;
    private Integer poolPingConnectionsNotUsedFor;
    private Integer poolMaximumActiveConnections;
    private Integer poolMaximumIdleConnections;
    private Integer initialSize;
    private Integer maxActive;
    private Integer minIdle;
    private Integer maxIdle;
    private Integer maxWait;
    private String validationQuery;
    private Boolean testWhileIdle;
    private Boolean testOnBorrow;
    private Integer timeBetweenEvictionRunsMillis;
    private Integer minEvictableIdleTimeMillis;
    private Integer numTestsPerEvictionRun;
    private Boolean removeAbandoned;
    private Integer removeAbandonedTimeout;
    private Integer defaultQueryTimeoutSeconds;
    private Integer validationQueryTimeoutSeconds;
    private Boolean testOnReturn;
}
