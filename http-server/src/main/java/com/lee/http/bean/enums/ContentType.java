package com.lee.http.bean.enums;

import com.lee.http.server.vertx.parser.JsonParser;
import com.lee.http.server.vertx.parser.Parser;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lichujun
 * @date 2019/3/15 18:40
 */
@Slf4j
@AllArgsConstructor
public enum ContentType {

    JSON("application/json", JsonParser.class),
    TEXT("text/plain", JsonParser.class),
    //XML("application/xml"),
    ;

    private static final Map<String, Parser> PARSER_MAP = new HashMap<>();

    private String content;
    private Class<? extends Parser> parser;

    public static Parser getParser(String content) {
        if (MapUtils.isEmpty(PARSER_MAP)) {
            synchronized (ContentType.class) {
                if (MapUtils.isEmpty(PARSER_MAP)) {
                    for (ContentType contentType : ContentType.values()) {
                        Parser parser = null;
                        try {
                            parser = contentType.parser.newInstance();
                        } catch (InstantiationException | IllegalAccessException e) {
                            log.warn("生成解析器失败", e);
                        }
                        PARSER_MAP.put(contentType.content, parser);
                    }
                }
            }
        }
        return PARSER_MAP.get(content);
    }
}
