package com.sharu.crm.telephony;
import android.telecom.Call;
import android.telecom.InCallService;
public class SharuInCallService extends InCallService { @Override public void onCallAdded(Call c) { super.onCallAdded(c); } @Override public void onCallRemoved(Call c) { super.onCallRemoved(c); } }