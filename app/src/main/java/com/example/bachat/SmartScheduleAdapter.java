package com.example.bachat;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class SmartScheduleAdapter extends RecyclerView.Adapter<SmartScheduleAdapter.ScheduleViewHolder> {

    private final List<SmartOptimizer.ApplianceScheduleItem> items;
    private final boolean isMonthly;

    public SmartScheduleAdapter(List<SmartOptimizer.ApplianceScheduleItem> items, boolean isMonthly) {
        this.items = items;
        this.isMonthly = isMonthly;
    }

    public SmartScheduleAdapter(List<SmartOptimizer.ApplianceScheduleItem> items) {
        this(items, true);
    }

    @NonNull
    @Override
    public ScheduleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_smart_schedule, parent, false);
        return new ScheduleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ScheduleViewHolder holder, int position) {
        SmartOptimizer.ApplianceScheduleItem item = items.get(position);

        holder.tvName.setText(item.name);
        holder.tvSpecs.setText(String.format(Locale.getDefault(), "%d Unit%s · %.0f Watts · %s",
                item.quantity, (item.quantity > 1 ? "s" : ""), item.wattage, item.environmentName));

        // Priority Badge (Evaluated automatically from area, state, time, and BEE ratings)
        if (holder.tvPriorityBadge != null) {
            String badge = (item.priorityBadge != null && !item.priorityBadge.isEmpty()) ?
                    item.priorityBadge : (item.isEssential ? "ESSENTIAL LOAD" : "PRIORITY LOAD");
            holder.tvPriorityBadge.setText(badge);

            if (item.isEssential) {
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
            } else if (badge.contains("HIGH")) {
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_teal));
            } else {
                holder.tvPriorityBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_purple_light));
            }
        }

        // BEE Star Rating Badge
        if (holder.tvBeeBadge != null) {
            if (item.beeLabel != null && !item.beeLabel.isEmpty()) {
                holder.tvBeeBadge.setVisibility(View.VISIBLE);
                holder.tvBeeBadge.setText(item.beeLabel);
            } else {
                holder.tvBeeBadge.setVisibility(View.GONE);
            }
        }

        // Recommended Operating Hours Badge
        if (item.recommendedHours <= 0) {
            holder.tvHoursBadge.setText("0 hrs / day");
            holder.tvHoursBadge.setBackgroundResource(R.drawable.bg_chip);
            holder.tvHoursBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_coral));
        } else if (item.recommendedHours >= (item.S_max - 0.05)) {
            holder.tvHoursBadge.setText(String.format(Locale.getDefault(), "%.1f hrs / day", item.recommendedHours));
            holder.tvHoursBadge.setBackgroundResource(R.drawable.bg_chip_selected);
            holder.tvHoursBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
        } else {
            holder.tvHoursBadge.setText(String.format(Locale.getDefault(), "%.1f hrs / day", item.recommendedHours));
            holder.tvHoursBadge.setBackgroundResource(R.drawable.bg_chip_selected);
            holder.tvHoursBadge.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
        }

        // Current requested hours
        if (holder.tvMaxHours != null) {
            holder.tvMaxHours.setText(String.format(Locale.getDefault(), "Current: %.1fh", item.S_max));
        }

        // Status Note
        if (holder.tvStatusNote != null) {
            holder.tvStatusNote.setText(item.statusNote);
        }

        // Savings Subtext
        if (holder.tvSavingsSubtext != null) {
            if (isMonthly) {
                if (item.monthlyCostSavings > 0.5) {
                    holder.tvSavingsSubtext.setText(String.format(Locale.getDefault(), "Saves ₹ %,.0f / mo", item.monthlyCostSavings));
                    holder.tvSavingsSubtext.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
                } else {
                    holder.tvSavingsSubtext.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo", item.recommendedDailyCost * 30.0));
                    holder.tvSavingsSubtext.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
                }
            } else {
                if (item.dailyCostSavings > 0.05) {
                    holder.tvSavingsSubtext.setText(String.format(Locale.getDefault(), "Saves ₹ %,.2f / day", item.dailyCostSavings));
                    holder.tvSavingsSubtext.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
                } else {
                    holder.tvSavingsSubtext.setText(String.format(Locale.getDefault(), "₹ %,.2f / day", item.recommendedDailyCost));
                    holder.tvSavingsSubtext.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));
                }
            }
        }

        // Visual category icon and color
        int iconRes = item.iconRes != 0 ? item.iconRes : android.R.drawable.ic_menu_rotate;
        int colorRes = item.colorRes != 0 ? item.colorRes : R.color.accent_green;
        int color = ContextCompat.getColor(holder.itemView.getContext(), colorRes);

        holder.ivIcon.setImageResource(iconRes);
        holder.ivIcon.setImageTintList(ColorStateList.valueOf(color));
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    public static class ScheduleViewHolder extends RecyclerView.ViewHolder {
        FrameLayout flIconBg;
        ImageView ivIcon;
        TextView tvName, tvSpecs, tvBeeBadge, tvHoursBadge, tvMaxHours, tvStatusNote, tvSavingsSubtext;
        TextView tvPriorityBadge;

        public ScheduleViewHolder(@NonNull View itemView) {
            super(itemView);
            flIconBg = itemView.findViewById(R.id.flScheduleIconBg);
            ivIcon = itemView.findViewById(R.id.ivScheduleIcon);
            tvName = itemView.findViewById(R.id.tvScheduleAppName);
            tvSpecs = itemView.findViewById(R.id.tvScheduleAppSpecs);
            tvBeeBadge = itemView.findViewById(R.id.tvScheduleBeeBadge);
            tvHoursBadge = itemView.findViewById(R.id.tvScheduleHoursBadge);
            tvMaxHours = itemView.findViewById(R.id.tvScheduleMaxHours);
            tvStatusNote = itemView.findViewById(R.id.tvScheduleStatusNote);
            tvSavingsSubtext = itemView.findViewById(R.id.tvScheduleSavingsSubtext);
            tvPriorityBadge = itemView.findViewById(R.id.tvSchedulePriorityBadge);
        }
    }
}
