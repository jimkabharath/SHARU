package com.sharu.crm.helper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.sharu.crm.automation.SharuAutomationService;
import com.sharu.crm.storage.FileManager;
import java.net.URLEncoder;

public class WhatsAppHelper {

    public static void sendMessage(Context context, String phone, String message) {
        try {
            SharuAutomationService.autoSendPending = true;
            String cleanPhone = phone.replaceAll("[^0-9]", "");
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + URLEncoder.encode(message, "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Follow-up message used by LeadAdapter
     */
    public static void sendLeadFollowUp(Context context, String phone, String name, String details) {
        String template = "Hello " + name + ",\n\nFollowing up regarding your requirement (" + details + "). Let me know a convenient time to connect!";
        sendMessage(context, phone, template);
    }

    /**
     * Exports all local .cli data directly to your own WhatsApp number as a daily backup.
     */
    public static void sendDailyBackupToWhatsApp(Context context, String selfPhoneNumber) {
        String backupData = FileManager.getFullBackupPayload(context);
        sendMessage(context, selfPhoneNumber, backupData);
    }
}
