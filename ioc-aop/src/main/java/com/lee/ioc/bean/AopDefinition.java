package com.lee.ioc.bean;

import lombok.Builder;
import lombok.Data;

import java.lang.reflect.Method;

/**
 * @author lichujun
 * @date 2019/2/16 4:15 PM
 */
@Data
@Builder
public class AopDefinition {

    private String packageName;
    private String className;
    private String methodName;
    private Method method;
}
