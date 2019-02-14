package com.lee.server.common;

import lombok.Getter;

@Getter
public enum  ResponseEnums {

    OK(0, "请求成功"),

    ;
    private Integer code;
    private String msg;

    ResponseEnums(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}

