package com.jarvis.ai;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.Settings;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

import org.json.JSONArray;

import java.util.List;

public class JarvisBridge {
    private static final String TAG = "JarvisBridge";
    private final Activity activity;
    private final WebView webView;

    public JarvisBridge(Activity activity, WebView webView) {
        this.activity = activity;
        this.webView = webView;
    }

    @JavascriptInterface
    public boolean isAndroid() {
        return true;
    }

    @JavascriptInterface
    public boolean directCall(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) return false;
        final String cleanNumber = phoneNumber.replaceAll("[^0-9+]", "");

        activity.runOnUiThread(() -> {
            try {
                if (activity.checkSelfPermission(android.Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + cleanNumber));
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(callIntent);
                    Log.d(TAG, "Direct call placed to: " + cleanNumber);
                } else {
                    // Fallback to dialer if CALL_PHONE permission not yet granted
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + cleanNumber));
                    dialIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(dialIntent);
                    Toast.makeText(activity, "Opening Dialer (Grant Phone permission for direct call)", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Call execution error: " + e.getMessage());
            }
        });
        return true;
    }

    @JavascriptInterface
    public boolean sendWhatsApp(String phone, String message) {
        final String msg = message != null ? message : "";
        final String rawPhone = phone != null ? phone.replaceAll("[^0-9]", "") : "";

        activity.runOnUiThread(() -> {
            try {
                // Arm accessibility auto-clicker
                JarvisAccessibilityService.setPendingAutoSend(rawPhone, msg);

                Intent waIntent = new Intent(Intent.ACTION_VIEW);
                String url;
                if (!rawPhone.isEmpty()) {
                    String formattedPhone = rawPhone;
                    if (formattedPhone.length() == 10) {
                        formattedPhone = "91" + formattedPhone; // Default to India prefix if 10-digit
                    }
                    url = "https://api.whatsapp.com/send?phone=" + formattedPhone + "&text=" + Uri.encode(msg);
                } else {
                    url = "https://api.whatsapp.com/send?text=" + Uri.encode(msg);
                }

                waIntent.setData(Uri.parse(url));
                waIntent.setPackage("com.whatsapp");
                waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(waIntent);
                Log.d(TAG, "WhatsApp intent launched for: " + rawPhone);
            } catch (Exception e) {
                // If standard WhatsApp not found, try without package specification
                try {
                    Intent fallback = new Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=" + Uri.encode(msg)));
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(fallback);
                } catch (Exception ex) {
                    Log.e(TAG, "WhatsApp dispatch error: " + ex.getMessage());
                }
            }
        });
        return true;
    }

    @JavascriptInterface
    public boolean openApp(String appNameOrQuery) {
        return AppLauncherHelper.launchApp(activity, appNameOrQuery);
    }

    @JavascriptInterface
    public boolean setFlashlight(boolean enable) {
        return HardwareHelper.setTorch(activity, enable);
    }

    @JavascriptInterface
    public boolean setVolume(int percent) {
        return HardwareHelper.setVolumePercent(activity, percent);
    }

    @JavascriptInterface
    public void vibrate(String patternCsv) {
        try {
            String[] parts = patternCsv.split(",");
            long[] pattern = new long[parts.length];
            for (int i = 0; i < parts.length; i++) {
                pattern[i] = Long.parseLong(parts[i].trim());
            }
            HardwareHelper.vibratePattern(activity, pattern);
        } catch (Exception e) {
            HardwareHelper.vibratePattern(activity, new long[]{0, 200, 100, 200});
        }
    }

    @JavascriptInterface
    public String getBatteryStatus() {
        return HardwareHelper.getBatteryStatusJson(activity);
    }

    @JavascriptInterface
    public String getInstalledApps() {
        return AppLauncherHelper.getInstalledAppsJson(activity);
    }

    @JavascriptInterface
    public boolean setAlarm(int hour, int minute, String message) {
        try {
            Intent intent = new Intent(AlarmClock.ACTION_SET_ALARM);
            intent.putExtra(AlarmClock.EXTRA_HOUR, hour);
            intent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, message != null ? message : "Jarvis Alarm");
            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarm: " + e.getMessage());
            return false;
        }
    }

    @JavascriptInterface
    public boolean setTimer(int seconds, String message) {
        try {
            Intent intent = new Intent(AlarmClock.ACTION_SET_TIMER);
            intent.putExtra(AlarmClock.EXTRA_LENGTH, seconds);
            intent.putExtra(AlarmClock.EXTRA_MESSAGE, message != null ? message : "Jarvis Timer");
            intent.putExtra(AlarmClock.EXTRA_SKIP_UI, true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error setting timer: " + e.getMessage());
            return false;
        }
    }

    @JavascriptInterface
    public void toggleFloatingWidget() {
        activity.runOnUiThread(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(activity)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
                Toast.makeText(activity, "Please allow Overlay Permission for Floating Arc Reactor", Toast.LENGTH_LONG).show();
                return;
            }

            Intent serviceIntent = new Intent(activity, FloatingArcReactorService.class);
            if (FloatingArcReactorService.isWidgetRunning()) {
                activity.stopService(serviceIntent);
                Toast.makeText(activity, "Floating Arc Reactor Closed", Toast.LENGTH_SHORT).show();
            } else {
                activity.startService(serviceIntent);
                Toast.makeText(activity, "Floating Arc Reactor Active on Screen", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @JavascriptInterface
    public boolean isAccessibilityEnabled() {
        AccessibilityManager am = (AccessibilityManager) activity.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK);
        for (AccessibilityServiceInfo enabledService : enabledServices) {
            if (enabledService.getId().contains(activity.getPackageName())) {
                return true;
            }
        }
        return false;
    }

    @JavascriptInterface
    public void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
        Toast.makeText(activity, "Enable 'Jarvis AI' under Installed Services for Auto-WhatsApp", Toast.LENGTH_LONG).show();
    }

    @JavascriptInterface
    public void startForegroundService() {
        Intent intent = new Intent(activity, JarvisForegroundService.class);
        intent.setAction(JarvisForegroundService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.startForegroundService(intent);
        } else {
            activity.startService(intent);
        }
    }
}
