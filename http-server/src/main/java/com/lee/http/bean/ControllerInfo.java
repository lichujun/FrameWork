package com.lee.http.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @author lichujun
 * @date 2018/12/13 11:28 PM
 */

@Data
@AllArgsConstructor
public final class ControllerInfo {

    /**
     * Controller类
     */
    private final Class<?> controllerClass;

    /**
     * 执行的方法
     */
    private final Method invokeMethod;

    /**
     * 方法的参数
     */
    private final Map<String, MethodParam> methodParameter;
}
