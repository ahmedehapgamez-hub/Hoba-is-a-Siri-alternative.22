package com.nora.assistant;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

    private static final int PERMISSION_REQ_CODE = 2026;
    private BroadcastReceiver wakeReceiver;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestRequiredPermissions();

        bridge.getWebView().post(() -> {
            WebView webView = bridge.getWebView();
            webView.addJavascriptInterface(new NoraNativeBridge(), "NoraNativeBridge");
        });

        wakeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String phrase = intent.getStringExtra(NoraBackgroundService.EXTRA_PHRASE);
                notifyWebWakeWord(phrase);
            }
        };

        IntentFilter filter = new IntentFilter(NoraBackgroundService.ACTION_WAKE_DETECTED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(wakeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(wakeReceiver, filter);
        }
    }

    private void requestRequiredPermissions() {
        String[] perms = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.POST_NOTIFICATIONS}
                : new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};

        boolean needAsk = false;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needAsk = true;
                break;
            }
        }
        if (needAsk) {
            ActivityCompat.requestPermissions(this, perms, PERMISSION_REQ_CODE);
        }
    }

    private void notifyWebWakeWord(String phrase) {
        if (bridge == null || bridge.getWebView() == null) return;
        String safe = phrase == null ? "يا نورا" : phrase.replace("'", "\\'");
        bridge.getWebView().post(() ->
                bridge.getWebView().evaluateJavascript(
                        "if (window.onNativeWakeWordDetected) window.onNativeWakeWordDetected('" + safe + "');",
                        null
                )
        );
    }

    @Override
    protected void onDestroy() {
        if (wakeReceiver != null) {
            unregisterReceiver(wakeReceiver);
        }
        super.onDestroy();
    }

    public class NoraNativeBridge {

        @JavascriptInterface
        public void startBackgroundAssistant() {
            runOnUiThread(() -> {
                Intent svc = new Intent(MainActivity.this, NoraBackgroundService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(svc);
                } else {
                    startService(svc);
                }
            });
        }

        @JavascriptInterface
        public void stopBackgroundAssistant() {
            runOnUiThread(() -> {
                Intent svc = new Intent(MainActivity.this, NoraBackgroundService.class);
                svc.setAction("STOP");
                startService(svc);
            });
        }

        @JavascriptInterface
        public void setTorch(boolean enabled) {
            runOnUiThread(() -> {
                try {
                    CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    String[] ids = cm.getCameraIdList();
                    if (ids.length > 0) {
                        cm.setTorchMode(ids[0], enabled);
                    }
                } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public void openSettings(String action) {
            runOnUiThread(() -> {
                try {
                    Intent intent = new Intent(action != null && !action.isEmpty() ? action : Settings.ACTION_SETTINGS);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                } catch (Exception ignored) {}
            });
        }

        @JavascriptInterface
        public void launchApp(String packageName) {
            runOnUiThread(() -> {
                try {
                    Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launch);
                    }
                } catch (Exception ignored) {}
            });
        }
    }
}
