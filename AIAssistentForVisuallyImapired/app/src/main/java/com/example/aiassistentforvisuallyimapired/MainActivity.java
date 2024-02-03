package com.example.aiassistentforvisuallyimapired;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private String input_text;
    private TextToSpeech textToSpeech;
    private static final int SPEECH_REQUEST_CODE = 123;
    private long lastTapTime = 0;
    private static final long DOUBLE_TAP_DELAY = 300; // Adjust this value as needed

    private String stringOutput;
    private SharedPreferences prefs;

    private TextView mainTextView;
    private String instructions = "\n\tInstructions:- \n\n 1. If you want to ask any questions, triple-tap the screen to enable the microphone and then ask me your question. " +
            "\n\n 2. To enable Object Recognition say \n'Open Object Recognition.'" +
            "\n\n 3. To enable Text Recognition say \n'Open Text Recognition.'" +
            "\n\n 4. To enable Face Recognition say \n'Open Face Recognition.'\n";
    private String display_text = instructions;

    private static final int PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if the required permissions are granted
        if (!checkPermissions()) {
            // Request permissions if not granted
            requestPermissions();
        }

        // Initialize prefs
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        // Initialize TextView
        mainTextView = findViewById(R.id.mainText);

        input_text = "Hello!! User, How can I assist you today?";

        // for speaking the required text
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                // Set language to default locale
                int langResult = textToSpeech.setLanguage(Locale.getDefault());

                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "Language not supported.", Toast.LENGTH_SHORT).show();
                } else {
                    // Check if instructions have been spoken before
                    boolean instructionsSpoken = prefs.getBoolean("instructionsSpoken", false);

                    if (!instructionsSpoken) {
                        // Display and speak introductory instructions
                        displayAndSpeakInstructions();

                        // Set the flag to true indicating that instructions have been spoken
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("instructionsSpoken", true);
                        editor.apply();
                    }
                }
            } else {
                Toast.makeText(this, "Text-to-Speech initialization failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean checkPermissions() {
        // Check if the required permissions are granted
        int recordAudioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        int cameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

        return recordAudioPermission == PackageManager.PERMISSION_GRANTED &&
                cameraPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissions() {
        // Request the required permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void displayAndSpeakInstructions() {
        textToSpeech.speak("Hello, My name is Tara, your personal AI assistant. " +
                        "If you want to ask any questions, triple-tap the screen to enable the microphone and then ask me your question.",
                TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void startSpeechRecognition() {
        // Create an intent to recognize speech
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your Input");

        // Start the speech recognition activity
        startActivityForResult(intent, SPEECH_REQUEST_CODE);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        long currentTime = System.currentTimeMillis();

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            if (currentTime - lastTapTime < DOUBLE_TAP_DELAY) {
                // Double-tap detected, initiate speech recognition
                startSpeechRecognition();
            }

            lastTapTime = currentTime;
        }

        return super.onTouchEvent(event);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
            // Retrieve the speech input results
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (results != null && !results.isEmpty()) {
                // Use the first result as the input_text
                input_text = results.get(0);

                // handle key word response
                handleKeywordResponses(input_text);

                // Speak the greeting text
                textToSpeech.speak(stringOutput, TextToSpeech.QUEUE_FLUSH, null, null);
            } else {
                return;
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Shutdown TextToSpeech when the activity is destroyed to free up resources
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }

    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private String handleDayQuery(String input) {

        LocalDateTime now = LocalDateTime.now();
        String trimmedInput = input.trim().toLowerCase();

        if (trimmedInput.contains("today")) {
            return "Today is " + now.getDayOfWeek().toString();
        } else if (trimmedInput.contains("day") && trimmedInput.contains("before yesterday")) {
            LocalDateTime dayBeforeYesterday = now.minusDays(2);
            return "The day before yesterday was " + dayBeforeYesterday.getDayOfWeek().toString();
        } else if (trimmedInput.contains("day") && trimmedInput.contains("after tomorrow")) {
            LocalDateTime dayAfterTomorrow = now.plusDays(2);
            return "The day after tomorrow will be " + dayAfterTomorrow.getDayOfWeek().toString();
        } else if (trimmedInput.contains("yesterday")) {
            LocalDateTime yesterday = now.minusDays(1);
            return "Yesterday was " + yesterday.getDayOfWeek().toString();
        } else if (trimmedInput.contains("tomorrow")) {
            LocalDateTime tomorrow = now.plusDays(1);
            return "Tomorrow will be " + tomorrow.getDayOfWeek().toString();
        } else {
            return "I'm not sure which day you are referring to.";
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void handleKeywordResponses(String input) {
        if (input.toLowerCase().contains("your name")) {
            stringOutput = "My name is Tara.";
        } else if (input.toLowerCase().contains("favorite color")) {
            stringOutput = "My favorite color is Pink.";
        } else if (input.toLowerCase().contains("favorite food")) {
            stringOutput = "My favorite food is Donuts!";
        } else if (input.toLowerCase().contains("how are you")) {
            stringOutput = "I'm good, thanks for asking! How are you?";
        } else if (input.toLowerCase().contains("time")) {
            stringOutput = "The current time is: \n" + getCurrentTime();
        } else if (input.toLowerCase().contains("date")) {
            if (((input.toLowerCase().contains("which")) || (input.toLowerCase().contains("what") || (input.toLowerCase().contains("today") || input.toLowerCase().contains("today's")))) && input.toLowerCase().contains("date")) {
                stringOutput = "The today's date is: " + LocalDate.now();
            } else {
                stringOutput = "What do you mean by 'date' ?";
            }
        } else if ((input.toLowerCase().contains("which") || (input.toLowerCase().contains("what"))) && input.toLowerCase().contains("month")) {
            // Format the month using a DateTimeFormatter
            stringOutput = "The current month is: " + Month.from(LocalDate.now());
        } else if ((input.toLowerCase().contains("which") || (input.toLowerCase().contains("what"))) && input.toLowerCase().contains("year")) {
            stringOutput = "The current year is: " + Year.from(LocalDate.now());
        } else if (input.toLowerCase().contains("day")) {
            stringOutput = handleDayQuery(input);
        } else if (input.toLowerCase().contains("weather") || (input.toLowerCase().contains("weather") && input.toLowerCase().contains("location"))) {
            stringOutput = "Sorry, I do not know about the weather at your location.";
        } else if (input.toLowerCase().contains("near") || input.toLowerCase().contains("nearby") && (input.toLowerCase().contains("park") || (input.toLowerCase().contains("restaurant") || input.toLowerCase().contains("metro") || input.toLowerCase().contains("train") || input.toLowerCase().contains("bus") || input.toLowerCase().contains("hotel") || input.toLowerCase().contains("school") || input.toLowerCase().contains("police") || input.toLowerCase().contains("hospital")))) {
            stringOutput = "Sorry, I do not have information about it.";
        } else if (input.toLowerCase().contains("open") && input.toLowerCase().contains("text recognition")) {
            // Start TextRecognition activity
            stringOutput = "";
            startActivity(new Intent(MainActivity.this, TextRecognition.class));
        } else {
            stringOutput = "Sorry, I don't understand. Can you please provide more information or rephrase your question?";
        }

        mainTextView.setText(stringOutput);

        CountDownTimer countDownTimer;

        // Set a new CountDownTimer for 2 seconds
        countDownTimer = new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // Do nothing on tick
            }

            @Override
            public void onFinish() {
                // Reset the text to the default value
                mainTextView.setText(display_text);
            }
        };

        // Start the timer
        countDownTimer.start();
    }
}
