package com.example.reclaim.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reclaim.R;
import com.example.reclaim.databinding.ItemMyReportBinding;
import com.example.reclaim.model.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the user's own lost/found reports with edit and delete actions.
 */
public class MyReportAdapter extends RecyclerView.Adapter<MyReportAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(@NonNull Item item);

        void onDelete(@NonNull Item item);
    }

    private final List<Item> items = new ArrayList<>();
    private final Listener listener;

    public MyReportAdapter(@NonNull Listener listener) {
        this.listener = listener;
    }

    public void updateItems(@NonNull List<Item> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMyReportBinding binding = ItemMyReportBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemMyReportBinding binding;

        ViewHolder(@NonNull ItemMyReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Item item) {
            Context context = binding.getRoot().getContext();
            binding.textTitle.setText(item.getTitle());
            binding.textCategory.setText(item.getCategory());
            binding.textLocation.setText(item.getLocation());

            String type = item.getType() != null ? item.getType() : "";
            binding.chipType.setText(type);
            if ("Lost".equalsIgnoreCase(type)) {
                binding.chipType.setChipBackgroundColorResource(R.color.type_lost);
            } else {
                binding.chipType.setChipBackgroundColorResource(R.color.type_found);
            }
            // White text on dark red/green for sufficient contrast
            binding.chipType.setTextColor(context.getColor(R.color.text_on_accent));

            binding.btnEdit.setOnClickListener(v -> listener.onEdit(item));
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
            binding.getRoot().setOnClickListener(v -> listener.onEdit(item));
        }
    }
}
