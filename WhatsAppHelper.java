package com.sharu.crm.helper;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;
import com.sharu.crm.automation.SharuAutomationService;
import java.net.URLEncoder;
public class WhatsAppHelper { public static void sendLeadFollowUp(Context context, String phone, String leadName, String emiSummary) { String targetPackage = getInstalledWhatsAppPackage(context); if (targetPackage == null) { Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show(); return; } try { String clean = phone.replaceAll("[^0-9]", ""); if (clean.length() == 10) clean = "91" + clean; String msg = "Hello " + leadName + ",\n" + emiSummary; String uri = "https://api.whatsapp.com/send?phone=" + clean + "&text=" + URLEncoder.encode(msg, "UTF-8"); SharuAutomationService.autoSendPending = isAccessibilityServiceEnabled(context); Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri)); intent.setPackage(targetPackage); intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(intent); } catch (Exception ignored) {} } public static String getInstalledWhatsAppPackage(Context c) { PackageManager pm = c.getPackageManager(); try { pm.getPackageInfo("com.whatsapp.w4b", 0); return "com.whatsapp.w4b"; } catch (Exception e) { try { pm.getPackageInfo("com.whatsapp", 0); return "com.whatsapp"; } catch (Exception e2) { return null; } } } public static boolean isAccessibilityServiceEnabled(Context c) { int enabled = 0; try { enabled = Settings.Secure.getInt(c.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED); } catch (Exception ignored) {} return enabled == 1; } }