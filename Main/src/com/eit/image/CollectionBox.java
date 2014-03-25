package com.eit.image;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class CollectionBox extends VisualObject implements Comparator<Point> {

    private Point[] points;
    private final double distance;

    public CollectionBox(ArrayList<Point> corners, int type) {
        super(0, 0, type);

        for (Point p : corners) {
            this.x += p.x;
            this.y += p.y;
        }

        this.x /= corners.size();
        this.y /= corners.size();


        ArrayList<Point> top = new ArrayList<Point>(), bot = new ArrayList<Point>();

        for (int i = 0; i < corners.size(); i++) {
            Point p = corners.get(i);
            if (p.y < this.y)
                top.add(p);
            else
                bot.add(p);
        }

        Collections.sort(top, this);
        Collections.sort(bot, this);

        this.points = new Point[]{
                top.get(0), top.get(top.size() - 1),
                bot.get(0), bot.get(bot.size() - 1)
        };

        distance = 1 - 2*Math.abs(points[0].y - points[2].y) / ImageProcessing.CAMERA_WIDTH;
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


    private void drawLines(Mat img, int... index) {
        Point from = points[index[0]];

        Scalar color = new Scalar(0, 0, 0);
        color.val[this.type] = 255;
        for (int i = 1; i < index.length; i++) {
            Core.line(img, from, points[index[i]], color, 2);
            from = points[index[i]];
        }
    }

    @Override
    public void draw(Mat img) {
        drawLines(img, 0, 1, 3, 2, 0);
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
