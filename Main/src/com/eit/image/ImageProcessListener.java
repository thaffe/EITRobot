package com.eit.image;

import java.util.ArrayList;

/**
 * Created by thaffe on 3/4/14.
 */
public interface ImageProcessListener {

    public void onBallDetect(VisualObject ball);

    public void onBoxDetect(VisualObject box);
}
