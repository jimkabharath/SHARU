package com.sharu.crm.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.sharu.crm.R;
import com.sharu.crm.helper.WhatsAppHelper;
import com.sharu.crm.voice.VoiceCommandManager;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements VoiceCommandManager.VoiceCallback {

    private static final int RECORD_AUDIO_REQ = 101;
    private VoiceCommandManager voiceManager;
    private Button btnVoice;
    private Button btnAddLead;
    private List<LeadModel> leadList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView rv = findViewById(R.id.rvLeads);
        rv.setLayoutManager(new LinearLayoutManager(this));

        leadList = new ArrayList<>();
        leadList.add(new LeadModel("Direct Sample Lead", "+919876543210", "HOT", "Budget: 50L | Location: Hyderabad"));
        rv.setAdapter(new LeadAdapter(leadList));

        btnAddLead = findViewById(R.id.btnAddLead);
        btnVoice = findViewById(R.id.btnVoice);

        voiceManager = new VoiceCommandManager(this, this);

        btnAddLead.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddLeadActivity.class);
            startActivity(intent);
        });

        btnVoice.setOnClickListener(v -> checkAudioPermissionAndListen());
    }

    private void checkAudioPermissionAndListen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, RECORD_AUDIO_REQ);
        } else {
            voiceManager.startListening();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RECORD_AUDIO_REQ && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            voiceManager.startListening();
        } else {
            Toast.makeText(this, "Microphone permission is required for voice commands", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onCommandRecognized(String action, String payload) {
        Toast.makeText(this, "Recognized: " + payload, Toast.LENGTH_SHORT).show();

        if (action.equals("ACTION_CALL") && !leadList.isEmpty()) {
            Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + leadList.get(0).getPhone()));
            startActivity(callIntent);
        } else if (action.equals("ACTION_WHATSAPP") && !leadList.isEmpty()) {
            WhatsAppHelper.sendMessage(this, leadList.get(0).getPhone(), "Hello! Following up regarding your inquiry.");
        }
    }

    @Override
    public void onError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onListeningStatus(boolean isListening) {
        btnVoice.setText(isListening ? "Listening..." : "🎤 Voice");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (voiceManager != null) {
            voiceManager.destroy();
        }
    }
}
