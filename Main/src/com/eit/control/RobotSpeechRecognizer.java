package com.eit.control;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;
import java.util.Random;

public class RobotSpeechRecognizer implements RecognitionListener {
    private final SpeechRecognizer speechRecognizer;
    private final Intent recognitionIntent;
    private final Random random;

    private Runnable runnable;
    private ArrayList<String> items;

    public RobotSpeechRecognizer(final Context context, Runnable runnable) {
        this.runnable = runnable;
        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        this.speechRecognizer.setRecognitionListener(this);
        this.random = new Random(575373);

        this.recognitionIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        this.recognitionIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
    }

    public ArrayList<String> getItems() {
        return items;
    }

   public void startRecognition() {
        speechRecognizer.startListening(recognitionIntent);
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        System.out.println("out- Ready for speech");
    }

    @Override
    public void onBeginningOfSpeech() {
        System.out.println("out- You started speaking");
    }

    @Override
    public void onRmsChanged(float rmsdB) {

    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {
        System.out.println("out- End of speech");
    }

    @Override
    public void onError(int error) {
        System.out.println("out- ERROR: " + error);
        if (error == 7) {
            startRecognition();
        }
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> voiceResults = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (voiceResults != null && voiceResults.size() > 0) {
            items = voiceResults;
            runnable.run();
        } else {
            startRecognition();
        }
    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {

    }
}
