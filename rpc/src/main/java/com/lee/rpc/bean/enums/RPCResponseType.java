package com.lee.rpc.bean.enums;

import com.lee.rpc.constants.RPCResponseConstants;
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

    SUCCESS(0, RPCResponseConstants.OK),

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
