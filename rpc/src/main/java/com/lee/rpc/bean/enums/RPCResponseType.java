package com.lee.rpc.bean.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RPC响应类型
 * @author lichujun
 * @date 2019/3/30 17:44
 */
@Getter
@AllArgsConstructor
public enum RPCResponseType {

    SUCCESS(0, "请求成功"),

    ;

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应信息
     */
    private String msg;
}
