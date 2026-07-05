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
 * Compact horizontal-scroll adapter for home-screen preview rows.
 * Uses {@code item_card_preview.xml}: fixed 200dp width, 112dp image height.
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
            binding.chipType.setText(type);
            if ("Lost".equalsIgnoreCase(type)) {
                binding.chipType.setChipBackgroundColorResource(R.color.type_lost);
            } else {
                binding.chipType.setChipBackgroundColorResource(R.color.type_found);
            }
            binding.chipType.setTextColor(ctx.getColor(R.color.text_on_accent));

            Glide.with(ctx)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(binding.imageItem);

            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
