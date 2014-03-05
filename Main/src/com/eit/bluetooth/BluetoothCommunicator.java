package com.eit.bluetooth;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Fredrik on 26.02.14.
 */
public class BluetoothCommunicator implements BluetoothCommunication {
    private NXTTalker mNXTTalker;
    private int mPower = 80;

    public BluetoothCommunicator(NXTTalker nxtTalker) {
        this.mNXTTalker = nxtTalker;
    }

    public void connect(BluetoothDevice device) {
        mNXTTalker.connect(device);
    }

    @Override
    public void move(int speed) {
        System.out.println("MOVING");
        mNXTTalker.motors((byte)speed,(byte) speed, false, false);
    }

    @Override
    public void rotate(double degrees) {
        byte l;
        byte r;
        byte power = (byte) mPower;
            l = (byte) (power*(-degrees));
            r = (byte) (power*degrees);
        mNXTTalker.motors(l,r,false,false);
    }

    @Override
    public void openClaw() {
        mNXTTalker.motor((byte) 1, (byte) 20, false, false);
    }

    @Override
    public void closeClaw() {
        mNXTTalker.motor((byte) 1, (byte) -20, false, false);
    }

    @Override
    public boolean isIdle() {
        return false;
    }

    @Override
    public boolean isSensorPressed() {
        return false;
    }
}
