package com.lee.server.exception;

/**
 * @author lichujun
 * @date 2019/2/15 12:00 AM
 */
public class BusinessException extends Exception {
    //无参构造方法
    public BusinessException(){
        super();
    }

    //有参的构造方法
    public BusinessException(String message){
        super(message);

    }
}
