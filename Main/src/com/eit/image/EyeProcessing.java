package com.eit.image;

/**
 * Created by Lysaker on 05.03.14.
 */
public interface EyeProcessing {
    void startBoxDetection();

    void startBallDetection();

    void addListener(ImageProcessListener listener);
}
