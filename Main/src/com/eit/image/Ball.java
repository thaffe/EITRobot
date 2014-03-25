package com.eit.image;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import java.util.ArrayList;

public class Ball extends VisualObject {
    public static double RADIUS_THRESHOLD = 100;

    protected int radius;

    public Ball(int x, int y, int radius, int type) {
        super(x, y, type);
        this.radius = radius;
    }

    /**
     * @return Value indicating distance, Values lower than 0 means object is inside claw area
     */
    public double getDistance() {
        return 1 - radius / (480 * 0.4 / 2);
    }

    @Override
    public void draw(Mat img) {
        Scalar color = new Scalar(0, 0, 0);
        color.val[this.type] = 255;
        Point pt = new Point(this.x, this.y);
        Core.circle(img, pt, this.radius, color, 3);
        Core.circle(img, pt, 3, new Scalar(255, 255, 255), 2);
    }

    @Override
    public double match(VisualObject object) {
        double match = super.match(object);
        if (match == 0) return 0;

        double r = Math.abs(this.radius - ((Ball) object).radius);
        if (r > RADIUS_THRESHOLD) return 0;
        r = 1 - r / RADIUS_THRESHOLD;

        return nextAvg(match, r, 3);
    }

    @Override
    public void merge(VisualObject object) {
        super.merge(object);
        radius = nextAvg(radius, ((Ball) object).radius);
    }


    public static Ball getClosest(ArrayList<Ball> balls) {
        if (balls.size() == 0) return null;

        Ball ball = balls.get(0);
        double distance = ball.getDistance();
        for (int i = 1; i < balls.size(); i++) {
            Ball b = balls.get(i);
            double newDist = b.getDistance();
            if (newDist < distance) {
                distance = newDist;
                ball = b;
            }

        }

        return ball;
    }
}
