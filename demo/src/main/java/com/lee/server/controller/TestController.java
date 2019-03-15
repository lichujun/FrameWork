package com.lee.server.controller;

import com.lee.http.annotation.RequestMapping;
import com.lee.http.annotation.RequestParam;
import com.lee.iocaop.annotation.Controller;
import com.lee.server.entity.People;

import java.util.List;

/**
 * @author lichujun
 * @date 2019/3/15 9:53 PM
 */
@Controller
public class TestController {

    @RequestMapping("/demo1")
    public List<People> test(List<People> peopleList) {
        peopleList.forEach(p -> System.out.println(p.getName()));
        return peopleList;
    }

    @RequestMapping("/demo")
    public int demo(@RequestParam("a") int a) {
        return a;
    }

}
