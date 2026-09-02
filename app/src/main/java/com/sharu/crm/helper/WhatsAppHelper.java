package com.sharu.crm.helper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import java.net.URLEncoder;

public class WhatsAppHelper {

    public static void sendMessage(Context context, String phone, String message) {
        try {
            String cleanPhone = phone.replaceAll("[^0-9+]", "");
            String url = "https://api.whatsapp.com/send?phone=" + cleanPhone + "&text=" + URLEncoder.encode(message, "UTF-8");
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage("com.whatsapp");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void sendLeadFollowUp(Context context, String phone, String name, String details) {
        String message = "Hello " + name + ", following up on your inquiry regarding: " + details;
        sendMessage(context, phone, message);
    }
}
