package com.eit.control;

import android.content.Context;
import android.hardware.*;
import android.media.AudioManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Lysaker on 05.03.14.
 */
public class RobotHumanInteraction implements Runnable {
    private static final boolean LIGHT_NOTIFICATION = false;
    private static final float FLASH_THRESHOLD = 30;
    private static final boolean USE_LIGHT = false;
    private static final boolean TALK = false;

    private TextToSpeech speech;
    private boolean speechInitiated = false;
    private boolean torchMode = false;
    private Camera cam;
    private RobotSpeechRecognizer recognizer;
    private AudioManager audioManager;

    public RobotHumanInteraction(Context context, AudioManager audioManager, SensorManager sensorManager) {
        this.recognizer = new RobotSpeechRecognizer(context, this);
        this.audioManager = audioManager;

        cam = Camera.open();

        SensorManager mySensorManager = sensorManager;
        Sensor LightSensor = mySensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        if(LightSensor != null){
            System.out.println("out- Sensor.TYPE_LIGHT Available");
            mySensorManager.registerListener(
                    LightSensorListener,
                    LightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
        }else{
            System.out.println("out- Sensor.TYPE_LIGHT NOT Available");
        }

        this.speech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
            public void onInit(int status) {

                System.out.println("out- start init");
                if (status == TextToSpeech.SUCCESS) {
                    System.out.println("out- speak!");
                    speech.setLanguage(Locale.US);
                    speak("Where the hell am i?");
                    speechInitiated = true;
                    recognizer.startRecognition();
                } else {
                    System.out.println("out- failed TTS init");
                    throw new IllegalStateException("TTS not available");
                }
            }
        });
        this.speech.setPitch(0.9f);
        this.speech.setSpeechRate(0.9f);
    }

    private void setMode(boolean torch) {

        if (torchMode != torch && USE_LIGHT) {
            torchMode = torch;
            Camera.Parameters p = cam.getParameters();
            p.setFlashMode((torch)? Camera.Parameters.FLASH_MODE_TORCH : Camera.Parameters.FLASH_MODE_OFF);
            cam.setParameters(p);
            cam.startPreview();

            if (speechInitiated && LIGHT_NOTIFICATION) {
                if (torch) {
                    speak("Light it up!");
                } else {
                    speak("Don\'t need lights anymore");
                }
            }

        }
    }

    private final SensorEventListener LightSensorListener
            = new SensorEventListener(){

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor.getType() == Sensor.TYPE_LIGHT){
                float value = event.values[0];
                setMode(value < FLASH_THRESHOLD);

                //System.out.println("out- LIGHT: " + value);
            }
        }

    };

    private void onRecognition(ArrayList<String> items) {
        if (anyContains(items, "Robot")) {
            if (anyContains(items, "GO")) {
                speak("I will simply walk into mordor!");
            } else if (anyContains(items, "FIND")) {
                speak("But i have no eyes, what am i supposed to do? smell them?");
            } else if (anyContains(items, "THINK")) {
                speak("My existence is pointless");
            } else if (anyContains(items, "STOP")) {
                speak("You don\'t have any power here");
            }
        }
        recognizer.startRecognition();
    }

    private static boolean anyContains(ArrayList<String> items, String word) {
        for (String s : items) {
            if (s.toLowerCase().contains(word.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public void speak(String text) {
        if (TALK) {
            audioManager.setStreamMute(AudioManager.VIBRATE_TYPE_NOTIFICATION, false);
            speech.speak(text, TextToSpeech.QUEUE_ADD, null);
            audioManager.setStreamMute(AudioManager.VIBRATE_TYPE_NOTIFICATION, true);
        }

    }

    @Override
    public void run() {
        ArrayList<String> items = recognizer.getItems();
        onRecognition(items);
    }
}
