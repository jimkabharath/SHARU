package com.sharu.crm.automation;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;

public class SharuAutomationService extends AccessibilityService {

    public static volatile boolean autoSendPending = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!autoSendPending) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        List<AccessibilityNodeInfo> targets = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send");
        if (targets == null || targets.isEmpty()) {
            targets = root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/send");
        }

        if (targets != null && !targets.isEmpty()) {
            final AccessibilityNodeInfo sendBtn = targets.get(0);
            mainHandler.postDelayed(() -> {
                if (sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    autoSendPending = false;
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }
            }, 300);
        }
    }

    @Override
    public void onInterrupt() {
        autoSendPending = false;
    }
}
