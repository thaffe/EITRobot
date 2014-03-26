package com.eit.image;

import org.opencv.core.Mat;
import org.opencv.core.Scalar;

import java.util.ArrayList;

public abstract class VisualObject {
    public static double DISTANCE_THRESHOLD = 100;
    public static final int RED = 0;
    public static final int GREEN = 1;
    public static final int BLUE = 1;

    protected int type;
    protected int x, y;

    public int matches = 0;

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
        return 2.0 * y / ImageProcessing.CAMERA_HEIGHT - 1;
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

    public abstract double getDistance();

    public abstract void draw(Mat img);

    public double match(VisualObject object) {
        if (this.type != object.type) return 0;

        double x = Math.abs(this.x - object.x);
        double y = Math.abs(this.y - object.y);
        if (x > DISTANCE_THRESHOLD || y > DISTANCE_THRESHOLD) return 0;
        x = 1 - x / DISTANCE_THRESHOLD;
        y = 1 - y/ DISTANCE_THRESHOLD;

        return (x + y) / 2.0;
    }

    public void merge(VisualObject object) {
        matches += 1 + object.matches;
        x = nextAvg(x, object.x);
        y = nextAvg(y, object.y);
    }

    protected int nextAvg(int avg, int newNumber){
        return (int)this.nextAvg(avg,newNumber,matches);
    }

    protected double nextAvg(double avg, double newNumber, int count) {
        return ((count - 1) * avg + newNumber) / (count * 1.0);
    }


    public static VisualObject getClosest(ArrayList<VisualObject> objects) {
        if (objects.size() == 0) return null;

        VisualObject closestObj = objects.get(0);
        double distance = closestObj.getDistance();
        for (int i = 1; i < objects.size(); i++) {
            VisualObject obj = objects.get(i);

            double newDist = obj.getDistance();
            if (newDist < distance) {
                distance = newDist;
                closestObj = obj;
            }

        }
        return closestObj;
    }

    public Scalar getColor(){
        return new Scalar(type == RED ? 255: 0, 0, type==BLUE ? 255:0);
    }
}
