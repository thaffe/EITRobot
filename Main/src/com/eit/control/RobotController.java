package com.eit.control;

import android.content.Context;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.speech.tts.TextToSpeech;
import android.content.Intent;
import android.hardware.*;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import java.util.ArrayList;
import android.app.Activity;
import com.eit.bluetooth.BluetoothCommunication;
import com.eit.image.Ball;
import com.eit.image.CollectionBox;
import com.eit.image.EyeProcessing;
import com.eit.image.ImageProcessListener;

import java.util.Locale;
import java.util.Random;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Lysaker on 05.03.14.
 */
public class RobotController implements ImageProcessListener {
    private final StateManager stateManager;
    private final EyeCommunicationManager eye;
    private final EyeProcessing eyeProcessing;
    private final RobotHumanInteraction humanInteraction;

    private boolean stepCalled;

    public RobotController(EyeProcessing eyeProcessing, BluetoothCommunication control, Context context) {

        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

        this.humanInteraction = new RobotHumanInteraction(context, audioManager, sensorManager);
        this.eye = new EyeCommunicationManager();
        this.stateManager = new StateManager(control, eye, humanInteraction);
        this.eyeProcessing = eyeProcessing;
        this.eyeProcessing.addListener(this);
    }

    public void step() {
        if (stepCalled) {
            throw new IllegalStateException("Step already called");
        } else {
            stepCalled = true;

            StateRequirement requires = stateManager.requires();

            if (requires == StateRequirement.BALL) {
                eyeProcessing.startBallDetection();
            } else if (requires == StateRequirement.COLLECTION_BOX) {
                eyeProcessing.startBoxDetection();
            } else {
                stateManager.step();
                stepCalled = false;
            }
        }
    }

    public boolean stepRequiresCall() {
        return !stepCalled;
    }

    @Override
    public void OnBallDetect(ArrayList<Ball> balls) {
        eye.setFoundBalls(balls);
        stateManager.step();
        stepCalled = false;
    }

    @Override
    public void OnBoxDetect(ArrayList<CollectionBox> boxes) {
        eye.setFoundBoxes(boxes);
        stateManager.step();
        stepCalled = false;
    }
}
