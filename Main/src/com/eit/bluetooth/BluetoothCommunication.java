package com.eit.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.util.Log;
import com.eit.control.StateManager;

/**
 * Created by Fredrik on 26.02.14.
 */
public class BluetoothCommunication {
    private NXTTalker mNXTTalker;
    private int mPower = 45;

    public BluetoothCommunication(NXTTalker nxtTalker) {
        this.mNXTTalker = nxtTalker;
    }

    public void connect(BluetoothDevice device) {
        mNXTTalker.connect(device);
    }


    public void openClaw() {
        if (false) {
            Log.i("CLAW", "CLAWING OPEN");
            mNXTTalker.motor(1, (byte) 20, false, false);
        }
    }

    boolean closed = false;

    public void closeClaw() {
        if (!closed) {
            Log.i("ROBOT", "CLAW CLOSE");
            this.stop();
            mNXTTalker.motor(1, (byte) -20, false, false);
            closed = true;
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

    public void setSpeed(int l, int r) {
        mNXTTalker.motors((byte) l, (byte) r, false, false);
    }

}
