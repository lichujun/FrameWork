package com.lee.http.bean.enums;

import com.lee.http.server.vertx.parser.JsonParser;
import com.lee.http.server.vertx.parser.Parser;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author lichujun
 * @date 2019/3/15 18:40
 */
@Slf4j
@Getter
@AllArgsConstructor
public enum ContentType {

    JSON("application/json", new JsonParser()),
    TEXT("text/plain", new JsonParser()),
    //XML("application/xml"),
    ;

    private String content;
    private Parser parser;

    public static Parser getParser(String content) {
        for (ContentType contentType : ContentType.values()) {
            String contentLocal = contentType.getContent();
            if (contentLocal != null && content != null && content.contains(contentLocal)) {
                return contentType.getParser();
            }
        }
        return null;
    }
}
