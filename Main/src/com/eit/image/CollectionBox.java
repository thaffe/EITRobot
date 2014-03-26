package com.eit.image;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.util.ArrayList;
import java.util.Comparator;

public class CollectionBox extends VisualObject implements Comparator<Point> {

    private final double distance;
    private final Point tl, br;

    public CollectionBox(int tlx, int tly, int brx, int bry, int type) {
        super(tlx + Math.abs(brx-tlx)/2, tly + Math.abs(bry - tly)/2, type);
        tl = new Point(tlx,tly);
        br = new Point(brx,bry);

        distance = 1 - 2.0 * (bry-tly) / ImageProcessing.CAMERA_HEIGHT;
    }


    public static CollectionBox getClosest(ArrayList<CollectionBox> boxes, int type) {
        if (boxes.size() == 0) return null;

        CollectionBox best = null;
        double distance = 2;
        for (CollectionBox box : boxes) {
            double d = box.getDistance();
            if (box.type == type && d < distance) {
                best = box;
                distance = d;
            }
        }

        return best;
    }


    @Override
    public void draw(Mat img) {
        Core.rectangle(img, tl,br, getColor(), 3);
        Core.putText(img, String.format("D:%.3f, H:%.3f",distance,getHorizontalOffset()), new Point(x,y), Core.FONT_HERSHEY_SIMPLEX,1, getColor());
    }

    @Override
    public double getDistance() {
        return distance;
    }

    @Override
    public int compare(Point a, Point b) {
        return (int) (a.x - b.x);
    }
}
