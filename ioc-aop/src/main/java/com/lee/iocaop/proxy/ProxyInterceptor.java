package com.lee.iocaop.proxy;

import com.lee.iocaop.bean.AspectMethod;
import com.lee.iocaop.core.IocAppContext;
import com.lee.iocaop.utils.ReflectionUtils;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.apache.commons.collections4.CollectionUtils;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * @author lichujun
 * @date 2019/2/16 2:47 PM
 */
@Slf4j
public class ProxyInterceptor implements MethodInterceptor {

    private static final IocAppContext CONTEXT = IocAppContext.getInstance();

    @Override
    public Object intercept(Object o, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        List<AspectMethod> beforeList = CONTEXT.getBeforeAOP(method);
        // 前置通知
        if (CollectionUtils.isNotEmpty(beforeList)) {
            notify(beforeList, method, args);
        }
        Object result = methodProxy.invokeSuper(o, args);
        List<AspectMethod> afterList = CONTEXT.getAfterAOP(method);
        // 后置通知
        if (CollectionUtils.isNotEmpty(afterList)) {
            notify(afterList, method, args);
        }
        return result;
    }

    /** 通知 */
    private void notify(List<AspectMethod> list, Method method, Object[] args) throws Throwable {
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        for (AspectMethod aspectMethod : list) {
            try {
                Method apMethod = aspectMethod.getMethod();
                apMethod.setAccessible(true);
                if (apMethod.getParameterCount() == 0) {
                    apMethod.invoke(aspectMethod.getObject());
                } else if (ReflectionUtils.judgeParams(apMethod, method)) {
                    apMethod.invoke(aspectMethod.getObject(), args);
                }
            } catch (InvocationTargetException e) {
                throw e.getTargetException();
            }
        }
    }
}
