package com.lee.http.bean;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author lichujun
 * @date 2018/12/13 11:36 PM
 */
@AllArgsConstructor
@Data
public final class PathInfo {

    /**
     * http请求路径
     */
    private final String httpPath;

    /**
     * 处理http请求的方法
     */
    private final String httpMethod;
}
