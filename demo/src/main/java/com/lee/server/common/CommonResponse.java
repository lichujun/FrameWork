package com.lee.server.common;

import com.lee.http.utils.TraceIDUtils;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果
 * @param <T> 业务数据
 */
@Data
public class CommonResponse<T> implements Serializable {

    private Integer code;

    private String msg;

    private T data;

    private String traceID;

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
        res.setTraceID(TraceIDUtils.getTraceID());
        res.setCode(code);
        res.setMsg(msg);
        return res;
    }

    private static void setCodeAndMsg(CommonResponse res, ResponseEnums responseEnums) {
        if (res == null || responseEnums == null) {
            return;
        }
        res.setTraceID(TraceIDUtils.getTraceID());
        res.setCode(responseEnums.getCode());
        res.setMsg(responseEnums.getMsg());
    }
}

