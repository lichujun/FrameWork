package com.lee.server.service;

import com.lee.server.interfaces.IHello;
import com.lee.server.repository.HelloRepository;
import com.lee.iocaop.annotation.Resource;
import com.lee.iocaop.annotation.Service;

/**
 * @author lichujun
 * @date 2018/12/11 10:40 PM
 */
@Service
public class HelloService implements IHello {

    @Resource
    private HelloRepository helloRepository;

    @Override
    public String sayHello() {
        return helloRepository.sayHello();
    }

    @Override
    public String doSomething() {
        return null;
    }
}
