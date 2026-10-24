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

    public ApplianceAdapter(List<ApplianceItem> appliances, OnItemClickListener listener, OnQuantityChangeListener qtyListener) {
        this.appliances = appliances;
        this.listener = listener;
        this.qtyListener = qtyListener;
    }

    public void setTariffRate(double rate) {
        this.tariffRate = rate > 0 ? rate : 8.0;
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

        double hours = app.hoursPerDay > 0 ? app.hoursPerDay : 8.0;
        holder.tvWattage.setText(String.format(Locale.getDefault(), "%.0fW · %.0fh/day", app.wattage, hours));

        // Calculate monthly cost
        double monthlyKwh = (app.wattage * app.quantity * hours * 30.0) / 1000.0;
        double monthlyCost = monthlyKwh * tariffRate;
        holder.tvMonthlyCost.setText(String.format(Locale.getDefault(), "₹ %,.0f / mo", monthlyCost));

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
        } else if (n.contains("refrigerator") || n.contains("fridge")) {
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
        TextView tvName, tvQty, tvWattage, tvMonthlyCost;
        ImageView btnMinus, btnPlus;

        public AppViewHolder(@NonNull View itemView) {
            super(itemView);
            flIconBg = itemView.findViewById(R.id.flAppIconBg);
            ivIcon = itemView.findViewById(R.id.ivAppIcon);
            tvName = itemView.findViewById(R.id.tvAppName);
            tvQty = itemView.findViewById(R.id.tvAppQty);
            tvWattage = itemView.findViewById(R.id.tvAppWattage);
            tvMonthlyCost = itemView.findViewById(R.id.tvAppMonthlyCost);
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

        public ApplianceItem(long id, String name, int quantity, double wattage) {
            this(id, name, quantity, wattage, 8.0);
        }

        public ApplianceItem(long id, String name, int quantity, double wattage, double hoursPerDay) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.wattage = wattage;
            this.hoursPerDay = hoursPerDay > 0 ? hoursPerDay : 8.0;
        }
    }
}
