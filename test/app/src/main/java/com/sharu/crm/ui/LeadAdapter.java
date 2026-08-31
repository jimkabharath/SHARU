package com.sharu.crm.ui;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.sharu.crm.R;
import com.sharu.crm.helper.WhatsAppHelper;
import java.util.List;
public class LeadAdapter extends RecyclerView.Adapter<LeadAdapter.LeadViewHolder> { private Context context; private List<LeadModel> leads; public LeadAdapter(Context c, List<LeadModel> l) { this.context = c; this.leads = l; } @NonNull @Override public LeadViewHolder onCreateViewHolder(@NonNull ViewGroup p, int v) { View view = LayoutInflater.from(context).inflate(R.layout.item_lead, p, false); return new LeadViewHolder(view); } @Override public void onBindViewHolder(@NonNull LeadViewHolder h, int pos) { LeadModel item = leads.get(pos); h.tvName.setText(item.name); h.tvDetails.setText(item.details); h.btnWhatsApp.setOnClickListener(v -> WhatsAppHelper.sendLeadFollowUp(context, item.phone, item.name, item.details)); } @Override public int getItemCount() { return leads != null ? leads.size() : 0; } static class LeadViewHolder extends RecyclerView.ViewHolder { TextView tvName, tvDetails, tvPill; ImageButton btnCall, btnWhatsApp; LeadViewHolder(View v) { super(v); tvName = v.findViewById(R.id.tvLeadName); tvDetails = v.findViewById(R.id.tvDetails); tvPill = v.findViewById(R.id.tvCategoryPill); btnCall = v.findViewById(R.id.btnCall); btnWhatsApp = v.findViewById(R.id.btnWhatsApp); } } }