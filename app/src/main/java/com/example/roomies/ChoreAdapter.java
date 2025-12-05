package com.example.roomies;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ChoreAdapter extends RecyclerView.Adapter<ChoreAdapter.ViewHolder> {

    private List<ChoreItem> chores;
    private String highlightName = null;

    public ChoreAdapter(List<ChoreItem> chores) {
        this.chores = chores;
    }

    public void setHighlightName(String name) {
        this.highlightName = name;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_chore, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ChoreItem item = chores.get(position);
        String name = item.getRoommate();
        String chore = item.getChore();

        holder.roommateText.setText(name);
        holder.choreText.setText(chore);

        // default colors
        holder.roommateText.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary)
        );
        if (holder.youBadge != null) {
            holder.youBadge.setVisibility(View.GONE);
        }

        // highlight current user row
        if (highlightName != null && highlightName.equals(name)) {
            holder.roommateText.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.user_highlight)
            );
            if (holder.youBadge != null) {
                holder.youBadge.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return chores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView roommateText, choreText, youBadge;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            roommateText = itemView.findViewById(R.id.roommateText);
            choreText = itemView.findViewById(R.id.choreText);
            youBadge = itemView.findViewById(R.id.youBadge);
        }
    }

    public void updateList(List<ChoreItem> newList) {
        this.chores = newList;
        notifyDataSetChanged();
    }

}
