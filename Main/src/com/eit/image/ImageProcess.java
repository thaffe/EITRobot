package com.eit.image;

import android.util.Log;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * Created by thaffe on 3/18/14.
 */
public class ImageProcess implements Runnable {

    private final Mat hsv, threshold;
    private Mat rgb;
    private boolean isComplete, terminate, start, findBalls;
    private ArrayList<Ball> balls = new ArrayList<Ball>();
    private ArrayList<CollectionBox> boxes = new ArrayList<CollectionBox>();
    private ArrayList<VisualObject> objects;
    private int color;
    public ImageProcess() {
        hsv = new Mat(ImageProcessing.CAMERA_HEIGHT, ImageProcessing.CAMERA_WIDTH, CvType.CV_8UC4);
        threshold = new Mat(hsv.height(), hsv.width(), CvType.CV_8UC1);
    }

    private void startProcessing(Mat rgb, boolean findBalls, int color) {
        start = true;
        this.findBalls = findBalls;
        this.rgb = rgb;
        this.color = color;
    }

    public void startBallProcess(Mat rgb){
        startProcessing(rgb,true,-1);
    }

    public void startBoxProcess(Mat rgb, int color){
        startProcessing(rgb,false,color);
    }


    @Override
    public void run() {
        while (!terminate) {
            if (start) {
                start = false;
                isComplete = false;

                balls.clear();
                boxes.clear();
                Log.i("ROBOT", "PROCESS START");
                if(findBalls){
                    objects = FindFeatures(rgb.getNativeObjAddr(), VisualObject.RED, 0);
                    objects.addAll(FindFeatures(rgb.getNativeObjAddr(), VisualObject.BLUE, 0));

                    for (VisualObject object : objects) {
                        balls.add((Ball)object);
                    }
                }else{
                    objects = FindFeatures(rgb.getNativeObjAddr(), color, 1);
                    for (VisualObject object : objects) {
                        boxes.add((CollectionBox)object);
                    }
                }

                objects = null;
                isComplete = true;
                Log.i("ROBOT", "PROCESS COMPLETE");

//                Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV, 4);
//                for (ImageProcessing.ColorRange colorRange : ImageProcessing.colorRanges) {
//                    setThreshold(colorRange);
//                    cleanImage();
//
//                    if (findBalls)
//                        balls.addAll(processBallColor(colorRange));
//                    else
//                        boxes.addAll(processBoxColor(colorRange));
//
//                }
            }


            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void dispose() {
        terminate = true;
    }

    public ArrayList<Ball> getBalls() {
        return balls;
    }

    public ArrayList<CollectionBox> getBoxes() {
        return boxes;
    }

    public boolean isComplete() {
        return isComplete;
    }

    private void setThreshold(ImageProcessing.ColorRange color) {
        Core.inRange(hsv, color.minHsv, color.maxHsv, threshold);
    }

    private void cleanImage() {
        Imgproc.GaussianBlur(threshold, threshold, new Size(9, 9), 2, 2);

        Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(7, 7));
        Imgproc.erode(threshold, threshold, element);
        Imgproc.dilate(threshold, threshold, element);
    }

    private ArrayList<Ball> processBallColor(ImageProcessing.ColorRange color) {
        Mat circles = new Mat();
        Imgproc.HoughCircles(threshold, circles, Imgproc.CV_HOUGH_GRADIENT, 2, hsv.height() / 4, ImageProcessing.CANNY_THRESHOLD, ImageProcessing.ACCUMULATOR, 10, 0);

        ArrayList<Ball> b = new ArrayList<Ball>(circles.cols());
        for (int x = 0; x < circles.cols(); x++) {
            double vCircle[] = circles.get(0, x);
            b.add(new Ball((int) vCircle[0], (int) vCircle[1], (int) vCircle[2], color.colorIndex));
        }
        return b;
    }

    public ArrayList<CollectionBox> processBoxColor(ImageProcessing.ColorRange color) {

        Mat lines = new Mat();

        Imgproc.Canny(threshold, threshold, ImageProcessing.CANNY_THRESHOLD / 2, ImageProcessing.CANNY_THRESHOLD);

        int thresh = 40;
        int minLineSize = 10;
        int lineGap = 5;

        Imgproc.HoughLinesP(threshold, lines, 1, Math.PI / 180, thresh, minLineSize, lineGap);

        double[][] ls = new double[lines.cols()][4];
        for (int x = 0; x < lines.cols(); x++) {
            double[] vec = lines.get(0, x);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];


            double deltaX = x1 - x2, deltaY = y1 - y2;
            double a = deltaY / deltaX;

            if (Double.isInfinite(a)) {
                vec[0] = x1;
                vec[1] = 0;
                vec[2] = x2;
                vec[3] = rgb.rows();
            } else {
                vec[0] = 0;
                vec[1] = a * -x1 + y1;
                vec[2] = rgb.cols();
                vec[3] = a * (rgb.cols() - x2) + y2;
            }

            ls[x] = vec;

        }

        int[] poly = new int[ls.length];
        for (int i = 0; i < ls.length; i++) poly[i] = -1;

        ArrayList<ArrayList<Point>> corners = new ArrayList<ArrayList<Point>>();
        int curPoly = 0;
        for (int i = 0; i < ls.length; i++) {
            double[] l1 = ls[i];
            for (int j = 0; j < ls.length; j++) {
                double[] l2 = ls[j];
                Point p = computeIntersect(l1, l2);
                if (isInside(p)) {
                    if (poly[i] == -1 && poly[j] == -1) {
                        ArrayList<Point> v = new ArrayList<Point>();
                        v.add(p);
                        corners.add(v);
                        poly[i] = curPoly;
                        poly[j] = curPoly;
                        curPoly++;
                        continue;
                    }
                    if (poly[i] == -1 && poly[j] >= 0) {
                        corners.get(poly[j]).add(p);
                        poly[i] = poly[j];
                        continue;
                    }
                    if (poly[i] >= 0 && poly[j] == -1) {
                        corners.get(poly[i]).add(p);
                        poly[j] = poly[i];
                        continue;
                    }
                    if (poly[i] >= 0 && poly[j] >= 0) {
                        if (poly[i] == poly[j]) {
                            corners.get(poly[i]).add(p);
                            continue;
                        }

                        for (int k = 0; k < corners.get(poly[j]).size(); k++) {
                            corners.get(poly[i]).add(corners.get(poly[j]).get(k));
                        }

                        corners.get(poly[j]).clear();
                        poly[j] = poly[i];
                        continue;
                    }

                }
            }
        }

        ArrayList<CollectionBox> boxes = new ArrayList<CollectionBox>();
        for (ArrayList<Point> c : corners) {
            if (c.size() < 4) continue;

            boxes.add(new CollectionBox(c, color.colorIndex));
        }

        return boxes;
    }


    private Point computeIntersect(double[] l1, double[] l2) {

        double x1 = l1[0], y1 = l1[1], x2 = l1[2], y2 = l1[3];
        double x3 = l2[0], y3 = l2[1], x4 = l2[2], y4 = l2[3];
        double d = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
        if (d > 0) {
            return new Point(
                    ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d,
                    ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d
            );
        }

        return new Point(-1, -1);
    }

    private boolean isInside(Point p) {
        return p.x > 0 && p.y > 0 && p.x < ImageProcessing.CAMERA_WIDTH && p.y < ImageProcessing.CAMERA_HEIGHT;
    }


    public native ArrayList<VisualObject> FindFeatures(long matAddrRgba, int color, int type);


}
