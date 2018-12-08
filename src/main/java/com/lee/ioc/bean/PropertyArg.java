package com.lee.ioc.bean;

import lombok.Data;
import lombok.ToString;

/**
 * @author lichujun
 * @date 2018/12/8 11:20
 */
@Data
@ToString
public class PropertyArg {

    private String name;

    private String value;

    private String typeName;

    private String ref;
}
