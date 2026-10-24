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

public class ApplianceAdapter extends RecyclerView.Adapter<ApplianceAdapter.AppViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(ApplianceItem item);
    }

    public interface OnQuantityChangeListener {
        void onQuantityChanged(ApplianceItem item, int newQty);
    }

    private List<ApplianceItem> appliances;
    private OnItemClickListener listener;
    private OnQuantityChangeListener qtyListener;
    private double tariffRate = 8.0;
    private boolean isMonthlyMode = true;

    public ApplianceAdapter(List<ApplianceItem> appliances, OnItemClickListener listener, OnQuantityChangeListener qtyListener) {
        this.appliances = appliances;
        this.listener = listener;
        this.qtyListener = qtyListener;
    }

    public void setTariffRate(double rate) {
        this.tariffRate = rate > 0 ? rate : 8.0;
        notifyDataSetChanged();
    }

    public void setMonthlyMode(boolean isMonthly) {
        this.isMonthlyMode = isMonthly;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_appliance, parent, false);
        return new AppViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppViewHolder holder, int position) {
        ApplianceItem app = appliances.get(position);
        holder.tvName.setText(app.name);
        holder.tvQty.setText(String.valueOf(app.quantity));

        boolean isContinuous = SmartOptimizer.isContinuousEssential(app.name);
        double requestedHours = isContinuous ? Math.max(24.0, app.hoursPerDay) : (app.hoursPerDay > 0 ? app.hoursPerDay : 8.0);
        String hoursStr = (requestedHours == (long) requestedHours) ?
                String.format(Locale.getDefault(), "%.0f", requestedHours) :
                String.format(Locale.getDefault(), "%.1f", requestedHours);

        if (isContinuous) {
            app.allocatedHours = Math.max(24.0, app.allocatedHours);
            holder.tvWattage.setText(String.format(Locale.getDefault(), "%.0fW · 24h Baseline", app.wattage));
            if (holder.tvAllocatedHours != null) {
                holder.tvAllocatedHours.setText("24 hrs / day (Continuous)");
                holder.tvAllocatedHours.setBackgroundResource(R.drawable.bg_chip_selected);
                holder.tvAllocatedHours.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
            }
        } else {
            holder.tvWattage.setText(String.format(Locale.getDefault(), "%.0fW · %s hrs / day", app.wattage, hoursStr));
            if (holder.tvAllocatedHours != null) {
                if (app.allocatedHours <= 0.0) {
                    holder.tvAllocatedHours.setText("Turn off (0 hrs / day)");
                    holder.tvAllocatedHours.setBackgroundResource(R.drawable.bg_chip);
                    holder.tvAllocatedHours.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_coral));
                } else {
                    String allocStr = (app.allocatedHours == (long) app.allocatedHours) ?
                            String.format(Locale.getDefault(), "%.0f", app.allocatedHours) :
                            String.format(Locale.getDefault(), "%.1f", app.allocatedHours);
                    holder.tvAllocatedHours.setText(String.format(Locale.getDefault(), "%s hrs / day", allocStr));
                    holder.tvAllocatedHours.setBackgroundResource(R.drawable.bg_chip_selected);
                    holder.tvAllocatedHours.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
                }
            }
        }

        double effectiveHours = (app.allocatedHours >= 0) ? app.allocatedHours : requestedHours;

        if (isMonthlyMode) {
            double originalMonthlyKwh = (app.wattage * app.quantity * requestedHours * 30.0) / 1000.0;
            double originalMonthlyCost = originalMonthlyKwh * tariffRate;
            double optMonthlyKwh = (app.wattage * app.quantity * effectiveHours * 30.0) / 1000.0;
            double optMonthlyCost = optMonthlyKwh * tariffRate;
            double saveMonthly = Math.max(0.0, originalMonthlyCost - optMonthlyCost);

            if (saveMonthly > 0.5) {
                holder.tvMonthlyCost.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo · Saves ₹ %,.0f", optMonthlyCost, saveMonthly));
            } else {
                holder.tvMonthlyCost.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo", optMonthlyCost));
            }
        } else {
            double originalDailyKwh = (app.wattage * app.quantity * requestedHours) / 1000.0;
            double originalDailyCost = originalDailyKwh * tariffRate;
            double optDailyKwh = (app.wattage * app.quantity * effectiveHours) / 1000.0;
            double optDailyCost = optDailyKwh * tariffRate;
            double saveDaily = Math.max(0.0, originalDailyCost - optDailyCost);

            if (saveDaily > 0.05) {
                holder.tvMonthlyCost.setText(String.format(Locale.getDefault(), "₹ %,.2f / day · Saves ₹ %,.2f", optDailyCost, saveDaily));
            } else {
                holder.tvMonthlyCost.setText(String.format(Locale.getDefault(), "₹ %,.2f / day", optDailyCost));
            }
        }

        // Never display raw R formula badge to user
        if (holder.tvBenefitBadge != null) {
            holder.tvBenefitBadge.setVisibility(View.GONE);
        }

        // Color coding by category
        int iconRes = android.R.drawable.ic_menu_agenda;
        int colorRes = R.color.icon_default;

        String n = app.name.toLowerCase();
        if (n.contains("fan")) {
            iconRes = android.R.drawable.ic_menu_rotate;
            colorRes = R.color.icon_fan;
        } else if (n.contains("ac") || n.contains("air") || n.contains("cooler")) {
            iconRes = android.R.drawable.ic_menu_sort_by_size;
            colorRes = R.color.icon_ac;
        } else if (n.contains("light") || n.contains("lamp") || n.contains("bulb")) {
            iconRes = android.R.drawable.ic_menu_day;
            colorRes = R.color.icon_light;
        } else if (n.contains("tv") || n.contains("television") || n.contains("screen")) {
            iconRes = android.R.drawable.ic_menu_slideshow;
            colorRes = R.color.icon_tv;
        } else if (SmartOptimizer.isContinuousEssential(app.name)) {
            iconRes = android.R.drawable.ic_menu_directions;
            colorRes = R.color.icon_fridge;
        } else if (n.contains("washing") || n.contains("dryer")) {
            iconRes = android.R.drawable.ic_menu_compass;
            colorRes = R.color.icon_washing;
        } else if (n.contains("computer") || n.contains("pc") || n.contains("laptop") || n.contains("server")) {
            iconRes = android.R.drawable.ic_menu_call;
            colorRes = R.color.icon_computer;
        } else if (n.contains("machinery") || n.contains("motor") || n.contains("pump")) {
            iconRes = android.R.drawable.ic_menu_preferences;
            colorRes = R.color.icon_factory;
        }

        int color = ContextCompat.getColor(holder.itemView.getContext(), colorRes);
        holder.ivIcon.setImageResource(iconRes);
        holder.ivIcon.setImageTintList(ColorStateList.valueOf(color));

        // Stepper button listeners
        holder.btnMinus.setOnClickListener(v -> {
            if (qtyListener != null) {
                int newQty = Math.max(1, app.quantity - 1);
                qtyListener.onQuantityChanged(app, newQty);
            }
        });

        holder.btnPlus.setOnClickListener(v -> {
            if (qtyListener != null) {
                int newQty = app.quantity + 1;
                qtyListener.onQuantityChanged(app, newQty);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(app);
        });
    }

    @Override
    public int getItemCount() {
        return appliances.size();
    }

    public static class AppViewHolder extends RecyclerView.ViewHolder {
        FrameLayout flIconBg;
        ImageView ivIcon;
        TextView tvName, tvQty, tvWattage, tvAllocatedHours, tvMonthlyCost, tvBenefitBadge;
        ImageView btnMinus, btnPlus;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            flIconBg = itemView.findViewById(R.id.flAppIconBg);
            ivIcon = itemView.findViewById(R.id.ivAppIcon);
            tvName = itemView.findViewById(R.id.tvAppName);
            tvQty = itemView.findViewById(R.id.tvAppQty);
            tvWattage = itemView.findViewById(R.id.tvAppWattage);
            tvAllocatedHours = itemView.findViewById(R.id.tvAppAllocatedHours);
            tvMonthlyCost = itemView.findViewById(R.id.tvAppMonthlyCost);
            tvBenefitBadge = itemView.findViewById(R.id.tvAppBenefitBadge);
            btnMinus = itemView.findViewById(R.id.btnQtyMinus);
            btnPlus = itemView.findViewById(R.id.btnQtyPlus);
        }
    }

    public static class ApplianceItem {
        public long id;
        public String name;
        public int quantity;
        public double wattage;
        public double hoursPerDay;
        public double allocatedHours;
        public double costContribution;
        public double dailySavingsMoney;
        public double R_k;
        public double gamma_k;
        public String statusNote;

        public ApplianceItem(long id, String name, int quantity, double wattage) {
            this(id, name, quantity, wattage, 8.0, 0.0);
        }

        public ApplianceItem(long id, String name, int quantity, double wattage, double hoursPerDay) {
            this(id, name, quantity, wattage, hoursPerDay, 0.0);
        }

        public ApplianceItem(long id, String name, int quantity, double wattage, double hoursPerDay, double allocatedHours) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.wattage = wattage;
            this.hoursPerDay = hoursPerDay > 0 ? hoursPerDay : 8.0;
            this.allocatedHours = allocatedHours;
        }
    }
}
