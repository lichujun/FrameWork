package com.lee.server.repository;

import com.alibaba.fastjson.JSON;
import com.lee.iocaop.annotation.Repository;
import com.lee.mybatis.utils.SqlSessionUtils;
import com.lee.server.mapper.DemoMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author lichujun
 * @date 2018/12/11 10:39 PM
 */
@Slf4j
@Repository
public class HelloRepository {

    public String sayHello() {
        DemoMapper demoMapper = SqlSessionUtils.getMapper(DemoMapper.class);
        List<String> addressList = demoMapper.select();
        return JSON.toJSONString(addressList);
    }
}
