package com.lee.http.server.vertx.codec;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class HttpRequest {

    private Map<String, String> params;
    private String body;

}
