package com.eit.image;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Created by thaffe on 3/4/14.
 */
public class ImageProcessing implements CameraBridgeViewBase.CvCameraViewListener2 {
    public static int CANNY_THRESHOLD = 500;
    public static int ACCUMULATOR = 50;
    public static double MATCH_THRESHOLD = 0.8;
    public static int CAMERA_WIDTH = 720, CAMERA_HEIGHT = 480;
    public static int USE_FRAMES = 6;

    public int currentView = 0;

    public static final String TAG = "IMAGEPROCESSING";
    public JavaCameraView cameraView;
    private final Activity activity;
    private final ImageProcessListener listener;
    private final BaseLoaderCallback loaderCallback;
    private boolean doLocate;

    public static ColorRange[] colorRanges = new ColorRange[]{
            //Red
            new ColorRange(0, 130, 120, 10, 255, 255, Ball.RED),
            //Blue
            new ColorRange(30, 180, 50, 120, 255, 255, Ball.BLUE)
    };


    private ArrayList<ImageProcess> processes;
    private ArrayList<Ball> balls = new ArrayList<Ball>();

    public ImageProcessing(Activity activity, ImageProcessListener listener) {
        this.processes = new ArrayList<ImageProcess>(USE_FRAMES);
        this.listener = listener;
        this.activity = activity;

        loaderCallback = new BaseLoaderCallback(activity) {
            @Override
            public void onManagerConnected(int status) {
                Log.i(TAG, "com.example OpenCV loaded successfully");
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        // Load native library after(!) OpenCV initialization
                        //System.loadLibrary("mixed_sample");
                        cameraView.enableView();
                        Log.i(TAG, "CAMERA ENABLES");
                        cameraView.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                return false;
                            }
                        });
//                    mOpenCvCameraView.setOnTouchListener(new View.OnTouchListener() {

                    }
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

        ImageSettings imageSettings = new ImageSettings(activity,this);

        FrameLayout l = (FrameLayout) activity.findViewById(R.id.mainView);
        this.cameraView = new JavaCameraView(activity, CameraBridgeViewBase.CAMERA_ID_BACK);
        this.cameraView.setCvCameraViewListener(this);
        this.cameraView.enableFpsMeter();
        l.addView(this.cameraView, 720, 480);
    }

    public void resume() {
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_8, activity, loaderCallback);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        CAMERA_WIDTH = width;
        CAMERA_HEIGHT = height;
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgb = inputFrame.rgba();
        if(balls.size() > 0){
            printBalls(rgb);
        }
        if (!doLocate)
            return getImageView(rgb);

        if (processes.size() < USE_FRAMES) {
            processes.add((ImageProcess) new ImageProcess().execute(rgb));
        }

        for (ImageProcess process : processes) {
            if (process.getStatus() != AsyncTask.Status.FINISHED) return rgb;
        }

        balls = new ArrayList<Ball>();
        for (ImageProcess process : processes) {
            try {
                ArrayList<Ball> newBalls = process.get();
                for (Ball newBall : newBalls) {
                    boolean matched = false;
                    for (Ball oldBall : balls) {
                        if (oldBall.match(newBall) > MATCH_THRESHOLD) {
                            oldBall.merge(newBall);
                            matched = true;
                            break;
                        }
                    }

                    if (!matched) balls.add(newBall);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        int i = 0;
        while(i < balls.size()){
            if(balls.get(i).matches < 2){
                balls.remove(i);
            }else{
                i++;
            }
        }

        listener.OnBallDetect(balls);
        doLocate = false;
        processes.clear();
        return rgb;
    }

    public void locateMyBalls() {
        doLocate = true;
    }

    public void locateBoxes() {

    }

    private Mat getImageView(Mat rgb){
        if(currentView > 0){
            Imgproc.cvtColor(rgb,rgb,Imgproc.COLOR_RGB2HSV);
        }
        switch (currentView){
            case 0: return rgb;
            case 1: return getInRange(rgb, colorRanges[0]);
            case 3: return getInRange(rgb, colorRanges[1]);
        }

        if(currentView == 0) return rgb;

        return rgb;
    }

    private void printBalls(Mat img) {
        double[] pixel;
        for (Ball ball : balls) {
            Scalar color = new Scalar(0,0,0);
            color.val[ball.type] = 255;
            Point pt = new Point(ball.x, ball.y);
            Core.circle(img,pt, ball.radius, color, 3);
            Core.circle(img,pt, 3, new Scalar(255, 255, 255), 2);
        }
    }

    public static ArrayList<Ball> getBalls(Mat img, ColorRange color) {
        // One way to select a range of colors by Hue
        Mat thresh = getInRange(img, color);

        Imgproc.GaussianBlur(thresh, thresh, new Size(9, 9), 0, 0);
        Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(7, 7));
        Imgproc.erode(thresh, thresh, element);
        Imgproc.dilate(thresh, thresh, element);

        Mat circles = new Mat();
        Imgproc.HoughCircles(thresh, circles, Imgproc.CV_HOUGH_GRADIENT, 2, img.height() / 4, CANNY_THRESHOLD, ACCUMULATOR, 10, 0);

        ArrayList<Ball> balls = new ArrayList<Ball>();
        for (int x = 0; x < circles.cols(); x++) {
            double vCircle[] = circles.get(0, x);
            balls.add(new Ball((int) vCircle[0], (int) vCircle[1], (int) vCircle[2], color.colorIndex));
        }

        return balls;
    }

    public static Mat getInRange(Mat hsv, ColorRange color){
        Mat thresh = new Mat(hsv.height(),hsv.width(), CvType.CV_8UC1);
        Core.inRange(hsv, color.minHsv, color.maxHsv, thresh);

        return thresh;
    }


    class ImageProcess extends AsyncTask<Mat, Integer, ArrayList<Ball>> {

        @Override
        protected ArrayList<Ball> doInBackground(Mat... mats) {
            ArrayList<Ball> balls = new ArrayList<Ball>();
            Mat img = mats[0];
            Imgproc.cvtColor(img, img, Imgproc.COLOR_RGB2HSV, 4);

            for (ColorRange c : colorRanges) {
                balls.addAll(ImageProcessing.getBalls(img, c));
            }
            return balls;
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
}
