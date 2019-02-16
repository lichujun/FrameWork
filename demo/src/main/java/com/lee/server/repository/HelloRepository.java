package com.lee.server.repository;

import com.lee.iocaop.annotation.Repository;

/**
 * @author lichujun
 * @date 2018/12/11 10:39 PM
 */
@Repository
public class HelloRepository {

    public String sayHello() {
        return "world sucks";
    }
}
