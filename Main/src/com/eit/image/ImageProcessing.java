package com.eit.image;

import android.app.Activity;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import com.eit.R;
import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.LinkedList;


public class ImageProcessing implements CameraBridgeViewBase.CvCameraViewListener2, EyeProcessing {
    public static int CANNY_THRESHOLD = 500;
    public static int ACCUMULATOR = 50;
    public static double MATCH_THRESHOLD = 0.8;
    public static int CAMERA_WIDTH = 720, CAMERA_HEIGHT = 480;
    public static int USE_FRAMES = 4;
    public static int MATCHES_THRESHOLD = USE_FRAMES / 2;

    public static ColorRange[] colorRanges = new ColorRange[]{
            new ColorRange(0, 90, 100, 10, 255, 255, Ball.RED),
            new ColorRange(110, 120, 10, 130, 255, 200, Ball.BLUE)
    };
    public int currentView = 0;
    public static final String TAG = "IMAGEPROCESSING";
    public JavaCameraView cameraView;

    private final Activity activity;
    private final BaseLoaderCallback loaderCallback;
    private boolean doLocate, findBalls;
    private Mat hsv, threshold;

    private ArrayList<ImageProcessListener> listeners = new ArrayList<ImageProcessListener>();
    private ArrayList<Ball> balls = new ArrayList<Ball>();
    private ArrayList<CollectionBox> boxes = new ArrayList<CollectionBox>();

    private LinkedList<ImageProcess> activeThreads, inactiveThreads;


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
        hsv = new Mat(ImageProcessing.CAMERA_HEIGHT, ImageProcessing.CAMERA_WIDTH, CvType.CV_8UC4);
        threshold = new Mat(hsv.height(), hsv.width(), CvType.CV_8UC1);

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
        if(findBalls)
            process.startBallProcess(rgb);
        else
            process.startBoxProcess(rgb,0);
        activeThreads.add(process);
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgb = inputFrame.rgba();
//        if (true) {
//            if(doLocate)
//                Log.i("ROBOT","LOCATE THIS SHIT");
//
//            ArrayList<VisualObject> rects = FindFeatures(rgb.getNativeObjAddr(), 1, 0, doLocate ? 1 : 0);
//            if (rects != null) {
//                for (VisualObject rect : rects) {
//                    rect.draw(rgb);
//                }
//
//            }
//            doLocate = false;
//            return rgb;
//        }


        if (!doLocate || inactiveThreads.size() == 0) {
//            for (Ball ball : balls) {
//                ball.draw(rgb);
//            }
//
//            for (CollectionBox box : boxes) {
//                box.draw(rgb);
//            }
        }
        if (!doLocate) {
            return getImageView(rgb);
        }

        if (inactiveThreads.size() > 0) {
            startProcess(rgb);
        } else {
            for (ImageProcess activeThread : activeThreads) if (!activeThread.isComplete()) return getImageView(rgb);

            doLocate = false;

            if (findBalls) {
                balls = retrieveBalls();
                balls = mergeVisualObjects(balls);
                notifyListenersBalls();
            } else {
                boxes = mergeVisualObjects(retrieveBoxes());
                notifyListenersBoxes();
            }

        }

        return getImageView(rgb);
    }

    public ArrayList<Ball> retrieveBalls() {
        ArrayList<Ball> b = new ArrayList<Ball>();
        while (!activeThreads.isEmpty()) {
            ImageProcess process = activeThreads.poll();
            inactiveThreads.add(process);
            b.addAll(process.getBalls());
        }
        return b;
    }

    public ArrayList<CollectionBox> retrieveBoxes() {
        ArrayList<CollectionBox> boxes = new ArrayList<CollectionBox>();
        while (!activeThreads.isEmpty()) {
            ImageProcess process = activeThreads.poll();
            inactiveThreads.add(process);
            boxes.addAll(process.getBoxes());
        }
        return boxes;
    }

    public <T extends VisualObject> ArrayList<T> mergeVisualObjects(ArrayList<T> objects) {
        ArrayList<T> matchedObj = new ArrayList<T>();

        for (T newObj : objects) {
            boolean matched = false;

            for (T old : matchedObj) {
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

    private Mat getImageView(Mat rgb) {
        Mat img, hsv = null;
        if (currentView == 0) return rgb;

        Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV, 4);

        if (currentView == 1 || currentView == 3) {
            ColorRange c = colorRanges[currentView / 2];
            Core.inRange(hsv, c.minHsv, c.maxHsv, threshold);
            return threshold;
        }

        return rgb;
    }

    @Override
    public void startBoxDetection() {
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

    public void notifyListenersBalls() {
        Ball ball = Ball.getClosest(balls);
        for (ImageProcessListener listener : listeners) {
            listener.OnBallDetect(balls);
        }
    }

    public void notifyListenersBoxes() {
        for (ImageProcessListener listener : listeners) {
            listener.OnBoxDetect(boxes);
        }
    }

    public static class ColorRange {
        Scalar minHsv;
        Scalar maxHsv;
        int colorIndex;

        public ColorRange(int minR, int minG, int minB, int maxR, int maxG, int maxB, int colorIndex) {
            this.minHsv = new Scalar(minR, minG, minB, 0);
            this.maxHsv = new Scalar(maxR, maxG, maxB, 0);
            this.colorIndex = colorIndex;
        }

    }


    public native ArrayList<VisualObject> FindFeatures(long matAddrRgba, int color, int type, int test);
}
