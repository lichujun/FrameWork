package com.lee.iocaop.proxy;

import com.lee.iocaop.bean.AspectMethod;
import com.lee.iocaop.core.IocAppContext;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.collections4.CollectionUtils;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author lichujun
 * @date 2019/2/16 2:47 PM
 */
@Slf4j
public class ProxyInterceptor implements MethodInterceptor {

    private static final IocAppContext context = IocAppContext.getInstance();

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        List<AspectMethod> beforeList = context.getBeforeAOP(method);
        // 前置通知
        if (CollectionUtils.isNotEmpty(beforeList)) {
            notify(beforeList);
        }
        Exception exception = null;
        Object result = null;
        try {
            result = methodProxy.invokeSuper(o, args);
        } catch (Exception e) {
            exception = e;
        }
        List<AspectMethod> afterList = context.getAfterAOP(method);
        // 后置通知
        if (CollectionUtils.isNotEmpty(afterList)) {
            notify(afterList);
        }
        if (exception != null) {
            throw exception;
        }
        return result;
    }

    private void notify(List<AspectMethod> list) {
        if (CollectionUtils.isNotEmpty(list)) {
            for (AspectMethod aspectMethod : list) {
                try {
                    aspectMethod.getMethod().invoke(aspectMethod.getObject());
                } catch (Exception e) {
                    log.warn(aspectMethod.getMethod() + "动态代理aop的处理方法需要无参数");
                }
            }
        }
    }
}
