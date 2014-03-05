package com.eit.image;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;
import org.opencv.android.*;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import com.eit.R;


public class ImageProcessing implements CameraBridgeViewBase.CvCameraViewListener2, EyeProcessing {
    public static int CANNY_THRESHOLD = 500;
    public static int ACCUMULATOR = 50;
    public static double MATCH_THRESHOLD = 0.8;
    public static int CAMERA_WIDTH = 720, CAMERA_HEIGHT = 480;
    public static int USE_FRAMES = 4;
    public static int BALL_MATCHES_THRESHOLD = USE_FRAMES / 2;

    public int currentView = 0;

    public static final String TAG = "IMAGEPROCESSING";
    public JavaCameraView cameraView;
    private final Activity activity;
    private ArrayList<ImageProcessListener> listeners = new ArrayList<ImageProcessListener>();
    private final BaseLoaderCallback loaderCallback;
    private boolean doLocate;

    public static ConcurrentLinkedQueue<Mat> thresholds = new ConcurrentLinkedQueue<Mat>();
    public static ConcurrentLinkedQueue<Mat> hsvs = new ConcurrentLinkedQueue<Mat>();


    public static ColorRange[] colorRanges = new ColorRange[]{
            //Red
            new ColorRange(0, 90, 100, 10, 255, 255, Ball.RED),
            //Blue
            new ColorRange(110, 120, 10, 130, 255, 200, Ball.BLUE)
    };


    private ArrayList<ImageProcess> processes;
    private ArrayList<Ball> balls = new ArrayList<Ball>();

    public ImageProcessing(Activity activity) {
        this.processes = new ArrayList<ImageProcess>(USE_FRAMES);
        this.activity = activity;

        loaderCallback = new BaseLoaderCallback(activity) {
            @Override
            public void onManagerConnected(int status) {
                Log.i(TAG, "com.example OpenCV loaded successfully");
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS: {
                        // Load native library after(!) OpenCV initialization
                        cameraView.enableView();
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
        FrameLayout l = (FrameLayout) activity.findViewById(R.id.mainView);
        this.cameraView = new JavaCameraView(activity, CameraBridgeViewBase.CAMERA_ID_BACK);
        this.cameraView.setCvCameraViewListener(this);
        this.cameraView.enableFpsMeter();

        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(CAMERA_WIDTH, CAMERA_HEIGHT, Gravity.TOP | Gravity.LEFT);
        l.addView(this.cameraView, p);
        ImageSettings imageSettings = new ImageSettings(activity, this);
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
        if (balls.size() > 0) {
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
                            Log.i(TAG, "MERGING BALL");
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
        while (i < balls.size()) {
            if (balls.get(i).matches < MATCH_THRESHOLD) {
                balls.remove(i);
            } else {
                i++;
            }
        }

        doLocate = false;
        processes.clear();
        for (ImageProcessListener listener : listeners) {
            listener.OnBallDetect(balls);
        }
        return rgb;
    }

    public void locateMyBalls() {
        doLocate = true;
    }

    public void locateBoxes() {

    }

    private Mat getImageView(Mat rgb) {
        Mat img,hsv = null;
        if (currentView > 0) {
            hsv = getHsv(rgb);
        }
        switch (currentView) {
            case 1:
                img = getThresholded(hsv, colorRanges[0]);
                releseThresholded(img);
                break;
            case 3:
                img = getThresholded(hsv, colorRanges[1]);
                releseThresholded(img);
                break;
            case 5:
                Mat t = getThresholded(hsv,colorRanges[1]);

                Mat lines = new Mat();
                Imgproc.Canny(t,t,CANNY_THRESHOLD/2,CANNY_THRESHOLD);
                int threshold = 30;
                int minLineSize = 10;
                int lineGap = 20;

                Imgproc.HoughLinesP(t, lines, 1, Math.PI/180, threshold, minLineSize, lineGap);

                for (int x = 0; x < lines.cols(); x++)
                {
                    double[] vec = lines.get(0, x);
                    double x1 = vec[0],
                            y1 = vec[1],
                            x2 = vec[2],
                            y2 = vec[3];
                    Point start = new Point(x1, y1);
                    Point end = new Point(x2, y2);

                    Core.line(rgb, start, end, new Scalar(255,0,0), 3);

                }
                img = rgb;
                break;

            default:
                img = rgb;
        }

        return img;
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

    public static ArrayList<Ball> getBalls(Mat img, ColorRange color) {
        // One way to select a range of colors by Hue
        Mat thresh = getThresholded(img, color);


        Mat circles = new Mat();

        Imgproc.HoughCircles(thresh, circles, Imgproc.CV_HOUGH_GRADIENT, 2, img.height() / 4, CANNY_THRESHOLD, ACCUMULATOR, 10, 0);

        ArrayList<Ball> balls = new ArrayList<Ball>();
        for (int x = 0; x < circles.cols(); x++) {
            double vCircle[] = circles.get(0, x);
            balls.add(new Ball((int) vCircle[0], (int) vCircle[1], (int) vCircle[2], color.colorIndex));
        }

        releseThresholded(thresh);
        return balls;
    }

    public static Mat getThresholded(Mat hsv, ColorRange color) {
        Mat thresh;
        if (thresholds.size() > 0) {
            thresh = thresholds.poll();
        } else {
            thresh = new Mat(hsv.height(), hsv.width(), CvType.CV_8UC1);
        }
        Core.inRange(hsv, color.minHsv, color.maxHsv, thresh);
        Imgproc.GaussianBlur(thresh, thresh, new Size(9, 9), 2, 2);

        Mat element = Imgproc.getStructuringElement(Imgproc.CV_SHAPE_RECT, new Size(7, 7));
        Imgproc.erode(thresh, thresh, element);
        Imgproc.dilate(thresh, thresh, element);
        return thresh;
    }

    public static void releseThresholded(Mat thresh) {
        thresholds.add(thresh);
    }

    public static Mat getHsv(Mat img) {
        Mat hsv;
        if (hsvs.size() > 0) {
            hsv = hsvs.poll();
        } else {
            hsv = new Mat(img.height(), img.width(), CvType.CV_8UC4);
        }

        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_RGB2HSV, 4);
        return hsv;
    }


    public static void releaseHsv(Mat hsv) {
        hsvs.add(hsv);
    }

    @Override
    public void startBoxDetection() {
        // doLocate = true;
    }

    @Override
    public void startBallDetection() {
        doLocate = true;
    }

    @Override
    public void addListener(ImageProcessListener listener) {
        this.listeners.add(listener);
    }


    class ImageProcess extends AsyncTask<Mat, Integer, ArrayList<Ball>> {

        @Override
        protected ArrayList<Ball> doInBackground(Mat... mats) {
            ArrayList<Ball> balls = new ArrayList<Ball>();
            Mat img = ImageProcessing.getHsv(mats[0]);

            for (ColorRange c : colorRanges) {
                balls.addAll(ImageProcessing.getBalls(img, c));
            }


            releaseHsv(img);
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
