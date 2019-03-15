package com.lee.http.utils;

import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author lichujun
 * @date 2019/3/15 19:04
 */
public class ParseParamUtils {

    public static List<Object> parse(Map<String, Type> paramClassMap,
                                      Map<String, String> paramMap) {
        List<Object> paramList = new ArrayList<>();
        try {
            paramClassMap.forEach((paramName, type) -> {
                Object param = getParamObject(paramMap, paramName, type);
                paramList.add(param);
            });
        } catch (Throwable e) {
            return null;
        }
        return paramList;
    }

    /**
     * 获取参数的对象
     * @param paramMap 参数Map
     * @param paramName 参数名称
     * @param type 参数类型
     * @return 参数对象
     */
    private static Object getParamObject(Map<String, String> paramMap,
                                         String paramName,
                                         Type type) {
        return Optional.ofNullable(paramName)
                .map(paramMap::get)
                .filter(StringUtils::isNotBlank)
                .map(it -> InvokeControllerUtils.convert(it, type))
                .orElse(null);
    }
}
