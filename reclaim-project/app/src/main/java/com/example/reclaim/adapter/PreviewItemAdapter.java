package com.example.reclaim.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.reclaim.R;
import com.example.reclaim.databinding.ItemCardPreviewBinding;
import com.example.reclaim.model.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Small cards for the home preview rows.
 */
public class PreviewItemAdapter extends RecyclerView.Adapter<PreviewItemAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(@NonNull Item item);
    }

    private final List<Item> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public PreviewItemAdapter(@NonNull OnItemClickListener listener) {
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
        ItemCardPreviewBinding binding = ItemCardPreviewBinding.inflate(
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

        private final ItemCardPreviewBinding binding;

        ViewHolder(@NonNull ItemCardPreviewBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(@NonNull Item item) {
            android.content.Context ctx = binding.getRoot().getContext();

            binding.textItemTitle.setText(item.getTitle());
            binding.textItemLocation.setText(item.getLocation());

            String type = item.getType() != null ? item.getType() : "";
            if ("Lost".equalsIgnoreCase(type)) {
                binding.chipType.setText(R.string.type_lost);
                binding.chipType.setChipBackgroundColorResource(R.color.type_lost);
            } else {
                binding.chipType.setText(type.isEmpty() ? "" : ctx.getString(R.string.type_found));
                binding.chipType.setChipBackgroundColorResource(R.color.type_found);
            }
            binding.chipType.setTextColor(ctx.getColor(R.color.text_on_accent));

            Glide.with(ctx)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_item_placeholder)
                    .error(R.drawable.ic_item_placeholder)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(binding.imageItem);

            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
