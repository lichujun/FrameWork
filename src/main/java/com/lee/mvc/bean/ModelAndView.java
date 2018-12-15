package com.lee.mvc.bean;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lichujun
 * @date 2018/12/13 11:21 PM
 */
@Getter
public class ModelAndView {

    /**
     * 页面路径
     */
    private String view;

    /**
     * 页面data数据
     */
    private Map<String, Object> model = new HashMap<>();

    public ModelAndView setView(String view) {
        this.view = view;
        return this;
    }

    public ModelAndView setModel(Map<String, Object> model) {
        this.model = model;
        return this;
    }

    public ModelAndView addObject(String attributeName, String attributeValue) {
        model.put(attributeName, attributeValue);
        return this;
    }

    public ModelAndView addAllObjects(Map<String, Object> modelMap) {
        model.putAll(modelMap);
        return this;
    }
}
