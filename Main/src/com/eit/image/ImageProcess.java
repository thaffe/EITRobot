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

    public ImageProcess() {
        hsv = new Mat(ImageProcessing.CAMERA_HEIGHT, ImageProcessing.CAMERA_WIDTH, CvType.CV_8UC4);
        threshold = new Mat(hsv.height(), hsv.width(), CvType.CV_8UC1);
    }

    public void startProcessing(Mat rgb, boolean findBalls) {
        start = true;
        findBalls = true;
        this.rgb = rgb;
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

                Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV, 4);
                for (ImageProcessing.ColorRange colorRange : ImageProcessing.colorRanges) {
                    setThreshold(colorRange);
                    cleanImage();

                    if(findBalls)
                        balls.addAll(processBallColor(colorRange));
                    else
                        boxes.addAll(processBoxColor(colorRange));

                }
                Log.i("ROBOT", "PROCESS COMPLETE");
                isComplete = true;
            }


            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void dispose() {
        terminate = true;
    }

    public ArrayList<Ball> getBalls(){
        return balls;
    }

    public boolean isComplete() {
        return isComplete;
    }

    private void setThreshold(ImageProcessing.ColorRange color){
        Core.inRange(hsv, color.minHsv, color.maxHsv, threshold);
    }

    private void cleanImage(){
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
        Imgproc.Canny(threshold,threshold, ImageProcessing.CANNY_THRESHOLD / 2, ImageProcessing.CANNY_THRESHOLD);

        int thresh = 30;
        int minLineSize = 10;
        int lineGap = 20;

        Imgproc.HoughLinesP(threshold, lines, 1, Math.PI / 180, thresh, minLineSize, lineGap);

        ArrayList<CollectionBox> b = new ArrayList<CollectionBox>();
        for (int x = 0; x < lines.cols(); x++) {
            double[] vec = lines.get(0, x);
            double x1 = vec[0],
                    y1 = vec[1],
                    x2 = vec[2],
                    y2 = vec[3];
            Point start = new Point(x1, y1);
            Point end = new Point(x2, y2);

            Core.line(threshold, start, end, new Scalar(255, 0, 0), 3);

        }

        return null;
    }

}
