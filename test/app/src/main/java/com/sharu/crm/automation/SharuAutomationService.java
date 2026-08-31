package com.sharu.crm.automation;
import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.List;
public class SharuAutomationService extends AccessibilityService { public static volatile boolean autoSendPending = false; private final Handler mainHandler = new Handler(Looper.getMainLooper()); @Override public void onAccessibilityEvent(AccessibilityEvent event) { if (!autoSendPending) return; AccessibilityNodeInfo root = getRootInActiveWindow(); if (root == null) return; List<AccessibilityNodeInfo> btns = root.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send"); if (btns == null || btns.isEmpty()) btns = root.findAccessibilityNodeInfosByViewId("com.whatsapp.w4b:id/send"); if (btns != null && !btns.isEmpty()) { mainHandler.postDelayed(() -> { if (btns.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK)) { autoSendPending = false; performGlobalAction(GLOBAL_ACTION_BACK); } }, 300); } } @Override public void onInterrupt() { autoSendPending = false; } }