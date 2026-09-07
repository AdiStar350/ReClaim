package com.example.reclaim.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.reclaim.R;
import com.example.reclaim.databinding.ItemMyReportBinding;
import com.example.reclaim.model.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * The user's own reports, with edit and delete.
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

            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_item_placeholder)
                    .error(R.drawable.ic_item_placeholder)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(binding.imageThumbnail);

            String type = item.getType() != null ? item.getType() : "";
            if ("Lost".equalsIgnoreCase(type)) {
                binding.chipType.setText(R.string.type_lost);
                binding.chipType.setChipBackgroundColorResource(R.color.type_lost);
            } else {
                binding.chipType.setText(type.isEmpty() ? "" : context.getString(R.string.type_found));
                binding.chipType.setChipBackgroundColorResource(R.color.type_found);
            }
            // White text on the dark chip
            binding.chipType.setTextColor(context.getColor(R.color.text_on_accent));

            // Only the buttons do something. The card
            // itself is not clickable, so a stray tap
            // does not open the edit form.
            binding.btnEdit.setOnClickListener(v -> listener.onEdit(item));
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(item));
            binding.getRoot().setOnClickListener(null);
            binding.getRoot().setClickable(false);
        }
    }
}
