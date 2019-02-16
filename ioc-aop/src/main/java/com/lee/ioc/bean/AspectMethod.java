package com.lee.ioc.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Method;

/**
 * @author lichujun
 * @date 2019/2/16 4:45 PM
 */
@Data
@AllArgsConstructor
public class AspectMethod {
    // @Aspect标注的类的bean对象
    private Object object;
    // @Before或@After标注下的方法
    private Method method;
}
