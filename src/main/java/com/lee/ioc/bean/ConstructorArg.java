package com.lee.ioc.bean;

import lombok.Data;
import lombok.ToString;

/**
 * @author lichujun
 * @date 2018/12/8 11:20
 */
@Data
@ToString
public class ConstructorArg {

    private int index;

    private String ref;

    private String name;
}
