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

    private StateManager stateManager;
    private final RobotHumanInteraction humanInteraction;
    private final EyeProcessing eyeProcessing;
    private final BluetoothCommunication control;

    public RobotController(EyeProcessing eyeProcessing, BluetoothCommunication control, Context context) {

        this.eyeProcessing = eyeProcessing;
        this.control = control;
        AudioManager audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        SensorManager sensorManager = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);

        this.humanInteraction = new RobotHumanInteraction(context, audioManager, sensorManager);
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.run();
    }

    @Override
    public void run() {
        this.stateManager = new StateManager(control, eyeProcessing, humanInteraction);
        while (true) {
            this.stateManager.step();
        }
    }
}
