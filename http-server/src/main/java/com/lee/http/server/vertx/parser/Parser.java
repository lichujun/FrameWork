package com.lee.http.server.vertx.parser;

import java.lang.reflect.Type;

/**
 * @author lichujun
 * @date 2019/3/15 19:22
 */
public interface Parser {

    Object parse(Type type, String body);
}
