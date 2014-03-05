package com.eit.image;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;

import java.util.ArrayList;

public class MyActivity extends Activity implements ImageProcessListener{
    ImageProcessing imgP;

    public MyActivity() {
        imgP = new ImageProcessing(this, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);
        imgP.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        imgP.resume();
    }

    @Override
    public void OnBallDetect(ArrayList<Ball> balls) {
        Log.i(ImageProcessing.TAG, "FOUND SOME BALLS:" + balls.size());
    }

    @Override
    public void OnBoxDetect(ArrayList<CollectionBox> boxes) {

    }
}
