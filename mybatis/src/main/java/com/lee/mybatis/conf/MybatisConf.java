package com.lee.mybatis.conf;

import lombok.Data;
import org.apache.ibatis.session.ExecutorType;

import java.util.List;

/**
 * @author lichujun
 * @date 2019/3/14 14:19
 */

@Data
public class MybatisConf {
    private Boolean cacheEnabled;

    private Boolean useGeneratedKeys;

    private ExecutorType defaultExecutorType;

    private String logImpl;

    private List<String> mappers;
}
