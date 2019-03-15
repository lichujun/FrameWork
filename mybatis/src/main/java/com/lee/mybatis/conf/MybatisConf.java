package com.lee.mybatis.conf;

import lombok.Data;
import org.apache.ibatis.session.ExecutorType;

import java.util.List;

import static org.apache.ibatis.session.ExecutorType.REUSE;

/**
 * @author lichujun
 * @date 2019/3/14 14:19
 */

@Data
public class MybatisConf {
    private Boolean cacheEnabled = true;

    private Boolean useGeneratedKeys = true;

    private ExecutorType defaultExecutorType = REUSE;

    private String logImpl = "org.apache.ibatis.logging.slf4j.Slf4jImpl";

    private List<String> mappers;
}
