package com.eit.control;

import android.util.Log;
import com.eit.bluetooth.BluetoothCommunication;
import com.eit.image.*;

import java.util.ArrayList;

public class StateManager implements ImageProcessListener {
    public final static int MAX_SPEED = 100;
    private static final boolean SKIP_BOX = true;
    private static final double ROTATION_STEP = -1.5;
    private static final int UNDOCK_BACK_STEP = 800;
    private static final int SPIN_STEP = 500;


    private boolean positionChanged = true;
    private EyeProcessing eye;
    private VisualObject ball, box;
    private double radius;

    public final BluetoothCommunication control;
    private final RobotHumanInteraction humanInteraction;

    public BehaviorState state;
    private boolean init = true;
    private boolean naiveBoxRun = false;
    private static String TAG = "ROBOT";

    public StateManager(BluetoothCommunication control, EyeProcessing eye,
                        RobotHumanInteraction humanInteraction) {
        this.control = control;
        this.eye = eye;
        this.eye.addListener(this);
        this.humanInteraction = humanInteraction;
        this.state = BehaviorState.LOCATE_BALL;
    }

    public void step() {
        Log.i(TAG, "ROBOT: " + state.name());
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
    }

    private void undock() {
        move(-1,-1,UNDOCK_BACK_STEP);
        move(-1,1, 500);
        state = BehaviorState.LOCATE_BALL;
        init = true;
    }

    private void releaseBall() {
        control.openClaw();
        state = BehaviorState.UNDOCK;
    }

    private void pickupBall() {
        control.closeClaw();
        this.state = BehaviorState.LOCATE_BOX;
    }

    private void locateBall() {
        if (init) {
            speak("Locating balls");
            init = false;
            radius = 0;
        }

        if (positionChanged) {
            startBallTracking();
        } else {
            if (ball == null) {
                searchStep();
            } else {
                speak("Ball found");
                state = BehaviorState.REACH_BALL;
            }
            step();
        }
    }

    private void reachBall() {
        if (positionChanged) {
            startBallTracking();
        } else {
            if (ball == null) {
                state = BehaviorState.LOCATE_BALL;
                radius = 0;
            } else if (moveTowards(ball)) {
                state = BehaviorState.PICK_UP;
                Log.i(TAG, "IM HERE");
            }
            step();
        }
    }

    private void locateBox() {
        if (init) {
            speak("Locating box");
            init = false;
            radius = 0;
        }

        if (positionChanged) {
            startBoxTracking();
        } else {
            if (box == null) {
                searchStep();
            } else {
                speak("Box found");
                state = BehaviorState.DOCK;
            }
            step();
        }
    }

    private void reachBox() {
        if (positionChanged) {
            startBoxTracking();
        } else {
            if (box == null) {
                state = BehaviorState.LOCATE_BOX;
                radius = 0;
                step();
            } else if (control.isSensorPressed()) {
                state = BehaviorState.RELEASE_BALL;
                Log.i(TAG, "BALL RELEASE");
                step();
            } else {
                moveTowards(box);
            }
        }
    }

    private void speak(String text) {
        humanInteraction.speak(text);
    }

    private void setSpeed(double percentL, double percentR) {
        control.setSpeed((int) (percentL * MAX_SPEED), (int) (percentR * MAX_SPEED));
        positionChanged = true;
    }

    private void move(double percentL, double percentR, int time) {
        setSpeed(percentL, percentR);
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        control.stop();
    }

    private void searchStep(){
        move(radius, 1, 500);
        radius += 0.05;
    }

    /**
     * Returns true if reached object
     *
     * @param object
     * @return
     */
    private boolean moveTowards(VisualObject object) {
        double distance = object.getDistance();
        if (distance <= 0) return true;

        double offset = object.getHorizontalOffset();
        double left = 2 * offset + 1;
        double right = -2 * offset + 1;
        move(left, right, (int) (200 * (distance + 0.5)));

        return false;
    }

    private void startBallTracking() {
        this.positionChanged = false;
        eye.startBallDetection();
    }

    private void startBoxTracking() {
        this.positionChanged = false;
        eye.startBoxDetection();
    }

    @Override
    public void OnBallDetect(ArrayList<Ball> balls) {
        ball = Ball.getClosest(balls);
        if (ball != null)
            Log.i(TAG, String.format("BALL Dist:%f Offset:%f", ball.getDistance(), ball.getHorizontalOffset()));
        else {
            Log.i(TAG, "BALL No ball found");
        }
        step();
    }

    @Override
    public void OnBoxDetect(ArrayList<CollectionBox> boxes) {
        box = CollectionBox.getClosest(boxes, ball.getType());
        step();
    }
}
