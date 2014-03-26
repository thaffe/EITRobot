package com.eit.image;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import com.eit.R;
import org.opencv.android.*;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.LinkedList;


public class ImageProcessing implements CameraBridgeViewBase.CvCameraViewListener2, EyeProcessing {
    public static boolean DEBUG = false;
    public static double MATCH_THRESHOLD = 0.8;
    public static int CAMERA_WIDTH = 720, CAMERA_HEIGHT = 480;
    public static int USE_FRAMES = 4;
    public static int MATCHES_THRESHOLD = USE_FRAMES / 2;

    public int currentView = 0;
    public static final String TAG = "IMAGEPROCESSING";
    public JavaCameraView cameraView;

    private final Activity activity;
    private final BaseLoaderCallback loaderCallback;
    private boolean doLocate, findBalls;

    private ArrayList<ImageProcessListener> listeners = new ArrayList<ImageProcessListener>();

    private LinkedList<ImageProcess> activeThreads, inactiveThreads;

    private VisualObject closestBall, closestBox;

    public ImageProcessing(Activity activity) {
        this.activeThreads = new LinkedList<ImageProcess>();
        this.inactiveThreads = new LinkedList<ImageProcess>();
        this.activity = activity;

        loaderCallback = new BaseLoaderCallback(activity) {
            @Override
            public void onManagerConnected(int status) {
                Log.i(TAG, "com.example OpenCV loaded successfully");
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                        System.loadLibrary("mixed_sample");
                        init();
                        break;
                    default: {
                        super.onManagerConnected(status);
                    }
                    break;
                }
            }

        };

    }

    public void create() {
        FrameLayout l = (FrameLayout) activity.findViewById(R.id.mainView);
        this.cameraView = new JavaCameraView(activity, CameraBridgeViewBase.CAMERA_ID_BACK);
        this.cameraView.setCvCameraViewListener(this);
        this.cameraView.enableFpsMeter();

        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(CAMERA_WIDTH, CAMERA_HEIGHT, Gravity.TOP | Gravity.LEFT);
        l.addView(this.cameraView, p);
        ImageSettings imageSettings = new ImageSettings(activity, this);
    }

    private void init() {
        cameraView.enableView();

        while (inactiveThreads.size() < USE_FRAMES) {
            ImageProcess process = new ImageProcess();
            new Thread(process).start();
            inactiveThreads.add(process);
        }
    }

    public void resume() {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, activity, loaderCallback);
    }


    public void stop() {
        while (!inactiveThreads.isEmpty()) inactiveThreads.poll().dispose();
        while (!activeThreads.isEmpty()) activeThreads.poll().dispose();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        CAMERA_WIDTH = width;
        CAMERA_HEIGHT = height;
    }

    @Override
    public void onCameraViewStopped() {
    }

    public void startProcess(Mat rgb) {
        ImageProcess process = inactiveThreads.poll();
        if (findBalls)
            process.startBallProcess(rgb);
        else
            process.startBoxProcess(rgb, 0);
        activeThreads.add(process);
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgb = inputFrame.rgba();
        if (DEBUG) {
            ArrayList<VisualObject> rects = FindFeatures(rgb.getNativeObjAddr(), currentView, 0, doLocate ? 1 : 0);
            if (rects != null) {
                VisualObject v = VisualObject.getClosest(rects);
                if(v!= null)
                    v.draw(rgb);
            }
            return rgb;
        }


        if (!doLocate || inactiveThreads.size() == 0) {
            if (closestBall != null) closestBall.draw(rgb);
            if (closestBox != null) closestBox.draw(rgb);
        }
        if (!doLocate) {
            return rgb;
        }

        if (inactiveThreads.size() > 0) {
            startProcess(rgb);
        } else {
            for (ImageProcess activeThread : activeThreads) if (!activeThread.isComplete()) return rgb;

            doLocate = false;
            ArrayList<VisualObject> objects = retrieveObjects();
            objects = mergeVisualObjects(objects);

            VisualObject closest = VisualObject.getClosest(objects);

            if (findBalls) notifyListenersBalls(closest);
            else notifyListenersBoxes(closest);


        }

        return rgb;
    }

    public ArrayList<VisualObject> retrieveObjects() {
        ArrayList<VisualObject> o = new ArrayList<VisualObject>();
        while (!activeThreads.isEmpty()) {
            ImageProcess process = activeThreads.poll();
            inactiveThreads.add(process);
            o.addAll(process.getObjects());
        }
        return o;
    }


    public ArrayList<VisualObject> mergeVisualObjects(ArrayList<VisualObject> objects) {
        ArrayList<VisualObject> matchedObj = new ArrayList<VisualObject>();

        for (VisualObject newObj : objects) {
            boolean matched = false;

            for (VisualObject old : matchedObj) {
                if (old.match(newObj) > MATCH_THRESHOLD) {
                    old.merge(newObj);
                    matched = true;
                    Log.i(TAG, "MERGING OBJECT");
                    break;
                }
            }

            if (!matched) matchedObj.add(newObj);
        }


        Log.i(TAG, "BALLS:" + matchedObj.size());
        int i = 0;
        while (i < matchedObj.size()) {
            if (matchedObj.get(i).matches < MATCHES_THRESHOLD) {
                matchedObj.remove(i);
            } else {
                i++;
            }
        }

        Log.i(TAG, "BALLS:" + matchedObj.size());

        return matchedObj;
    }

    @Override
    public void startBoxDetection(int color) {
        doLocate = true;
        findBalls = false;
    }

    @Override
    public void startBallDetection() {
        doLocate = true;
        findBalls = true;
    }

    @Override
    public void addListener(ImageProcessListener listener) {
        this.listeners.add(listener);
    }

    public void notifyListenersBalls(VisualObject object) {
        this.closestBall = object;
        for (ImageProcessListener listener : listeners) {
            listener.OnBallDetect(object);
        }
    }

    public void notifyListenersBoxes(VisualObject object) {
        this.closestBox = object;
        for (ImageProcessListener listener : listeners) {
            listener.OnBoxDetect(object);
        }
    }

    public native ArrayList<VisualObject> FindFeatures(long matAddrRgba, int color, int type, int test);
}
