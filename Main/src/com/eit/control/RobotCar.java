package com.eit.control;

import android.util.Log;
import com.eit.bluetooth.BluetoothCommunication;
import com.eit.image.VisualObject;

import java.util.Calendar;
import java.util.Date;

/**
 * Created by Lysaker on 26.03.14.
 */
public class RobotCar {
    public final static int MAX_SPEED = -50;
    private static final int STEP_TIME = 500;
    private static final double SEARCH_ACCELERATION = 0.01;
    private static final long SEARCH_INSTRUCTION_INTERVAL = 100;

    private final BluetoothCommunication control;

    private double leftPower;
    private double rightPower;

    private boolean clawOpen;
    private boolean stepMode;

    private boolean searchMode;
    private long searchModeStart;
    private long searchModeLastCall;

    public RobotCar(BluetoothCommunication control) {
        this.control = control;
        this.clawOpen = true;
    }

    public void setSearchMode(boolean enabled) {
        if (!this.searchMode && enabled) {
            this.searchModeStart = System.currentTimeMillis();
            this.searchModeLastCall = 0;
        } else if (this.searchMode && !enabled) {
            this.stop();
        }
        this.searchMode = enabled;
    }

    public void step() {
        if (searchMode) {
            Log.i(StateManager.TAG, "SEARCHING");
            long duration = System.currentTimeMillis() - this.searchModeStart;
            double durationSeconds = (duration / 1000);
            float radius = (float)Math.min(durationSeconds * SEARCH_ACCELERATION  + 0.2, 1) * 3;

            if (this.searchModeLastCall < this.searchModeStart - SEARCH_INSTRUCTION_INTERVAL) {
                Log.i(StateManager.TAG, "UPDATE_MOVE");
                move(radius, -radius);
                this.searchModeLastCall = System.currentTimeMillis();
            }
        } else {

        }
        Log.i(StateManager.TAG, String.format("POWER: L:%f, R:%f", leftPower, rightPower));

    }

    public void setStepMode(boolean enabled) {
        this.stepMode = enabled;
    }

    public void moveTowards(VisualObject object) {
        double distance = object.getDistance();
        double offset = object.getHorizontalOffset();
        // double left = Math.min(2.0 * offset + 1,1);
        // double right = Math.min(-2.0 * offset +1,1);
        float x = (float)(offset + 2 * (1 - distance)) / 3;
        float left = StateManager.getSigmoid(x);
        float right = StateManager.getSigmoid(-x);
        Log.i(StateManager.TAG, String.format("SUPER: L:%f, R:%f of:%f", left, right, offset));

        //double deltaTurn = 2 - Math.abs(left - right);
        //double time = 200 * (2 * distance + deltaTurn / 3.0 + 0.2);

        move(left, right);
    }

    public boolean isSensorPressed() {
        return control.isSensorPressed();
    }

    public boolean isClawOpen() {
        return this.clawOpen;
    }

    public void closeClaw() {
        if (clawOpen) {
            this.clawOpen = false;
            control.closeClaw();
        } else {
            // error
        }
    }

    public void openClaw() {
        if (!clawOpen) {
            this.clawOpen = true;
            control.openClaw();
        } else {
            // error
        }
    }

    public void rotate(float power) {
        move(-power, power);
    }

    public void forward(float power) {
        move(power, power);
    }

    public void move(float leftPower, float rightPower) {
        this.leftPower = leftPower;
        this.rightPower = rightPower;
        control.setSpeed( (leftPower * MAX_SPEED),  (rightPower * MAX_SPEED));

        if (stepMode) {
            try {
                Thread.sleep(STEP_TIME);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            stop();
        }
    }

    public void stop() {
        this.leftPower = 0;
        this.rightPower = 0;
        this.control.stop();
    }
}
