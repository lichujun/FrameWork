package com.lee.demo.repository;

import com.lee.ioc.annotation.Repository;

/**
 * @author lichujun
 * @date 2018/12/11 10:39 PM
 */
@Repository
public class HelloRepository {

    public void sayHello() {
        System.out.println("world sucks");
    }
}
