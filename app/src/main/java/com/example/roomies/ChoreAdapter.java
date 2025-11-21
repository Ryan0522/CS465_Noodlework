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
        holder.roommateText.setText(item.getRoommate());
        holder.choreText.setText(item.getChore());

        // default colors
        holder.roommateText.setTextColor(
                ContextCompat.getColor(holder.itemView.getContext(), R.color.text_primary)
        );

        // highlight current user row
        if (highlightName != null && highlightName.equals(item.getRoommate())) {
            holder.roommateText.setTextColor(
                    ContextCompat.getColor(holder.itemView.getContext(), R.color.user_highlight)
            );
            // add a dot cue before the roommate name
            if (!holder.roommateText.getText().toString().startsWith("• ")) {
                holder.roommateText.setText("• " + holder.roommateText.getText());
            }
        }
    }

    @Override
    public int getItemCount() {
        return chores.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView roommateText, choreText;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            roommateText = itemView.findViewById(R.id.roommateText);
            choreText = itemView.findViewById(R.id.choreText);
        }
    }

    public void updateList(List<ChoreItem> newList) {
        this.chores = newList;
        notifyDataSetChanged();
    }

}
