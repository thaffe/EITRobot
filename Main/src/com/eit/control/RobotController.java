package com.eit.control;

import android.content.Context;
import android.hardware.SensorManager;
import android.media.AudioManager;

import java.util.ArrayList;

import com.eit.bluetooth.BluetoothCommunication;
import com.eit.image.Ball;
import com.eit.image.CollectionBox;
import com.eit.image.EyeProcessing;
import com.eit.image.ImageProcessListener;

/**
 * Created by Lysaker on 05.03.14.
 */
public class RobotController implements Runnable {

    private final StateManager stateManager;
    private final RobotHumanInteraction humanInteraction;


    public RobotController(EyeProcessing eyeProcessing, BluetoothCommunication control, Context context) {

        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

        this.humanInteraction = new RobotHumanInteraction(context, audioManager, sensorManager);
        this.stateManager = new StateManager(control, eyeProcessing, humanInteraction);
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.run();
    }

    @Override
    public void run() {
        while (true) {
            this.stateManager.step();
        }
    }
}
