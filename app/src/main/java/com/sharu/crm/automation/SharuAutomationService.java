package com.sharu.crm.automation;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
import java.util.Random;

public class SharuAutomationService extends AccessibilityService {

    public static volatile boolean autoSendPending = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();

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

            // Anti-ban human jitter: 1800ms to 3200ms randomized wait
            int humanJitterMs = 1800 + random.nextInt(1400);

            mainHandler.postDelayed(() -> {
                if (sendBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    autoSendPending = false;
                    // Additional pause before navigating back
                    mainHandler.postDelayed(() -> performGlobalAction(GLOBAL_ACTION_BACK), 600);
                }
            }, humanJitterMs);
        }
    }

    @Override
    public void onInterrupt() {
        autoSendPending = false;
    }
}
