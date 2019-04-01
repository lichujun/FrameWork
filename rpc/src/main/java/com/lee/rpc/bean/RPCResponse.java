package com.lee.rpc.bean;

import lombok.Data;

/**
 * RPC调用响应实体
 * @author lichujun
 * @date 2019/3/30 17:41
 */
@Data
public class RPCResponse<T> {

    /**
     * 响应码
     */
    private Integer code;
    /**
     * 响应信息
     */
    private String msg;
    /**
     * 响应数据
     */
    private T data;
    /**
     * 追踪RPC调用的ID
     */
    private String traceID;
}
