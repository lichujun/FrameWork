package com.lee.http.server.vertx.parser;

import com.lee.http.utils.InvokeControllerUtils;
import org.apache.commons.lang3.StringUtils;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * @author lichujun
 * @date 2019/3/15 19:20
 */
public class JsonParser implements Parser {

    @Override
    public Object parse(Type type, String body) {
        if (type != null) {
            try {
                return Optional.ofNullable(body)
                        .filter(StringUtils::isNotBlank)
                        .map(it -> InvokeControllerUtils.convert(it, type))
                        .orElse(null);
            } catch (Throwable e) {
                return null;
            }
        } else {
            return null;
        }
    }

}
