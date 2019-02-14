package com.lee.server.common;

import lombok.Data;

/**
 * 统一响应结果
 * @param <T> 业务数据
 */
@Data
public class CommonResponse<T> {

    private Integer code;

    private String msg;

    private T data;

    /**
     * 构建成功响应
     */
    public static <T> CommonResponse<T> buildOkRes(T data) {
        CommonResponse<T> res = new CommonResponse<>();
        setCodeAndMsg(res, ResponseEnums.OK);
        res.setData(data);
        return res;
    }

    /**
     * 构建失败响应
     */
    public static <T> CommonResponse<T> buildErrRes(ResponseEnums responseEnums) {
        CommonResponse<T> res = new CommonResponse<>();
        setCodeAndMsg(res, responseEnums);
        return res;
    }

    public static <T> CommonResponse<T> buildErrRes(Integer code, String msg) {
        CommonResponse<T> res = new CommonResponse<>();
        res.setCode(code);
        res.setMsg(msg);
        return res;
    }

    private static void setCodeAndMsg(CommonResponse res, ResponseEnums responseEnums) {
        if (res == null || responseEnums == null) {
            return;
        }
        res.setCode(responseEnums.getCode());
        res.setMsg(responseEnums.getMsg());
    }
}

