package com.lee.http.server.vertx.parser;

import com.lee.http.bean.MethodParam;
import com.lee.http.utils.CastUtils;
import org.apache.commons.lang3.StringUtils;
import java.util.Optional;

/**
 * @author lichujun
 * @date 2019/3/15 19:20
 */
public class JsonParser implements Parser {

    @Override
    public Object parse(MethodParam methodParam, String body) {
        if (methodParam != null) {
            try {
                return Optional.ofNullable(body)
                        .filter(StringUtils::isNotBlank)
                        .map(it -> CastUtils.convert(it, methodParam.getType(),
                                methodParam.getParamClass()))
                        .orElse(null);
            } catch (Throwable e) {
                return null;
            }
        } else {
            return null;
        }
    }

}
