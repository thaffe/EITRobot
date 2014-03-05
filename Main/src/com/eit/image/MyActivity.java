package com.eit.image;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class MyActivity extends Activity implements ImageProcessListener,View.OnTouchListener{
    ImageProcessing imgP;

    public MyActivity() {
        imgP = new ImageProcessing(this, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        imgP.create();
        findViewById(R.id.locateBalls).setOnTouchListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        imgP.resume();
    }

    @Override
    public void OnBallDetect(ArrayList<Ball> balls) {
        Log.i(ImageProcessing.TAG, "FOUND SOM BALLS:" + balls.size());
    }

    @Override
    public void OnBoxDetect(ArrayList<CollectionBox> boxes) {

    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        Log.i(ImageProcessing.TAG, "START TO LOCATE BALLS");
        imgP.locateMyBalls();
        return false;
    }
}
