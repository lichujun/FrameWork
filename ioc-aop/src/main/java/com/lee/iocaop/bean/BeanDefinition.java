package com.lee.iocaop.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

/**
 * @author lichujun
 * @date 2018/12/8 11:15
 */
@Data
@AllArgsConstructor
public final class BeanDefinition {

    /** 实例名称 */
    private final String name;
    /** class名称 */
    private final String className;
    /** 实例实现的接口 */
    private final List<String> interfaces;
    /** 构造函数的传参 */
    private final List<ConstructorArg> constructorArgs;
}
