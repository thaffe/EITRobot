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
    public static int BALL_MATCHES_THRESHOLD = USE_FRAMES / 2;

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
        process.startProcessing(rgb, findBalls);
        activeThreads.add(process);
    }

    private Point computeIntersect(double[] a, double[] b) {

        double x1 = a[0], y1 = a[1], x2 = a[2], y2 = a[3], x3 = b[0], y3 = b[1], x4 = b[2], y4 = b[3];
        double d = ((x1 - x2) * (y3 - y4)) - ((y1 - y2) * (x3 - x4));
        if (d > 0) {
            return new Point(
                    ((x1 * y2 - y1 * x2) * (x3 - x4) - (x1 - x2) * (x3 * y4 - y3 * x4)) / d,
                    ((x1 * y2 - y1 * x2) * (y3 - y4) - (y1 - y2) * (x3 * y4 - y3 * x4)) / d
            );
        } else
            return null;
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgb = inputFrame.rgba();
        if (true) {
            Imgproc.cvtColor(rgb, hsv, Imgproc.COLOR_RGB2HSV, 4);
            ColorRange color = colorRanges[0];
            Core.inRange(hsv, color.minHsv, color.maxHsv, threshold);
            Imgproc.GaussianBlur(threshold, threshold, new Size(9, 9), 2, 2);

            Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(7, 7));
            Imgproc.erode(threshold, threshold, element);
            Imgproc.dilate(threshold, threshold, element);

            Mat lines = new Mat();
            Imgproc.Canny(threshold, threshold, ImageProcessing.CANNY_THRESHOLD / 2, ImageProcessing.CANNY_THRESHOLD);

            int thresh = 30;
            int minLineSize = 10;
            int lineGap = 20;

            Imgproc.HoughLinesP(threshold, lines, 1, Math.PI / 180, thresh, minLineSize, lineGap);

//            ArrayList<CollectionBox> b = new ArrayList<CollectionBox>();
            // Needed for visualization only
//            for (int i = 0; i < lines.cols(); i++) {
//                double[] v = lines.get(0, i);
//                v[0] = 0;
//                v[1] = ((float) v[1] - v[3]) / (v[0] - v[2]) * -v[0] + v[1];
//                v[2] = rgb.cols();
//                v[3] = ((float) v[1] - v[3]) / (v[0] - v[2]) * (rgb.cols() - v[2]) + v[3];
//                lines.put(0, i, v);
//            }

            ArrayList<Point> ponits = new ArrayList<Point>();
            for (int x = 0; x < lines.cols(); x++) {
                double[] vec = lines.get(0, x);
                double x1 = vec[0],
                        y1 = vec[1],
                        x2 = vec[2],
                        y2 = vec[3];
                Point a = new Point(x1, y1);
                Point b = new Point(x2, y2);

                double deltaX = x1-x2, deltaY = y1-y2;
                double graphA = Math.abs(deltaX) < 0.001 ? 0 : deltaY / deltaX;

                Point start = new Point(0, graphA * -a.x + a.y);
                Point end = new Point(rgb.cols(),graphA * (rgb.cols() - b.x) + b.y);

                Core.line(rgb, start, end, new Scalar(255, 0, 0), 3);
                Core.line(rgb, a,b,new Scalar(0,255,0),1);

            }

            return rgb;
        }
        if (balls.size() > 0) {
            printBalls(rgb);
        }

        if (!doLocate) return getImageView(rgb);

        if (inactiveThreads.size() > 0) {
            startProcess(rgb);
        } else {
            for (ImageProcess activeThread : activeThreads) if (!activeThread.isComplete()) return getImageView(rgb);
            if (findBalls) {
                collectBalls();
                notifyListenersBalls();
            } else {
                collectBoxes();
                notifyListenersBoxes();
            }

            doLocate = false;
        }

        return getImageView(rgb);
    }

    public void collectBalls() {
        balls = new ArrayList<Ball>();
        while (!activeThreads.isEmpty()) {
            ImageProcess process = activeThreads.poll();
            ArrayList<Ball> newBalls = process.getBalls();

            for (Ball newBall : newBalls) {
                boolean matched = false;
                for (Ball oldBall : balls) {
                    if (oldBall.match(newBall) > MATCH_THRESHOLD) {
                        oldBall.merge(newBall);
                        matched = true;
                        Log.i(TAG, "MERGING BALL");
                        break;
                    }
                }

                if (!matched) balls.add(newBall);
            }

            inactiveThreads.add(process);
        }

        int i = 0;
        while (i < balls.size()) {
            if (balls.get(i).matches < BALL_MATCHES_THRESHOLD) {
                balls.remove(i);
            } else {
                i++;
            }
        }


    }

    public void collectBoxes() {

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

    private void printBalls(Mat img) {
        double[] pixel;
        for (Ball ball : balls) {
            Scalar color = new Scalar(0, 0, 0);
            color.val[ball.type] = 255;
            Point pt = new Point(ball.x, ball.y);
            Core.circle(img, pt, ball.radius, color, 3);
            Core.circle(img, pt, 3, new Scalar(255, 255, 255), 2);
        }
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
}
