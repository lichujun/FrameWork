package com.lee.rpc.bean;

import lombok.Data;

import java.util.List;

/**
 * RPC调用请求实体
 * @author lichujun
 * @date 2019/3/30 17:41
 */
@Data
public class RPCRequest {

    /**
     * 类名（包含包名）
     */
    private String className;

    /**
     * 方法名称
     */
    private String method;

    /**
     * 参数类型列表
     */
    private List<String> parameterType;

    /**
     * 参数列表
     */
    private List<String> parameters;

    /**
     * 追踪RPC调用的ID
     */
    private String traceID;
}
