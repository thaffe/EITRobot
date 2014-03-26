package com.eit.image;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import com.eit.R;
import com.eit.bluetooth.BluetoothCommunication;
import com.eit.bluetooth.NXTTalker;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

public class MyActivity extends Activity implements ImageProcessListener{
    ImageProcessing imgP;

    private boolean NO_BT = false;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_CONNECT_DEVICE = 2;
    private static final int REQUEST_SETTINGS = 3;

    public static final int MESSAGE_TOAST = 1;
    public static final int MESSAGE_STATE_CHANGE = 2;

    public static final String TOAST = "toast";

    private static final int MODE_BUTTONS = 1;

    private BluetoothAdapter mBluetoothAdapter;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;
    private BluetoothCommunication btCommunicator;

    private int mState = NXTTalker.STATE_NONE;
    private int mSavedState = NXTTalker.STATE_NONE;
    private boolean mNewLaunch = true;
    private String mDeviceAddress = null;
    private TextView mStateDisplay;
    private Button mConnectButton;
    private Button mDisconnectButton;

    private int mPower = 80;
    private int mControlsMode = MODE_BUTTONS;

    private boolean mReverse;
    private boolean mReverseLR;
    private boolean mRegulateSpeed;
    private boolean mSynchronizeMotors;

    public MyActivity() {
        imgP = new ImageProcessing(this);
        imgP.addListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        imgP.resume();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.main);
        imgP.create();
    }

    @Override
    public void onBallDetect(VisualObject obj) {
        if(obj != null)
            Log.i(ImageProcessing.TAG,String.format("Hoffset:%f    Distance:%f", obj.getHorizontalOffset(), obj.getDistance())); 
    }

    @Override
    protected void onStop() {
        super.onStop();
        imgP.stop();
    }

    @Override
    public void onBoxDetect(VisualObject obj) {
        if(obj != null)
            Log.i(ImageProcessing.TAG,String.format("Hoffset:%f    Distance:%f",obj.getHorizontalOffset(), obj.getDistance()));
    }
}
