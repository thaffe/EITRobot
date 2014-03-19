package com.eit.image;

import com.eit.image.VisualObject;

import java.util.ArrayList;

public class CollectionBox extends VisualObject {

    public CollectionBox(int x, int y, int type) {
        super(x,y, type);
    }


    public static CollectionBox getClosest(ArrayList<CollectionBox> boxes, int type){
        if(boxes.size() == 0) return null;

        CollectionBox best = null;
        double distance = 2;
        for (CollectionBox box : boxes) {
            double d = box.getDistance();
            if(box.type == type && d < distance){
                best = box;
                distance = d;
            }
        }

        return best;
    }
}
