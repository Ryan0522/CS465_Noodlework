package com.example.roomues;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ChoreAdapter extends RecyclerView.Adapter<ChoreAdapter.ViewHolder> {

    private List<ChoreItem> chores;

    public ChoreAdapter(List<ChoreItem> chores) {
        this.chores = chores;
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
