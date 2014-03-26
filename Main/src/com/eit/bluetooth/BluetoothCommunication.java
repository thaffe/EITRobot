package com.eit.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

/**
 * Created by Fredrik on 26.02.14.
 */
public class BluetoothCommunication {
    private NXTTalker mNXTTalker;
    private int mPower = 45;
    private boolean closed = false;

    public BluetoothCommunication(NXTTalker nxtTalker) {
        this.mNXTTalker = nxtTalker;
    }

    public void connect(BluetoothDevice device) {
        mNXTTalker.connect(device);
    }


    public void openClaw() {
        if (closed) {
            closed = false;
            Log.i("CLAW", "CLAWING OPEN");
            this.stop();
            mNXTTalker.motor(1, (byte) 20, false, false);
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    public void closeClaw() {
        if (!closed) {
            Log.i("ROBOT", "CLAW CLOSE");
            this.stop();
            mNXTTalker.motor(1, (byte) -20, false, false);
            closed = true;
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isIdle() {
        return false;
    }

    public boolean isSensorPressed() {
        return false;
    }


    public void stop() {
        mNXTTalker.motors((byte) 0, (byte) 0, false, false);
    }

    public void setSpeed(float l, float r) {
        mNXTTalker.motors((byte) l, (byte) r, false, false);
    }

}
