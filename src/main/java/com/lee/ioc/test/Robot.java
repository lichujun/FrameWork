package com.lee.ioc.test;


/**
 * @author lichujun
 * @date 2018/12/8 14:46
 */
public class Robot {


    private Hand h;
    private Mouth m;

    public Robot(Hand h, Mouth m) {
        this.h = h;
        this.m = m;
    }

    public void show(){
        h.waveHand();
        m.speak();
    }
}
