package com.lee.server.controller;

import com.lee.http.annotation.RequestMapping;
import com.lee.http.annotation.RequestParam;
import com.lee.http.bean.enums.RequestMethod;
import com.lee.http.utils.TraceIDUtils;
import com.lee.iocaop.annotation.Controller;
import com.lee.iocaop.annotation.Resource;
import com.lee.server.common.CommonResponse;
import com.lee.server.conf.DemoConf;
import com.lee.server.entity.Hello;
import com.lee.server.exception.BusinessException;
import com.lee.server.interfaces.IHello;
import com.lee.server.service.WorldService;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.*;

/**
 * @author lichujun
 * @date 2018/12/11 10:39 PM
 */
@Slf4j
@Controller
public class HelloController {

    private static final ExecutorService ES = new ThreadPoolExecutor(8, 16,
            60, TimeUnit.SECONDS, new SynchronousQueue<>(),
            new ThreadPoolExecutor.DiscardPolicy());

    @Resource
    private DemoConf demoConf;

    @Resource("helloService")
    private IHello helloService;
    private final IHello worldService;

    public HelloController(@Resource WorldService worldService) {
        this.worldService = worldService;
    }

    @RequestMapping("/hello")
    public CommonResponse<Hello> test(@RequestParam("word") String word) {
        Hello h = new Hello();
        if (word == null) {
            //h.setWord(helloService.sayHello());
        } else {
            h.setWord(word);
        }
        return CommonResponse.buildOkRes(h);
    }

    @RequestMapping(value = "/helloWorld", method = RequestMethod.POST)
    public CommonResponse<Hello> test(Hello hello) {
        /*try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        execute(() ->
           log.info("I want to see traceID, please!")
        );
        return CommonResponse.buildOkRes(hello);
    }

    @RequestMapping("/")
    public CommonResponse<String> sayHello() throws BusinessException {
        throw new BusinessException("world sucks!");
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    public CommonResponse<String> sayIt() {
        log.info(demoConf.getName());
        return CommonResponse.buildOkRes("world sucks");
    }

    /**
     * 线程池调用，防止线程池日志没traceID
     */
    private void execute(Runnable r) {
        String traceID = TraceIDUtils.getTraceID();
        ES.execute(() -> {
            try {
                TraceIDUtils.setTraceID(traceID);
                r.run();
            } finally {
                TraceIDUtils.removeTraceID();
            }
        });
    }
}
