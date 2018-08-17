package com.github.ixtf.japp.print;

/**
 * @author jzb 2018-08-17
 */
public class Jprint {

    //页面大小以点为计量单位，1点为1英寸的1/72，1英寸为25.4毫米。A4纸大致为595×842点
    public static float mmToPix(float mm) {
        return (float) (mm / 25.4 * 72);
    }
}
