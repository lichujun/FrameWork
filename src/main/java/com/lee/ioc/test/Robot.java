package com.lee.ioc.test;


/**
 * @author lichujun
 * @date 2018/12/8 14:46
 */
public class Robot {


    private Hand h;
    private Mouth mouth;

    public Robot(Hand h) {
        this.h = h;
    }

    public Robot() {

    }

    public void show(){
        h.waveHand();
        mouth.speak();
    }
}
