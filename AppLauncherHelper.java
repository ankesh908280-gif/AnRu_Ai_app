package com.jarvis.ai;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLauncherHelper {
    private static final String TAG = "JarvisAppLauncher";

    // Common Package Aliases for quick and accurate matching
    private static final Map<String, String[]> APP_ALIASES = new HashMap<>();

    static {
        APP_ALIASES.put("com.google.android.youtube", new String[]{"youtube", "यूट्यूब", "yt"});
        APP_ALIASES.put("com.whatsapp", new String[]{"whatsapp", "व्हाट्सएप", "व्हाट्सअप", "wa"});
        APP_ALIASES.put("com.instagram.android", new String[]{"instagram", "इंस्टाग्राम", "insta"});
        APP_ALIASES.put("com.spotify.music", new String[]{"spotify", "स्पॉटिफ़ाई", "स्पॉटिफाई"});
        APP_ALIASES.put("com.google.android.apps.maps", new String[]{"maps", "google maps", "गूगल मैप्स", "नक्शा"});
        APP_ALIASES.put("com.android.chrome", new String[]{"chrome", "क्रोम", "browser"});
        APP_ALIASES.put("com.phonepe.app", new String[]{"phonepe", "फ़ोनपे", "फोन पे"});
        APP_ALIASES.put("net.one97.paytm", new String[]{"paytm", "पेटीएम"});
        APP_ALIASES.put("com.google.android.apps.nbu.paisa.user", new String[]{"gpay", "google pay", "गूगल पे"});
        APP_ALIASES.put("com.pubg.imobile", new String[]{"bgmi", "battlegrounds", "pubg"});
        APP_ALIASES.put("com.android.calculator2", new String[]{"calculator", "कैलकुलेटर", "calc"});
        APP_ALIASES.put("com.google.android.calculator", new String[]{"calculator", "कैलकुलेटर", "calc"});
        APP_ALIASES.put("com.android.camera", new String[]{"camera", "कैमरा"});
        APP_ALIASES.put("com.google.android.GoogleCamera", new String[]{"camera", "कैमरा"});
        APP_ALIASES.put("com.android.settings", new String[]{"settings", "सेटिंग्स", "setting"});
        APP_ALIASES.put("com.android.gallery3d", new String[]{"gallery", "गैलरी", "photos"});
        APP_ALIASES.put("com.google.android.apps.photos", new String[]{"photos", "google photos", "गैलरी"});
    }

    public static boolean launchApp(Context context, String query) {
        if (query == null || query.trim().isEmpty()) return false;
        String q = query.toLowerCase().trim();
        PackageManager pm = context.getPackageManager();

        // 1. Check known aliases first
        for (Map.Entry<String, String[]> entry : APP_ALIASES.entrySet()) {
            String pkg = entry.getKey();
            for (String alias : entry.getValue()) {
                if (q.contains(alias) || alias.contains(q)) {
                    if (launchPackage(context, pkg)) {
                        return true;
                    }
                }
            }
        }

        // 2. Scan all installed launcher apps
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> pkgAppsList = pm.queryIntentActivities(mainIntent, 0);

        // Exact label match
        for (ResolveInfo info : pkgAppsList) {
            String label = info.loadLabel(pm).toString().toLowerCase();
            if (label.equals(q)) {
                return launchPackage(context, info.activityInfo.packageName);
            }
        }

        // Substring label match
        for (ResolveInfo info : pkgAppsList) {
            String label = info.loadLabel(pm).toString().toLowerCase();
            if (label.contains(q) || q.contains(label)) {
                return launchPackage(context, info.activityInfo.packageName);
            }
        }

        // 3. Fallback: Check package name substring
        for (ResolveInfo info : pkgAppsList) {
            String pkg = info.activityInfo.packageName.toLowerCase();
            if (pkg.contains(q)) {
                return launchPackage(context, info.activityInfo.packageName);
            }
        }

        // 4. Fallback to Play Store if not installed
        try {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(query)));
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(marketIntent);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Play Store launch fallback failed: " + e.getMessage());
        }

        return false;
    }

    public static boolean launchPackage(Context context, String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                Log.d(TAG, "Successfully launched package: " + packageName);
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch package " + packageName + ": " + e.getMessage());
        }
        return false;
    }

    public static String getInstalledAppsJson(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> list = pm.queryIntentActivities(mainIntent, 0);

            JSONArray arr = new JSONArray();
            for (ResolveInfo info : list) {
                JSONObject obj = new JSONObject();
                obj.put("name", info.loadLabel(pm).toString());
                obj.put("package", info.activityInfo.packageName);
                arr.put(obj);
            }
            return arr.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error listing installed apps: " + e.getMessage());
            return "[]";
        }
    }
}
