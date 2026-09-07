package com.example.reclaim.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.reclaim.R;
import com.example.reclaim.databinding.ItemCardBinding;
import com.example.reclaim.model.Item;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows a list of {@link Item} cards.
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private final List<Item> items;
    private final OnItemClickListener listener;

    /**
     * Called when a card is tapped.
     */
    public interface OnItemClickListener {
        /**
         * The tapped item.
         *
         * @param item the item
         */
        void onItemClick(@NonNull Item item);
    }

    /**
     * Builds an adapter from a list and a click listener.
     *
     * @param items    items to show
     * @param listener called when a card is tapped
     */
    public ItemAdapter(@NonNull List<Item> items, @NonNull OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemCardBinding binding = ItemCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ItemViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /**
     * Replaces the list and redraws.
     *
     * @param newItems the new items
     */
    public void updateItems(@NonNull List<Item> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    // ViewHolder

    /**
     * Views for one card.
     */
    class ItemViewHolder extends RecyclerView.ViewHolder {

        private final ItemCardBinding binding;

        ItemViewHolder(@NonNull ItemCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Fills the card from {@code item}.
         *
         * @param item the item to show
         */
        void bind(@NonNull Item item) {
            Context context = binding.getRoot().getContext();

            // Title, category, place
            binding.textItemTitle.setText(item.getTitle());
            binding.textItemCategory.setText(item.getCategory());
            binding.textItemLocation.setText(item.getLocation());

            // Lost / Found chip
            String type = item.getType();
            if ("Lost".equalsIgnoreCase(type)) {
                binding.chipType.setText(R.string.type_lost);
                // Dark red, white text
                binding.chipType.setChipBackgroundColorResource(R.color.type_lost);
            } else {
                binding.chipType.setText(type != null ? context.getString(R.string.type_found) : "");
                // Dark green, white text
                binding.chipType.setChipBackgroundColorResource(R.color.type_found);
            }
            binding.chipType.setTextColor(context.getColor(R.color.text_on_accent));

            // Open / Closed chip
            String status = item.getStatus();
            if ("CLOSED".equalsIgnoreCase(status)) {
                // Slate, cream text
                binding.chipStatus.setText(R.string.status_closed);
                binding.chipStatus.setChipBackgroundColorResource(R.color.status_closed_bg);
                binding.chipStatus.setTextColor(context.getColor(R.color.status_closed_text));
            } else {
                // Light green, dark text
                binding.chipStatus.setText(R.string.status_open);
                binding.chipStatus.setChipBackgroundColorResource(R.color.status_open_bg);
                binding.chipStatus.setTextColor(context.getColor(R.color.status_open_text));
            }

            // Photo
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_item_placeholder)
                    .error(R.drawable.ic_item_placeholder)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(binding.imageItem);

            // Tap
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
