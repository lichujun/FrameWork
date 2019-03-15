package com.lee.http.server.vertx.parser;

import com.lee.http.bean.MethodParam;

/**
 * @author lichujun
 * @date 2019/3/15 19:22
 */
public interface Parser {

    Object parse(MethodParam methodParam, String body);
}
