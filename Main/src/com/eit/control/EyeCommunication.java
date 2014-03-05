package com.eit.control;

import com.eit.image.Ball;
import com.eit.image.CollectionBox;

import java.util.ArrayList;

public interface EyeCommunication {
    ArrayList<Ball> locateMyBalls();

    ArrayList<CollectionBox> locateBoxes();
}
