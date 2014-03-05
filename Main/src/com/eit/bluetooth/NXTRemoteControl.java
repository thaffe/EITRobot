package com.eit.bluetooth;

import android.app.Activity;
import android.os.Bundle;

/*
 * Copyright (c) 2010 Jacek Fedorynski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This file is derived from:
 *
 * http://developer.android.com/resources/samples/BluetoothChat/src/com/example/android/BluetoothChat/BluetoothChat.html
 *
 * Copyright (c) 2009 The Android Open Source Project
 */


/*
 * TODO:
 *
 * tilt controls
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.eit.R;
import com.eit.image.*;

import java.util.ArrayList;

public class NXTRemoteControl extends Activity implements ImageProcessListener {

    private boolean NO_BT = false;
    ImageProcessing imgP;


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
    private BluetoothCommunicator btCommunicator;

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

    public NXTRemoteControl(){
        imgP = new ImageProcessing(this, this);

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "NXT Remote Control");

        if (!NO_BT) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            if (mBluetoothAdapter == null) {
                Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
        }

       // setupUI();
        NXTTalker mNXTTalker = new NXTTalker(mHandler);
        btCommunicator = new BluetoothCommunicator(mNXTTalker);

        setContentView(R.layout.main);
        imgP.create();

    }

    private class DirectionButtonOnTouchListener implements OnTouchListener {

        private double lmod;
        private double rmod;

        public DirectionButtonOnTouchListener(double l, double r) {
            lmod = l;
            rmod = r;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            if (action == MotionEvent.ACTION_DOWN) {
                byte power = (byte) mPower;
                if (mReverse) {
                    power *= -1;
                }
                byte l = (byte) (power*lmod);
                byte r = (byte) (power*rmod);
                if (!mReverseLR) {
                    //mNXTTalker.motors(l, r, mRegulateSpeed, mSynchronizeMotors);
                    //btCommunicator.move(20);
                   // btCommunicator.rotate(30)
                   // ;
                    //Høyre +
                    //venstre -

                    btCommunicator.rotate(30);

                } else {
                    //mNXTTalker.motors(r, l, mRegulateSpeed, mSynchronizeMotors);
                    btCommunicator.move(20);
                }
            } else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
                //mNXTTalker.motors((byte) 0, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
            }
            return true;
        }
    }

    private void setupUI() {
       /* if (mControlsMode == MODE_BUTTONS) {
            setContentView(R.layout.main2);

            Button buttonUp = (Button) findViewById(R.id.button_up);
            buttonUp.setOnTouchListener(new DirectionButtonOnTouchListener(1, 1));
            Button buttonLeft = (Button) findViewById(R.id.button_left);
            buttonLeft.setOnTouchListener(new DirectionButtonOnTouchListener(-0.6, 0.6));
            Button buttonDown = (Button) findViewById(R.id.button_down);
            buttonDown.setOnTouchListener(new DirectionButtonOnTouchListener(-1, -1));
            Button buttonRight = (Button) findViewById(R.id.button_right);
            buttonRight.setOnTouchListener(new DirectionButtonOnTouchListener(0.6, -0.6));
            Button buttonClaw = (Button) findViewById(R.id.button_claw);
            buttonClaw.setOnTouchListener( new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN) {
                        //mNXTTalker.motor((byte) 1, (byte) -20, mRegulateSpeed, mSynchronizeMotors);
                        btCommunicator.openClaw();
                        return true;
                    } /*else if (event.getAction() == MotionEvent.ACTION_UP) {
                       // mNXTTalker.motor((byte) 1, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
                        btCommunicator.openClaw();
                        return true;
                    } else {
                        return false;
                    }
                }
            });
        }  */

        mStateDisplay = (TextView) findViewById(R.id.state_display);

        mConnectButton = (Button) findViewById(R.id.connect_button);
        mConnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!NO_BT) {
                    findBrick();
                } else {
                    mState = NXTTalker.STATE_CONNECTED;
                    //displayState();
                }
            }
        });

        mDisconnectButton = (Button) findViewById(R.id.disconnect_button);
        mDisconnectButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //mNXTTalker.stop();
            }
        });

        //displayState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Log.i("NXT", "NXTRemoteControl.onStart()");
        if (!NO_BT) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                if (mSavedState == NXTTalker.STATE_CONNECTED) {
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
                    //mNXTTalker.connect(device);
                    btCommunicator.connect(device);
                } else {
                    if (mNewLaunch) {
                        mNewLaunch = false;
                        findBrick();
                    }
                }
            }
        }
    }

    private void findBrick() {
        Intent intent = new Intent(this, ChooseDeviceActivity.class);
        startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    findBrick();
                } else {
                    Toast.makeText(this, "Bluetooth not enabled, exiting.", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {
                    String address = data.getExtras().getString(ChooseDeviceActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    //Toast.makeText(this, address, Toast.LENGTH_LONG).show();
                    mDeviceAddress = address;
                    btCommunicator.connect(device);
                    //mNXTTalker.connect(device);
                }
                break;
            case REQUEST_SETTINGS:
                //XXX?
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mState == NXTTalker.STATE_CONNECTED) {
            outState.putString("device_address", mDeviceAddress);
        }
        //outState.putBoolean("reverse", mReverse);
        outState.putInt("power", mPower);
        outState.putInt("controls_mode", mControlsMode);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //Log.i("NXT", "NXTRemoteControl.onConfigurationChanged()");
        setupUI();
    }

   /* private void displayState() {
        String stateText = null;
        int color = 0;
        switch (mState){
            case NXTTalker.STATE_NONE:
                stateText = "Not connected";
                color = 0xffff0000;
                mConnectButton.setVisibility(View.VISIBLE);
                mDisconnectButton.setVisibility(View.GONE);
                setProgressBarIndeterminateVisibility(false);
                if (mWakeLock.isHeld()) {
                    mWakeLock.release();
                }
                break;
            case NXTTalker.STATE_CONNECTING:
                stateText = "Connecting...";
                color = 0xffffff00;
                mConnectButton.setVisibility(View.GONE);
                mDisconnectButton.setVisibility(View.GONE);
                setProgressBarIndeterminateVisibility(true);
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
                break;
            case NXTTalker.STATE_CONNECTED:
                stateText = "Connected";
                color = 0xff00ff00;
                mConnectButton.setVisibility(View.GONE);
                mDisconnectButton.setVisibility(View.VISIBLE);
                setProgressBarIndeterminateVisibility(false);
                if (!mWakeLock.isHeld()) {
                    mWakeLock.acquire();
                }
                break;
        }
        mStateDisplay.setText(stateText);
        mStateDisplay.setTextColor(color);
    }*/

    @Override
    public void onResume() {
        super.onResume();
        imgP.resume();
    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                case MESSAGE_STATE_CHANGE:
                    mState = msg.arg1;
                    //displayState();
                    break;
            }
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        //Log.i("NXT", "NXTRemoteControl.onStop()");
        mSavedState = mState;
       // mNXTTalker.stop();
        if (mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    private void readPreferences(SharedPreferences prefs, String key) {
        if (key == null) {
            mReverse = prefs.getBoolean("PREF_SWAP_FWDREV", false);
            mReverseLR = prefs.getBoolean("PREF_SWAP_LEFTRIGHT", false);
            mRegulateSpeed = prefs.getBoolean("PREF_REG_SPEED", false);
            mSynchronizeMotors = prefs.getBoolean("PREF_REG_SYNC", false);
            if (!mRegulateSpeed) {
                mSynchronizeMotors = false;
            }
        } else if (key.equals("PREF_SWAP_FWDREV")) {
            mReverse = prefs.getBoolean("PREF_SWAP_FWDREV", false);
        } else if (key.equals("PREF_SWAP_LEFTRIGHT")) {
            mReverseLR = prefs.getBoolean("PREF_SWAP_LEFTRIGHT", false);
        } else if (key.equals("PREF_REG_SPEED")) {
            mRegulateSpeed = prefs.getBoolean("PREF_REG_SPEED", false);
            if (!mRegulateSpeed) {
                mSynchronizeMotors = false;
            }
        } else if (key.equals("PREF_REG_SYNC")) {
            mSynchronizeMotors = prefs.getBoolean("PREF_REG_SYNC", false);
        }
    }

    @Override
    public void OnBallDetect(ArrayList<Ball> balls) {
        Log.i(ImageProcessing.TAG, "FOUND SOME BALLS:" + balls.size());
    }

    @Override
    public void OnBoxDetect(ArrayList<CollectionBox> boxes) {

    }
}
