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
    private static final long SEARCH_INSTRUCTION_INTERVAL = 50;

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
        this.searchMode = true;
    }

    public void step() {
        if (searchMode) {
            long duration = System.currentTimeMillis() - this.searchModeStart;
            double durationSeconds = duration / 1000;
            double radius = Math.min(durationSeconds * SEARCH_ACCELERATION, 1);

            if (this.searchModeLastCall < this.searchModeStart - SEARCH_INSTRUCTION_INTERVAL) {
                move(radius, -radius * 0.5);
                this.searchModeLastCall = System.currentTimeMillis();
            }
        } else {

        }
    }

    public void setStepMode(boolean enabled) {
        this.stepMode = enabled;
    }

    public void moveTowards(VisualObject object) {
        double distance = object.getDistance();
        double offset = object.getHorizontalOffset();
        // double left = Math.min(2.0 * offset + 1,1);
        // double right = Math.min(-2.0 * offset +1,1);
        double x = (offset + 2 * (1 - distance)) / 3;
        double left = StateManager.getSigmoid(x);
        double right = StateManager.getSigmoid(-x);
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

    public void rotate(double power) {
        move(-power, power);
    }

    public void forward(double power) {
        move(power, power);
    }

    public void move(double leftPower, double rightPower) {
        this.leftPower = leftPower;
        this.rightPower = rightPower;
        control.setSpeed((int) (leftPower * MAX_SPEED), (int) (rightPower * MAX_SPEED));

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
