package com.lee.http.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Type;

/**
 * @author lichujun
 * @date 2019/3/15 11:49 PM
 */
@Data
@AllArgsConstructor
public final class MethodParam {

    private final Class<?> paramClass;

    private final Type type;
}
