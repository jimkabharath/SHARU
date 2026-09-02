package com.sharu.crm.storage;

import android.content.Context;
import java.io.*;
import java.util.*;

public class FileManager {

    private static final String LEADS_FILE = "leads.cli";

    /**
     * Format: name|phone|status|details|timestamp
     */
    public static synchronized void saveOrUpdateLead(Context context, String name, String phone, String status, String details) {
        Map<String, String> existingMap = loadAllLeadsRaw(context);
        long now = System.currentTimeMillis();
        String record = name + "|" + phone + "|" + status + "|" + details + "|" + now;
        
        // Upsert by phone number
        existingMap.put(phone, record);

        writeRawMap(context, existingMap);
    }

    /**
     * Resolves conflicts by comparing timestamps: Last Write Wins.
     */
    public static synchronized void mergeIncomingSync(Context context, List<String> incomingLines) {
        Map<String, String> localMap = loadAllLeadsRaw(context);

        for (String line : incomingLines) {
            String[] incomingParts = line.split("\\|");
            if (incomingParts.length >= 5) {
                String phone = incomingParts[1];
                long incomingTime = Long.parseLong(incomingParts[4]);

                if (localMap.containsKey(phone)) {
                    String[] localParts = localMap.get(phone).split("\\|");
                    long localTime = (localParts.length >= 5) ? Long.parseLong(localParts[4]) : 0L;
                    if (incomingTime > localTime) {
                        localMap.put(phone, line); // Incoming is newer
                    }
                } else {
                    localMap.put(phone, line); // New lead
                }
            }
        }
        writeRawMap(context, localMap);
    }

    public static Map<String, String> loadAllLeadsRaw(Context context) {
        Map<String, String> map = new LinkedHashMap<>();
        File file = new File(context.getFilesDir(), LEADS_FILE);
        if (!file.exists()) return map;

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\\|");
                if (parts.length >= 2) {
                    map.put(parts[1], line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

    private static void writeRawMap(Context context, Map<String, String> map) {
        File file = new File(context.getFilesDir(), LEADS_FILE);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file, false))) {
            for (String val : map.values()) {
                bw.write(val);
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getFullBackupPayload(Context context) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> leads = loadAllLeadsRaw(context);
        sb.append("📋 *SHARU CRM - CLI DATA BACKUP*\n");
        sb.append("Total Records: ").append(leads.size()).append("\n\n");

        int count = 1;
        for (String entry : leads.values()) {
            String[] p = entry.split("\\|");
            if (p.length >= 4) {
                sb.append(count++).append(". ").append(p[0])
                  .append(" (").append(p[1]).append(")\n")
                  .append("   • Status: ").append(p[2]).append("\n")
                  .append("   • Notes: ").append(p[3]).append("\n");
            }
        }
        return sb.toString();
    }
}
