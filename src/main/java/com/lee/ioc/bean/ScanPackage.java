package com.lee.ioc.bean;

import lombok.Data;

import java.util.List;

/**
 * 扫描包的实体类
 * @author lichujun
 * @date 2018/12/9 11:22 AM
 */
@Data
public class ScanPackage {

    private List<String> scanPackages;
}
