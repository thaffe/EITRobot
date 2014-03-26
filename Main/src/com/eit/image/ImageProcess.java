package com.eit.image;

import android.util.Log;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * Created by thaffe on 3/18/14.
 */
public class ImageProcess implements Runnable {

    private Mat rgb;
    private boolean isComplete, terminate, start, findBalls;
    private ArrayList<VisualObject> objects = new ArrayList<VisualObject>();
    private int color;

    public ImageProcess(){
        rgb = new Mat(ImageProcessing.CAMERA_HEIGHT, ImageProcessing.CAMERA_WIDTH, CvType.CV_8UC4);
    }

    private void startProcessing(Mat rgb, boolean findBalls, int color) {
        start = true;
        this.findBalls = findBalls;
        rgb.copyTo(this.rgb);
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
                objects.clear();
                Log.i("ROBOT", "PROCESS START");
                if(findBalls){
                    addAll(FindFeatures(rgb.getNativeObjAddr(), VisualObject.RED, 0));
                    addAll(FindFeatures(rgb.getNativeObjAddr(), VisualObject.BLUE, 0));
                }else{
                    addAll(FindFeatures(rgb.getNativeObjAddr(), color, 1));
                }

                isComplete = true;
                Log.i("ROBOT", "PROCESS COMPLETE count:"+objects.size());

            }


            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private void addAll(ArrayList<VisualObject> objs){
        if(objs != null){
            for (VisualObject obj : objs) {
                objects.add(obj);
            }
        }
    }

    public void dispose() {
        terminate = true;
    }

    public ArrayList<VisualObject> getObjects(){
        return objects;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public native ArrayList<VisualObject> FindFeatures(long matAddrRgba, int color, int type);


}
