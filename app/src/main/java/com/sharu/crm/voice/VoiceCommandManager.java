package com.sharu.crm.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.Locale;

public class VoiceCommandManager {

    public interface VoiceCallback {
        void onCommandRecognized(String action, String payload);
        void onError(String message);
        void onListeningStatus(boolean isListening);
    }

    private final Context context;
    private final VoiceCallback callback;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    public VoiceCommandManager(Context context, VoiceCallback callback) {
        this.context = context;
        this.callback = callback;
        initRecognizer();
    }

    private void initRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.onError("Speech recognition not available on this device.");
            return;
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) { callback.onListeningStatus(true); }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() { callback.onListeningStatus(false); }

            @Override
            public void onError(int error) {
                callback.onListeningStatus(false);
                callback.onError("Speech error code: " + error);
            }

            @Override
            public void onResults(Bundle results) {
                callback.onListeningStatus(false);
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    processVoiceInput(matches.get(0).toLowerCase());
                }
            }

            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    public void startListening() {
        if (speechRecognizer != null) {
            speechRecognizer.startListening(recognizerIntent);
        }
    }

    public void stopListening() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
        }
    }

    public void destroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    private void processVoiceInput(String text) {
        // Voice command routing
        if (text.contains("call") || text.contains("dial")) {
            callback.onCommandRecognized("ACTION_CALL", text);
        } else if (text.contains("whatsapp") || text.contains("message") || text.contains("quote")) {
            callback.onCommandRecognized("ACTION_WHATSAPP", text);
        } else if (text.contains("hot") || text.contains("warm") || text.contains("cold") || text.contains("fake")) {
            callback.onCommandRecognized("ACTION_STATUS_UPDATE", text);
        } else if (text.contains("emi") || text.contains("loan") || text.contains("calculator")) {
            callback.onCommandRecognized("ACTION_CALC_EMI", text);
        } else {
            callback.onCommandRecognized("ACTION_SEARCH", text);
        }
    }
}
