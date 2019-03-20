package com.lee.mybatis.utils;

import com.lee.mybatis.core.MybatisContext;
import org.apache.ibatis.session.SqlSession;

/**
 * @author lichujun
 * @date 2019/3/18 13:47
 */
public class SqlSessionUtils {

    private static final ThreadLocal<SqlSession> SQL_SESSION_LOCAL = ThreadLocal.withInitial(() -> null);

    public static <T> T getMapper(Class<T> tClass) {
        SqlSession sqlSession = getSqlSession();
        return sqlSession.getMapper(tClass);
    }

    private static SqlSession getSqlSession() {
        SqlSession sqlSessionLocal = SQL_SESSION_LOCAL.get();
        if (sqlSessionLocal == null) {
            SqlSession newSqlSession = MybatisContext.getSqlSession();
            SQL_SESSION_LOCAL.set(newSqlSession);
            return newSqlSession;
        }
        return sqlSessionLocal;
    }

    public static void remove() {
        SqlSession sqlSession = SQL_SESSION_LOCAL.get();
        if (sqlSession != null) {
            sqlSession.close();
            SQL_SESSION_LOCAL.remove();
        }
    }
}
