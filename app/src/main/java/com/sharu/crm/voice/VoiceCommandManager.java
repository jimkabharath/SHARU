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

/**
 * Handles offline/local speech recognition, parses intent hierarchies,
 * and extracts natural language parameters (loan amounts, tenure, interest).
 */
public class VoiceCommandManager {

    public interface VoiceCallback {
        void onCommandRecognized(String action, String payload);
        void onEmiCalculated(double principal, double rate, int months, double emi);
        void onError(String message);
        void onListeningStatus(boolean isListening);
    }

    // Default financial calculation fallbacks
    private static final double DEFAULT_INTEREST_RATE = 8.5;
    private static final int DEFAULT_TENURE_MONTHS = 240; // 20 years

    // Precompiled Regular Expressions for Performance
    private static final Pattern PATTERN_RATE = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(%|percent|pc)");
    private static final Pattern PATTERN_MONTHS = Pattern.compile("(\\d+)\\s*(months?|month|m)");
    private static final Pattern PATTERN_YEARS = Pattern.compile("(\\d+)\\s*(years?|yrs?|yr|y)");
    private static final Pattern PATTERN_CRORE = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(crores?|crore|cr)");
    private static final Pattern PATTERN_LAKH = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(lakhs?|lakh|lac|l)");
    private static final Pattern PATTERN_THOUSAND = Pattern.compile("(\\d+(\\.\\d+)?)\\s*(thousand|k)");
    private static final Pattern PATTERN_RAW_NUMBER = Pattern.compile("\\b(\\d{5,10})\\b");

    private final Context context;
    private final VoiceCallback callback;
    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;

    public VoiceCommandManager(Context context, VoiceCallback callback) {
        this.context = context;
        this.callback = callback;
        initRecognizer();
    }

    /**
     * Initializes the Android SpeechRecognizer service with free-form language model.
     */
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
                    processVoiceInput(matches.get(0).toLowerCase(Locale.ROOT).trim());
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

    /**
     * Evaluates spoken transcripts sequentially to avoid overlapping phrase collisions.
     * Order: System Backup -> Telephony -> Messaging -> Calculations -> Pipeline Status -> Search
     */
    private void processVoiceInput(String text) {
        // 1. High-priority system exports
        if (text.contains("backup") || text.contains("daily report") || text.contains("send report")) {
            callback.onCommandRecognized("ACTION_BACKUP_WHATSAPP", text);
            return;
        }

        // 2. Direct telephony triggers
        if (text.startsWith("call") || text.startsWith("dial") || text.contains("make a call") || text.contains("phone")) {
            callback.onCommandRecognized("ACTION_CALL", text);
            return;
        }

        // 3. Messaging triggers
        if (text.startsWith("whatsapp") || text.startsWith("message") || text.contains("send whatsapp") || text.contains("share quote")) {
            callback.onCommandRecognized("ACTION_WHATSAPP", text);
            return;
        }

        // 4. Numerical Loan & EMI calculation
        if (text.contains("emi") || text.contains("loan") || text.contains("interest") || text.contains("calculate")) {
            extractAndComputeEmi(text);
            return;
        }

        // 5. Priority lead tag updates
        if (text.contains("mark hot") || text.contains("mark warm") || text.contains("mark cold") || text.contains("mark fake")
                || text.equals("hot") || text.equals("warm") || text.equals("cold") || text.equals("fake")) {
            callback.onCommandRecognized("ACTION_STATUS_UPDATE", text);
            return;
        }

        // 6. Generic filter / Lead search fallback
        callback.onCommandRecognized("ACTION_SEARCH", text);
    }

    /**
     * Coordinates entity extraction for loan calculation.
     */
    private void extractAndComputeEmi(String text) {
        double rate = extractRate(text);
        int months = extractTenureMonths(text);
        double principal = extractPrincipal(text);

        if (principal > 0) {
            double emi = com.sharu.crm.calc.LoanCalculator.calculateEMI(principal, rate, months);
            callback.onEmiCalculated(principal, rate, months, emi);
        } else {
            // Fall back to opening calculator if no numeric principal could be parsed
            callback.onCommandRecognized("ACTION_CALC_EMI", text);
        }
    }

    /**
     * Parses interest rate percentage (e.g., "8.5%", "9 percent").
     */
    private double extractRate(String text) {
        Matcher matcher = PATTERN_RATE.matcher(text);
        if (matcher.find()) {
            try {
                return Double.parseDouble(matcher.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return DEFAULT_INTEREST_RATE;
    }

    /**
     * Parses tenure either from explicit months ("180 months") or converts years ("15 years" -> 180).
     */
    private int extractTenureMonths(String text) {
        Matcher monthMatcher = PATTERN_MONTHS.matcher(text);
        if (monthMatcher.find()) {
            try {
                return Integer.parseInt(monthMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        Matcher yearMatcher = PATTERN_YEARS.matcher(text);
        if (yearMatcher.find()) {
            try {
                return Integer.parseInt(yearMatcher.group(1)) * 12;
            } catch (NumberFormatException ignored) {}
        }

        return DEFAULT_TENURE_MONTHS;
    }

    /**
     * Parses numerical principal handling Indian denominations (Cr, Lakh, K) or raw digits.
     */
    private double extractPrincipal(String text) {
        Matcher crMatcher = PATTERN_CRORE.matcher(text);
        if (crMatcher.find()) {
            return parseMultiplier(crMatcher.group(1), 10_000_000.0);
        }

        Matcher lakhMatcher = PATTERN_LAKH.matcher(text);
        if (lakhMatcher.find()) {
            return parseMultiplier(lakhMatcher.group(1), 100_000.0);
        }

        Matcher kMatcher = PATTERN_THOUSAND.matcher(text);
        if (kMatcher.find()) {
            return parseMultiplier(kMatcher.group(1), 1_000.0);
        }

        Matcher rawMatcher = PATTERN_RAW_NUMBER.matcher(text);
        if (rawMatcher.find()) {
            try {
                return Double.parseDouble(rawMatcher.group(1));
            } catch (NumberFormatException ignored) {}
        }

        return 0.0;
    }

    private double parseMultiplier(String valueStr, double multiplier) {
        try {
            return Double.parseDouble(valueStr) * multiplier;
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
