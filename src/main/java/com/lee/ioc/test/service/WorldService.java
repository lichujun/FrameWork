package com.lee.ioc.test.service;

import com.lee.ioc.annotation.Resource;
import com.lee.ioc.annotation.Service;
import com.lee.ioc.test.interfaces.IHello;
import com.lee.ioc.test.repository.WorldRepository;

/**
 * @author lichujun
 * @date 2018/12/11 10:43 PM
 */
@Service
public class WorldService implements IHello {
    @Resource
    private WorldRepository worldRepository;

    @Override
    public void sayHello() {

    }

    @Override
    public void doSomething() {
        worldRepository.doSomething();
    }
}
