package com.eit.control;

import com.eit.image.Ball;
import com.eit.image.CollectionBox;

import java.util.ArrayList;

/**
 * Created by Lysaker on 05.03.14.
 */
public class EyeCommunicationManager implements EyeCommunication {
    private ArrayList<Ball> balls;
    private ArrayList<CollectionBox> boxes;

    public EyeCommunicationManager() {
        this.balls = null;
    }

    @Override
    public ArrayList<Ball> locateMyBalls() {
        return balls;
    }

    @Override
    public ArrayList<CollectionBox> locateBoxes() {
        return boxes;
    }

    public void setFoundBalls(ArrayList<Ball> balls) {
        this.balls = balls;
    }

    public void setFoundBoxes(ArrayList<CollectionBox> boxes) {
        this.boxes = boxes;
    }

}
