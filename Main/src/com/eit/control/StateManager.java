package com.eit.control;

import android.util.Log;
import com.eit.bluetooth.BluetoothCommunication;
import com.eit.image.*;

import java.util.ArrayList;

public class StateManager implements ImageProcessListener {
    public static final String TAG = "ROBOT";
    private static final int UNDOCK_BACK_STEP = 800;
    private static final int UNDOCK_ROTATION = 500;
    private static final int SEARCH_STEP_TIME = 500;

    private final RobotCar robotCar;
    private final RobotHumanInteraction humanInteraction;
    private final EyeProcessing eye;


    private VisualObject ball;
    private VisualObject box;

    // private BehaviorState state;

    private boolean isBallTracking;
    private boolean isBoxTracking;

    public StateManager(BluetoothCommunication control, EyeProcessing eye,
                        RobotHumanInteraction humanInteraction) {
        this.robotCar = new RobotCar(control);
        this.eye = eye;
        this.eye.addListener(this);
        this.humanInteraction = humanInteraction;
        // this.state = BehaviorState.LOCATE_BALL;
    }

    public void step() {
        //Log.i(TAG, "STATE" + state.name());

        robotCar.step();

        if (!isBallTracking) {
            startBallTracking();
        }

        if (!isBoxTracking) {
            startBoxTracking();
        }


        if (robotCar.isClawOpen()) {
            if (ball == null) {
                Log.i(TAG, "STATE" + "Locate ball");
                robotCar.setSearchMode(true);
            } else {
                robotCar.setSearchMode(false);

                if (moveTowards(ball)) {
                    Log.i(TAG, "STATE" + "Catch ball");
                    robotCar.closeClaw();
                } else {
                    Log.i(TAG, "STATE" + "Reach ball");
                }
            }
        } else {
            if (box == null) {
                Log.i(TAG, "STATE" + "Search box");
                robotCar.setSearchMode(true);
            } else {
                robotCar.setSearchMode(false);

                if (robotCar.isSensorPressed()) {
                    Log.i(TAG, "STATE" + "Sensor pressed!");
                    robotCar.openClaw();
                    robotCar.setStepMode(true);
                    robotCar.forward(-1);
                    robotCar.rotate(1);
                    robotCar.setStepMode(false);
                } else if (moveTowards(box)) {
                    Log.i(TAG, "STATE" + "???");
                } else {
                    Log.i(TAG, "STATE" + "Moving to box");
                }
            }
        }
    }

    private void speak(String text) {
        humanInteraction.speak(text);
    }

    public static float getSigmoid(float x) {
        return 2 / (1 + (float)Math.exp(-(8 * x + 3))) - 1;
    }

    /**
     * Returns true if reached object
     *
     * @param object
     * @return If object is close enough to grab
     */
    private boolean moveTowards(VisualObject object) {
        double distance = object.getDistance();
        if (distance <= 0) {
            return true;
        } else {
            robotCar.moveTowards(object);
            return false;
        }
    }

    public void startBallTracking() {
        //Log.i(TAG, "START Ball tracking");
        this.isBallTracking = true;
        eye.startBallDetection();
    }

    public void startBoxTracking() {
        //Log.i(TAG, "START Box tracking");

        if (ball != null) {
            this.isBoxTracking = true;
            eye.startBoxDetection(ball.getType());
        } else {

        }
    }

    @Override
    public void onBallDetect(VisualObject ball) {
        this.ball = ball;
        if (ball != null)
            Log.i(TAG, String.format("BALL Dist:%f Offset:%f", ball.getDistance(), ball.getHorizontalOffset()));
        else {
            Log.i(TAG, "BALL No ball found");
        }
        this.isBallTracking = false;
    }

    @Override
    public void onBoxDetect(VisualObject box) {
        this.box = box;
        this.isBoxTracking = false;
    }
}
