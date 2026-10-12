package com.jarvis.ai;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;

public class JarvisAccessibilityService extends AccessibilityService {
    private static final String TAG = "JarvisAccessibility";
    
    // Auto WhatsApp state flags
    private static volatile boolean pendingAutoSend = false;
    private static volatile String targetPhone = "";
    private static volatile String targetMessage = "";
    private static volatile long requestTimestamp = 0;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static void setPendingAutoSend(String phone, String message) {
        pendingAutoSend = true;
        targetPhone = phone != null ? phone : "";
        targetMessage = message != null ? message : "";
        requestTimestamp = System.currentTimeMillis();
        Log.d(TAG, "Pending auto-send WhatsApp armed for: " + phone);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!pendingAutoSend) return;

        // Auto-expire after 15 seconds to avoid accidental clicks later
        if (System.currentTimeMillis() - requestTimestamp > 15000) {
            pendingAutoSend = false;
            Log.d(TAG, "Auto-send request timed out.");
            return;
        }

        CharSequence pkgChar = event.getPackageName();
        if (pkgChar == null) return;
        String packageName = pkgChar.toString();

        if ("com.whatsapp".equals(packageName) || "com.whatsapp.w4b".equals(packageName)) {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode == null) return;

            attemptWhatsAppSend(rootNode);
        }
    }

    private void attemptWhatsAppSend(AccessibilityNodeInfo root) {
        try {
            // 1. Look for Send button by resource ID
            List<AccessibilityNodeInfo> sendNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send");
            if (sendNodes == null || sendNodes.isEmpty()) {
                sendNodes = root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/send");
            }

            if (sendNodes != null && !sendNodes.isEmpty()) {
                for (AccessibilityNodeInfo node : sendNodes) {
                    if (node.isEnabled() && node.isClickable()) {
                        performClickAndReset(node);
                        return;
                    }
                }
            }

            // 2. Fallback: Search by Content Description (English "Send" or Hindi "भेजें")
            List<AccessibilityNodeInfo> descNodes = root.findAccessibilityNodeInfosByText("Send");
            if (descNodes != null && !descNodes.isEmpty()) {
                for (AccessibilityNodeInfo node : descNodes) {
                    if (node.isClickable() && node.isEnabled()) {
                        performClickAndReset(node);
                        return;
                    }
                }
            }

            // 3. Fallback: Recursive search for nodes with content description
            searchNodeRecursive(root);

        } catch (Exception e) {
            Log.e(TAG, "Error in attemptWhatsAppSend: " + e.getMessage());
        }
    }

    private boolean searchNodeRecursive(AccessibilityNodeInfo node) {
        if (node == null) return false;

        CharSequence desc = node.getContentDescription();
        if (desc != null) {
            String d = desc.toString().toLowerCase();
            if (d.contains("send") || d.contains("भेजें")) {
                if (node.isClickable()) {
                    performClickAndReset(node);
                    return true;
                } else if (node.getParent() != null && node.getParent().isClickable()) {
                    performClickAndReset(node.getParent());
                    return true;
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (searchNodeRecursive(child)) return true;
        }
        return false;
    }

    private void performClickAndReset(AccessibilityNodeInfo targetNode) {
        Log.d(TAG, "Executing WhatsApp Auto-Click Send!");
        targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        pendingAutoSend = false;

        // Give WhatsApp 800ms to dispatch message, then return to Jarvis
        mainHandler.postDelayed(() -> {
            try {
                performGlobalAction(GLOBAL_ACTION_BACK);
            } catch (Exception e) {
                Log.w(TAG, "Could not perform back action: " + e.getMessage());
            }
        }, 800);
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "JarvisAccessibilityService interrupted");
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.i(TAG, "JarvisAccessibilityService connected and ready!");
    }
}
