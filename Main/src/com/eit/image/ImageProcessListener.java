package com.eit.image;

import java.util.ArrayList;

/**
 * Created by thaffe on 3/4/14.
 */
public interface ImageProcessListener {
    public void OnBallDetect(VisualObject ball);
    public void OnBoxDetect(VisualObject boxe);
}
