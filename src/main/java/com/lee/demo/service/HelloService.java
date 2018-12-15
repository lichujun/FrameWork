package com.lee.demo.service;

import com.lee.ioc.annotation.Resource;
import com.lee.ioc.annotation.Service;
import com.lee.demo.interfaces.IHello;
import com.lee.demo.repository.HelloRepository;

/**
 * @author lichujun
 * @date 2018/12/11 10:40 PM
 */
@Service
public class HelloService implements IHello {

    @Resource
    private HelloRepository helloRepository;

    @Override
    public void sayHello() {
        helloRepository.sayHello();
    }

    @Override
    public void doSomething() {

    }
}
