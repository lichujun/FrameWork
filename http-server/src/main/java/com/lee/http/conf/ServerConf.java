package com.lee.http.conf;


import com.lee.http.bean.ServerEnums;

import java.util.Optional;

public class ServerConf {

    private Integer port = 9000;
    private Integer bossThread = 2;
    private Integer workThread = 8;
    private String name = ServerEnums.NETTY.getServerName();

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        Optional.ofNullable(port)
                .ifPresent(it -> this.port = it);
    }

    public Integer getBossThread() {
        return bossThread;
    }

    public void setBossThread(Integer bossThread) {
        Optional.ofNullable(bossThread)
                .ifPresent(it -> this.bossThread = it);
    }

    public Integer getWorkThread() {
        return workThread;
    }

    public void setWorkThread(Integer workThread) {
        Optional.ofNullable(workThread)
                .ifPresent(it -> this.workThread = it);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        Optional.ofNullable(name)
                .ifPresent(it -> this.name = it);
    }
}
