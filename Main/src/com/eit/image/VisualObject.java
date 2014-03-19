package com.eit.image;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class VisualObject {
    protected int type;
    protected int x,y,radius;

    public VisualObject(int x, int y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    /**
     * @return Value indicating horisontal object position relative to camera in number of degrees.
     * 0 represents in the middle, Negative number represents left. Positive number represents right.
     */
    public double getHorizontalOffset() {
        return 2.0*y/ImageProcessing.CAMERA_HEIGHT - 1;
    }

    /**
     * @return The color type of object that it recognizes. Each different color may been given a different value
     */
    public int getType() {
        return type;
    }

    /**
     * @return Value indicating distance.
     */
    public double getDistance() {
        return 1 - radius/(480*0.4/2);
//        return 1.0*x/ImageProcessing.CAMERA_WIDTH -0.25;
    }
}
