package com.lee.ioc.test;

/**
 * @author lichujun
 * @date 2018/12/8 14:46
 */
public class Robot {

    private Hand hand;
    private Mouth mouth;

    public void show(){
        hand.waveHand();
        mouth.speak();
    }
}
