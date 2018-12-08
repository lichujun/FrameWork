package com.lee.ioc.bean;

import lombok.Data;
import lombok.ToString;
import java.util.List;

/**
 * @author lichujun
 * @date 2018/12/8 11:15
 */
@Data
@ToString
public class BeanDefinition {

    /** 实例名称 */
    private String name;
    /** class名称 */
    private String className;
    /** 实例实现的接口 */
    private String interfaceName;
    /** 构造函数的传参 */
    private List<ConstructorArg> constructorArgs;
    /** 注入的参数列表 */
    private PropertyArg propertyArg;
}
