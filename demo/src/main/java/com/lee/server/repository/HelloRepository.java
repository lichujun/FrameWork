package com.lee.server.repository;

import com.alibaba.fastjson.JSON;
import com.lee.iocaop.annotation.Repository;
import com.lee.mybatis.core.SqlSessionFactoryUtil;
import com.lee.server.mapper.DemoMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;

import java.util.List;

/**
 * @author lichujun
 * @date 2018/12/11 10:39 PM
 */
@Slf4j
@Repository
public class HelloRepository {

    public String sayHello() {
        List<String> addressList;
        try (SqlSession sqlSession = SqlSessionFactoryUtil.getSqlSession()) {
            DemoMapper mapper = sqlSession.getMapper(DemoMapper.class);
            addressList = mapper.select();
        } catch (Throwable e) {
            log.warn("error：" + e);
            return null;
        }
        return JSON.toJSONString(addressList);
    }
}
