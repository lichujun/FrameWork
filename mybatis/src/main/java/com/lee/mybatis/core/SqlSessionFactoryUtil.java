package com.lee.mybatis.core;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.lee.mybatis.conf.JdbcConf;
import com.lee.mybatis.conf.MybatisConf;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.util.Optional;

/**
 * @author lichujun
 * @date 2019/3/14 14:29
 */
@Slf4j
public class SqlSessionFactoryUtil {

    /**
     * 会话工厂
     */
    private static SqlSessionFactory sqlSessionFactory;

    /**
     * 构造器注入
     */
    private SqlSessionFactoryUtil() {
    }

    /**
     * 获取连接
     *
     * @return SqlSession
     */
    public static SqlSession getSqlSession() {
        // 设置为自动提交事务
        return sqlSessionFactory.openSession(true);
    }

    @SuppressWarnings(value = "unchecked")
    public static void init(JSONObject yamlJSON) {
        if (yamlJSON == null) {
            log.warn("配置文件为空，无法获数据库的取配置文件");
            return;
        }
        JSONObject mybatisJSON = yamlJSON.getJSONObject("mybatis");
        JSONObject jdbcJSON = yamlJSON.getJSONObject("jdbc");
        if (mybatisJSON == null || jdbcJSON == null) {
            log.warn("未配置数据库的配置文件");
            return;
        }
        // 转化Mybatis配置bean
        MybatisConf mybatisConf;
        JdbcConf jdbcConf;
        try {
            mybatisConf = mybatisJSON.toJavaObject(MybatisConf.class);
            jdbcConf = jdbcJSON.toJavaObject(JdbcConf.class);
        } catch (Throwable e) {
            log.warn("加载数据库的配置文件发生异常", e);
            return;
        }
        if (mybatisConf == null || jdbcConf == null) {
            log.warn("mybatis的配置文件或jdbc的配置文件为空");
            return;
        }
        // 配置事务管理，这里我们使用JDBC的事务
        TransactionFactory trcFactory = new JdbcTransactionFactory();
        // 配置Environment对象，"development"是我们给起的名字
        Environment env = new Environment("development", trcFactory, getDruidPool(jdbcConf));
        // 创建Configuration对象
        Configuration config = new Configuration(env);
        // <settings></settings>中的内容在此处配置
        Optional.ofNullable(mybatisConf.getCacheEnabled())
                .ifPresent(config::setCacheEnabled);
        Optional.ofNullable(mybatisConf.getUseGeneratedKeys())
                .ifPresent(config::setUseGeneratedKeys);
        Optional.ofNullable(mybatisConf.getDefaultExecutorType())
                .ifPresent(config::setDefaultExecutorType);
        if (StringUtils.isNotBlank(mybatisConf.getLogImpl())) {
            try {
                config.setLogImpl((Class<? extends Log>) Class.forName(mybatisConf.getLogImpl()));
            } catch (ClassNotFoundException e) {
                log.warn("设置mybatis的日志发生错误", e);
            }
        }
        if (CollectionUtils.isNotEmpty(mybatisConf.getMappers())) {
            for (String mapper : mybatisConf.getMappers()) {
                Optional.ofNullable(mapper)
                        .filter(StringUtils::isNotBlank)
                        .ifPresent(config::addMappers);
            }
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(config);
    }

    private static DruidDataSource getDruidPool(JdbcConf jdbcConf) {
        DruidDataSource druid = new DruidDataSource();
        druid.setDriverClassName(jdbcConf.getDriver());
        druid.setUrl(jdbcConf.getUrl());
        druid.setUsername(jdbcConf.getUsername());
        druid.setPassword(jdbcConf.getPassword());
        druid.setDefaultAutoCommit(true);
        druid.setInitialSize(jdbcConf.getInitialSize());
        druid.setMaxActive(jdbcConf.getMaxActive());
        druid.setMaxWait(jdbcConf.getMaxWait());
        druid.setQueryTimeout(jdbcConf.getDefaultQueryTimeoutSeconds());
        druid.setMinIdle(jdbcConf.getMinIdle());
        druid.setMinEvictableIdleTimeMillis(jdbcConf.getMinEvictableIdleTimeMillis());
        druid.setTimeBetweenEvictionRunsMillis(jdbcConf.getTimeBetweenEvictionRunsMillis());
        druid.setTestWhileIdle(true);
        druid.setValidationQuery(jdbcConf.getValidationQuery());
        druid.setRemoveAbandoned(true);
        druid.setRemoveAbandonedTimeout(80);
        return druid;
    }
}
