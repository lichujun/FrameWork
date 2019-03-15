package com.lee.http.server.vertx.codec;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HttpRequest {

    private List<Object> paramList;
}
