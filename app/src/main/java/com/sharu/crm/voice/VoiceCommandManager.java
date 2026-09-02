package com.sharu.crm.voice;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VoiceCommandManager {

    public interface VoiceCallback {
        void onCommandRecognized(String action, String payload);
        void onEmiCalculated(double principal, double rate, int months, double emi);
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
        if (text.contains("emi") || text.contains("loan") || text.contains("interest")) {
            extractAndComputeEmi(text);
        } else if (text.contains("backup") || text.contains("report") || text.contains("daily")) {
            callback.onCommandRecognized("ACTION_BACKUP_WHATSAPP", text);
        } else if (text.contains("call") || text.contains("dial")) {
            callback.onCommandRecognized("ACTION_CALL", text);
        } else if (text.contains("whatsapp") || text.contains("message") || text.contains("quote")) {
            callback.onCommandRecognized("ACTION_WHATSAPP", text);
        } else if (text.contains("hot") || text.contains("warm") || text.contains("cold") || text.contains("fake")) {
            callback.onCommandRecognized("ACTION_STATUS_UPDATE", text);
        } else {
            callback.onCommandRecognized("ACTION_SEARCH", text);
        }
    }

    private void extractAndComputeEmi(String text) {
        double principal = 0.0;
        double rate = 8.5; // Default fallback interest rate
        int months = 240;  // Default fallback tenure: 20 years

        // 1. Extract Rate (e.g., "8.5%", "9 percent", "at 10.25")
        Pattern ratePattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(%|percent|pc)");
        Matcher rateMatcher = ratePattern.matcher(text);
        if (rateMatcher.find()) {
            rate = Double.parseDouble(rateMatcher.group(1));
        }

        // 2. Extract Tenure (e.g., "240 months", "120 m", "5 years")
        Pattern monthPattern = Pattern.compile("(\\d+)\\s*(months?|month|m)");
        Matcher monthMatcher = monthPattern.matcher(text);
        if (monthMatcher.find()) {
            months = Integer.parseInt(monthMatcher.group(1));
        } else {
            Pattern yearPattern = Pattern.compile("(\\d+)\\s*(years?|yrs?|yr|y)");
            Matcher yearMatcher = yearPattern.matcher(text);
            if (yearMatcher.find()) {
                months = Integer.parseInt(yearMatcher.group(1)) * 12;
            }
        }

        // 3. Extract Principal (Crores, Lakhs, Thousands, or Raw numbers)
        Pattern crPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(crores?|crore|cr)");
        Matcher crMatcher = crPattern.matcher(text);
        if (crMatcher.find()) {
            principal = Double.parseDouble(crMatcher.group(1)) * 10000000;
        } else {
            Pattern lakhPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(lakhs?|lakh|lac|l)");
            Matcher lakhMatcher = lakhPattern.matcher(text);
            if (lakhMatcher.find()) {
                principal = Double.parseDouble(lakhMatcher.group(1)) * 100000;
            } else {
                Pattern kPattern = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(thousand|k)");
                Matcher kMatcher = kPattern.matcher(text);
                if (kMatcher.find()) {
                    principal = Double.parseDouble(kMatcher.group(1)) * 1000;
                } else {
                    Pattern rawNumPattern = Pattern.compile("\\b(\\d{5,10})\\b");
                    Matcher rawMatcher = rawNumPattern.matcher(text);
                    if (rawMatcher.find()) {
                        principal = Double.parseDouble(rawMatcher.group(1));
                    }
                }
            }
        }

        if (principal > 0) {
            double emi = com.sharu.crm.calc.LoanCalculator.calculateEMI(principal, rate, months);
            callback.onEmiCalculated(principal, rate, months, emi);
        } else {
            callback.onCommandRecognized("ACTION_CALC_EMI", text);
        }
    }
}
