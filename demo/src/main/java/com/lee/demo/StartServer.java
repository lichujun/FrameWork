package com.lee.demo;

import com.lee.server.Application;
import com.lee.server.core.ApplicationContext;

public class StartServer {
    public static void main(String[] args) {
        ApplicationContext.run(Application.class, 9000);
    }
}
