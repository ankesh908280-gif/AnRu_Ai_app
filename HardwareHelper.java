package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.util.Log;

public class HardwareHelper {
    private static final String TAG = "JarvisHardware";
    private static boolean isTorchOn = false;

    public static synchronized boolean setTorch(Context context, boolean enable) {
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager == null) return false;

            String[] cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);

                if (Boolean.TRUE.equals(hasFlash) && facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraManager.setTorchMode(id, enable);
                    isTorchOn = enable;
                    Log.d(TAG, "Torch set to: " + enable);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling torch: " + e.getMessage());
        }
        return false;
    }

    public static boolean isTorchActive() {
        return isTorchOn;
    }

    public static boolean setVolumePercent(Context context, int percent) {
        try {
            AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager == null) return false;

            int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int targetVol = Math.round((Math.max(0, Math.min(100, percent)) / 100.0f) * maxVol);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVol, AudioManager.FLAG_SHOW_UI);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error setting volume: " + e.getMessage());
            return false;
        }
    }

    public static void vibratePattern(Context context, long[] pattern) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                VibratorManager vibratorManager = (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
                if (vibratorManager != null) {
                    Vibrator vibrator = vibratorManager.getDefaultVibrator();
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                }
            } else {
                Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
                    } else {
                        vibrator.vibrate(pattern, -1);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error vibrating: " + e.getMessage());
        }
    }

    public static String getBatteryStatusJson(Context context) {
        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            if (batteryStatus != null) {
                int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                float batteryPct = (level >= 0 && scale > 0) ? ((float) level / (float) scale) * 100.0f : 50.0f;
                boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL);

                return String.format("{\"level\": %d, \"charging\": %b}", Math.round(batteryPct), isCharging);
            }
        } catch (Exception e) {
            Log.e(TAG, "Battery status error: " + e.getMessage());
        }
        return "{\"level\": 100, \"charging\": false}";
    }
}
