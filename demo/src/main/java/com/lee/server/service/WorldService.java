package com.lee.server.service;

import com.lee.server.interfaces.IHello;
import com.lee.server.repository.WorldRepository;
import com.lee.ioc.annotation.Resource;
import com.lee.ioc.annotation.Service;

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
