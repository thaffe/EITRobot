package com.eit.control;

import com.eit.bluetooth.BluetoothCommunication;
import com.eit.image.Ball;
import com.eit.image.CollectionBox;

import java.util.ArrayList;
import java.util.Random;

public class StateManager {
    private static final boolean SKIP_BOX = true;
    private static final double ROTATION_STEP = -1.5;
    private static final int MOVE_STEP = 2;
    private static final int UNDOCK_BACK_STEP = 20;

    private final BluetoothCommunication control;
    private final EyeCommunication eye;
    private final RobotHumanInteraction humanInteraction;

    private BehaviorState state;
    private int ballTypeContained;
    private boolean init = true;
    private boolean naiveBoxRun = false;
    private Random rnd = new Random(7373);

    private int randomRotationStepsTotal;
    private int randomRotationStepsDone;

    public StateManager(BluetoothCommunication control, EyeCommunication eye,
                        RobotHumanInteraction humanInteraction) {
        this.control = control;
        this.eye = eye;
        this.humanInteraction = humanInteraction;
        this.state = BehaviorState.LOCATE_BALL;
    }

    /**
     * Gives robot new instructions if it is ready for next task
     */
    public void step() {
        if (control.isIdle()) {
            stepNext();
        }
    }

    private void stepNext() {
        if (state == BehaviorState.LOCATE_BALL) {
            locateBall();
        } else if (state == BehaviorState.REACH_BALL) {
            reachBall();
        } else if (state == BehaviorState.PICK_UP) {
            control.closeClaw();
            if (SKIP_BOX) {
                state = BehaviorState.LOCATE_BALL;
                control.openClaw();
                control.move(-MOVE_STEP*2);
                control.rotate(2);
            } else {
                state = BehaviorState.LOCATE_BOX;
            }
        } else if (state == BehaviorState.LOCATE_BOX) {
            locateBox();
        } else if (state == BehaviorState.DOCK) {
            reachBox();
        } else if (state == BehaviorState.RELEASE_BALL) {
            control.openClaw();
            state = BehaviorState.UNDOCK;
        } else if (state == BehaviorState.UNDOCK) {
            control.move(-UNDOCK_BACK_STEP);
            state = BehaviorState.LOCATE_BALL;
            init = true;
        }
    }

    public StateRequirement requires() {
        if (state == BehaviorState.LOCATE_BALL || state == BehaviorState.REACH_BALL) {
            return StateRequirement.BALL;
        } else if (state == BehaviorState.LOCATE_BOX || state == BehaviorState.DOCK) {
            return StateRequirement.COLLECTION_BOX;
        } else {
            return StateRequirement.NONE;
        }
    }

    private void locateBall() {
        if (init)  {
            speak("Locating balls");
            init = false;
            randomRotationStepsTotal = rnd.nextInt(5) + 2;
            randomRotationStepsDone = 0;
        }

        Ball ball = closestBallOrDefault();

        if (ball == null) {
            if (randomRotationStepsDone < randomRotationStepsTotal) {
                control.rotate(ROTATION_STEP);
                randomRotationStepsDone++;
            }
            else {
                // TODO: Walk randomly around, or use map
                control.move(MOVE_STEP);
                randomRotationStepsDone = 0;
            }

        }  else {
            speak("Ball found");
            state = BehaviorState.REACH_BALL;
        }
    }

    private void reachBall() {
        Ball ball = closestBallOrDefault();

        if (ball == null) {
            state = BehaviorState.LOCATE_BALL;
        } else if (ball.getHorizontalOffset() != 0) {
            control.rotate(ball.getHorizontalOffset());
        } else if (ball.getDistance() <= 0) {
            state = BehaviorState.PICK_UP;
            ballTypeContained = ball.getType();
        } else {
            control.move(MOVE_STEP);
        }
    }

    private void locateBox() {
        CollectionBox box = getBoxOrDefault(ballTypeContained);

        if (box == null) {
            control.rotate(ROTATION_STEP);
        } else {
            state = BehaviorState.DOCK;
        }
    }

    private void reachBox() {
        if (naiveBoxRun) {

            if (control.isSensorPressed()) {
                naiveBoxRun = false;
                state = BehaviorState.RELEASE_BALL;
            } else {
                control.move(MOVE_STEP);
            }

        } else {
            CollectionBox box = getBoxOrDefault(ballTypeContained);

            if (box == null) {
                state = BehaviorState.LOCATE_BOX;
            } else if (box.getHorizontalOffset() != 0) {
                control.rotate(box.getHorizontalOffset());
            } else {
                naiveBoxRun = true;
            }
        }
    }

    private CollectionBox getBoxOrDefault(int type) {
        ArrayList<CollectionBox> boxes = eye.locateBoxes();

        for (CollectionBox box : boxes) {
            if (box.getType() == type) {
                return box;
            }
        }
        return null;
    }

    private void speak(String text) {
        humanInteraction.speak(text);
    }

    /**
     * Scan area for balls.
     * @return Closest ball, or null if none found
     */
    private Ball closestBallOrDefault() {
        ArrayList<Ball> balls = eye.locateMyBalls();

        Ball closest = null;
        for (Ball ball : balls) {
            if (closest == null) {
                closest = ball;
            } else if (ball.getDistance() < closest.getDistance()) {
                closest = ball;
            }
        }

        return closest;
    }
}
