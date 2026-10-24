package com.example.bachat;

import android.content.res.ColorStateList;
import android.graphics.Color;
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

public class EnvironmentAdapter extends RecyclerView.Adapter<EnvironmentAdapter.EnvViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Environment env);
    }

    private List<Environment> environments;
    private OnItemClickListener listener;

    public EnvironmentAdapter(List<Environment> environments, OnItemClickListener listener) {
        this.environments = environments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EnvViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_environment, parent, false);
        return new EnvViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EnvViewHolder holder, int position) {
        Environment env = environments.get(position);
        holder.tvName.setText(env.name);

        if (env.isAddButton) {
            holder.ivIcon.setImageResource(android.R.drawable.ic_input_add);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green)));
            holder.flIconBg.setBackgroundResource(R.drawable.bg_icon_circle_green);
            holder.tvName.setText("Add Space");
            holder.tvName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green));
            holder.tvSubtitle.setText("Create new environment");
            holder.tvSubtitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.accent_green_dark));
        } else {
            holder.tvName.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary));
            holder.tvSubtitle.setText("Tap to manage");
            holder.tvSubtitle.setTextColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.text_secondary));

            int iconRes = android.R.drawable.ic_menu_gallery;
            int colorRes = R.color.accent_purple;

            String lower = env.name.toLowerCase();
            if (lower.contains("home")) {
                iconRes = android.R.drawable.ic_menu_myplaces;
                colorRes = R.color.icon_home;
                holder.tvSubtitle.setText("Home space");
            } else if (lower.contains("office")) {
                iconRes = android.R.drawable.ic_menu_info_details;
                colorRes = R.color.icon_office;
                holder.tvSubtitle.setText("Workplace space");
            } else if (lower.contains("factory")) {
                iconRes = android.R.drawable.ic_menu_preferences;
                colorRes = R.color.icon_factory;
                holder.tvSubtitle.setText("Industrial space");
            } else if (lower.contains("shop")) {
                iconRes = android.R.drawable.ic_menu_agenda;
                colorRes = R.color.icon_shop;
                holder.tvSubtitle.setText("Commercial space");
            } else if (lower.contains("warehouse")) {
                iconRes = android.R.drawable.ic_menu_sort_by_size;
                colorRes = R.color.icon_warehouse;
                holder.tvSubtitle.setText("Storage space");
            } else {
                iconRes = android.R.drawable.ic_menu_edit;
                colorRes = R.color.icon_custom;
                holder.tvSubtitle.setText("Custom space");
            }

            int color = ContextCompat.getColor(holder.itemView.getContext(), colorRes);
            holder.ivIcon.setImageResource(iconRes);
            holder.ivIcon.setImageTintList(ColorStateList.valueOf(color));
            holder.flIconBg.setBackgroundResource(R.drawable.bg_icon_circle);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(env);
        });
    }

    @Override
    public int getItemCount() {
        return environments.size();
    }

    public static class EnvViewHolder extends RecyclerView.ViewHolder {
        FrameLayout flIconBg;
        ImageView ivIcon;
        TextView tvName;
        TextView tvSubtitle;

        public EnvViewHolder(@NonNull View itemView) {
            super(itemView);
            flIconBg = itemView.findViewById(R.id.flIconBg);
            ivIcon = itemView.findViewById(R.id.ivEnvIcon);
            tvName = itemView.findViewById(R.id.tvEnvName);
            tvSubtitle = itemView.findViewById(R.id.tvEnvSubtitle);
        }
    }

    public static class Environment {
        public long id;
        public String name;
        public boolean isAddButton;

        public Environment(long id, String name, boolean isAddButton) {
            this.id = id;
            this.name = name;
            this.isAddButton = isAddButton;
        }
    }
}
