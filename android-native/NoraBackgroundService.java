package com.nora.assistant;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Locale;

public class NoraBackgroundService extends Service {

    public static final String CHANNEL_ID = "NoraAssistantChannel";
    public static final int NOTIFICATION_ID = 1001;
    public static final String ACTION_WAKE_DETECTED = "com.nora.assistant.WAKE_DETECTED";
    public static final String EXTRA_PHRASE = "phrase";

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean isRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = buildForegroundNotification("نورا تعمل في الخلفية وتنتظر نداء «يا نورا»");
        startForeground(NOTIFICATION_ID, notification);

        isRunning = true;
        startListeningLoop();
        return START_STICKY;
    }

    private void startListeningLoop() {
        if (!isRunning) return;
        handler.post(() -> {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(this)) return;
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                }
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-SA");
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override public void onReadyForSpeech(Bundle params) {}
                    @Override public void onBeginningOfSpeech() {}
                    @Override public void onRmsChanged(float rmsdB) {}
                    @Override public void onBufferReceived(byte[] buffer) {}
                    @Override public void onEndOfSpeech() {}

                    @Override
                    public void onError(int error) {
                        restartAfterDelay(1200);
                    }

                    @Override
                    public void onResults(Bundle results) {
                        inspectMatches(results);
                        restartAfterDelay(600);
                    }

                    @Override
                    public void onPartialResults(Bundle partialResults) {
                        inspectMatches(partialResults);
                    }

                    @Override public void onEvent(int eventType, Bundle params) {}
                });

                speechRecognizer.startListening(recognizerIntent);
            } catch (Exception e) {
                restartAfterDelay(2000);
            }
        });
    }

    private void inspectMatches(Bundle bundle) {
        if (bundle == null) return;
        ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches == null) return;
        for (String text : matches) {
            if (text != null && (text.contains("يا نورا") || text.contains("نورا"))) {
                Intent broadcast = new Intent(ACTION_WAKE_DETECTED);
                broadcast.putExtra(EXTRA_PHRASE, text);
                sendBroadcast(broadcast);

                Intent openApp = new Intent(this, MainActivity.class);
                openApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                openApp.putExtra(EXTRA_PHRASE, text);
                startActivity(openApp);
                break;
            }
        }
    }

    private void restartAfterDelay(long delayMs) {
        if (!isRunning) return;
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(this::startListeningLoop, delayMs);
    }

    private Notification buildForegroundNotification(String contentText) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("نورا — المساعد الصوتي")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "خدمة مساعد نورا بالخلفية",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        handler.removeCallbacksAndMessages(null);
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
