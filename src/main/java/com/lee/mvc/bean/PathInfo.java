package com.lee.mvc.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author lichujun
 * @date 2018/12/13 11:36 PM
 */
@NoArgsConstructor
@AllArgsConstructor
@Data
public class PathInfo {

    /**
     * 处理http请求的方法
     */
    private String httpMethod;

    /**
     * http请求路径
     */
    private String httpPath;
}
