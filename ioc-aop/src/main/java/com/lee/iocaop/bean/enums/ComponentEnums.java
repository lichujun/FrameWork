package com.lee.iocaop.bean.enums;

import com.lee.iocaop.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.annotation.Annotation;

@AllArgsConstructor
@Getter
public enum ComponentEnums {

    COMPONENT("component", Component.class),
    CONTROLLER("controller", Controller.class),
    SERVICE("service", Service.class),
    REPOSITORY("repository", Repository.class),
    CONFIGURATION("configuration", Configuration.class),
    ;
    private String componentName;
    private Class<? extends Annotation> component;

}
