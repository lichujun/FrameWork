package com.lee.iocaop.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

/**
 * @author lichujun
 * @date 2018/12/8 11:20
 */
@Data
@AllArgsConstructor
public final class ConstructorArg {

    private final String ref;

    private final String className;

}
