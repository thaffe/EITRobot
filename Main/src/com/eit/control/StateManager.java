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

    private BehaviorState state;

    private boolean isBallTracking;
    private boolean isBoxTracking;

    public StateManager(BluetoothCommunication control, EyeProcessing eye,
                        RobotHumanInteraction humanInteraction) {
        this.robotCar = new RobotCar(control);
        this.eye = eye;
        this.eye.addListener(this);
        this.humanInteraction = humanInteraction;
        this.state = BehaviorState.LOCATE_BALL;
    }

    public void step() {
        Log.i(TAG, "STATE" + state.name());

        robotCar.step();

        if (!isBallTracking) {
            startBallTracking();
        }

        if (!isBoxTracking) {
            startBoxTracking();
        }

        switch (state) {
            case LOCATE_BALL:
                locateBall();
                break;
            case REACH_BALL:
                reachBall();
                break;
            case PICK_UP:
                pickupBall();
                break;
            case LOCATE_BOX:
                locateBox();
                break;
            case DOCK:
                reachBox();
                break;
            case RELEASE_BALL:
                releaseBall();
                break;
            case UNDOCK:
                undock();
                break;
        }

        step();
    }

    private void undock() {
        robotCar.setStepMode(true);
        robotCar.forward(-1);
        robotCar.rotate(1);
        robotCar.setStepMode(false);

        state = BehaviorState.LOCATE_BALL;

    }

    private void releaseBall() {
        robotCar.openClaw();
        state = BehaviorState.LOCATE_BALL;
    }

    private void pickupBall() {
        robotCar.closeClaw();
        this.state = BehaviorState.RELEASE_BALL;
    }

    private void locateBall() {
        if (ball == null) {
            robotCar.setSearchMode(true);
        } else {
            robotCar.setSearchMode(false);
            speak("Ball found");
            state = BehaviorState.REACH_BALL;
        }
    }

    private void reachBall() {
        if (ball == null) {
            state = BehaviorState.LOCATE_BALL;
        } else if (moveTowards(ball)) {
            state = BehaviorState.PICK_UP;
            Log.i(TAG, "IM HERE");
        }
    }

    private void locateBox() {
        if (box == null) {
            robotCar.setSearchMode(true);
        } else {
            speak("Box found");
            robotCar.setSearchMode(false);
            state = BehaviorState.DOCK;
        }
    }

    private void reachBox() {
        if (box == null) {
            state = BehaviorState.LOCATE_BOX;
        } else if (robotCar.isSensorPressed()) {
            state = BehaviorState.RELEASE_BALL;
            Log.i(TAG, "BALL RELEASE");
        } else {
            moveTowards(box);
        }
    }

    private void speak(String text) {
        humanInteraction.speak(text);
    }

    public static double getSigmoid(double x) {
        return 2 / (1 + Math.exp(-(8 * x + 3))) - 1;
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
