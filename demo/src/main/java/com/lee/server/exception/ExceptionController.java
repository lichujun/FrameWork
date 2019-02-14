package com.lee.server.exception;

import com.lee.ioc.annotation.ControllerAdvice;
import com.lee.ioc.annotation.ExceptionHandler;
import com.lee.server.common.CommonResponse;

/**
 * @author lichujun
 * @date 2019/2/14 10:48 PM
 */
@ControllerAdvice
public class ExceptionController {

    @ExceptionHandler(BusinessException.class)
    public CommonResponse handlerException(BusinessException e) {
        return CommonResponse.buildErrRes(-1, e.getMessage());
    }
}
