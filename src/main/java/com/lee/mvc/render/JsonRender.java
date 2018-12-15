package com.lee.mvc.render;

import com.alibaba.fastjson.JSON;
import com.lee.mvc.RequestHandlerChain;
import lombok.extern.slf4j.Slf4j;

import java.io.PrintWriter;

/**
 * @author lichujun
 * @date 2018/12/15 14:26
 */
@Slf4j
public class JsonRender implements Render {
    private Object jsonData;
    public JsonRender(Object jsonData) {
        this.jsonData = jsonData;
    }
    @Override
    public void render(RequestHandlerChain handlerChain) throws Exception {
        // 设置响应头
        handlerChain.getResponse().setContentType("application/json");
        handlerChain.getResponse().setCharacterEncoding("UTF-8");
        // 向响应中写入数据
        try (PrintWriter writer = handlerChain.getResponse().getWriter()) {
            writer.write(JSON.toJSONString(jsonData));
            writer.flush();
        }
    }
}
