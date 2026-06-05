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
 * RecyclerView adapter that binds a list of {@link Item} objects to
 * {@code item_card.xml} cards.
 * <p>
 * Uses View Binding ({@link ItemCardBinding}) for type-safe view access
 * and Glide for loading item images. Exposes an {@link OnItemClickListener}
 * callback interface so the hosting Activity/Fragment can handle navigation
 * to the Item Details screen.
 * </p>
 *
 * <h3>Usage example:</h3>
 * <pre>{@code
 * ItemAdapter adapter = new ItemAdapter(itemList, item -> {
 *     // Navigate to ItemDetailsActivity with item.getId()
 * });
 * recyclerView.setAdapter(adapter);
 * }</pre>
 */
public class ItemAdapter extends RecyclerView.Adapter<ItemAdapter.ItemViewHolder> {

    private final List<Item> items;
    private final OnItemClickListener listener;

    /**
     * Callback interface for item click events.
     */
    public interface OnItemClickListener {
        /**
         * Called when a user taps on an item card.
         *
         * @param item the {@link Item} that was clicked
         */
        void onItemClick(@NonNull Item item);
    }

    /**
     * Creates a new adapter with the given item list and click listener.
     *
     * @param items    the list of items to display; the adapter keeps a
     *                 reference to this list and reflects external mutations
     *                 when {@link #notifyDataSetChanged()} is called
     * @param listener callback invoked when an item card is tapped
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
     * Replaces the current data set with a new list of items and refreshes
     * the RecyclerView.
     *
     * @param newItems the updated list of items
     */
    public void updateItems(@NonNull List<Item> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    // ── ViewHolder ───────────────────────────────────────────────────────

    /**
     * Holds references to the views inside a single {@code item_card.xml}
     * via {@link ItemCardBinding}.
     */
    class ItemViewHolder extends RecyclerView.ViewHolder {

        private final ItemCardBinding binding;

        ItemViewHolder(@NonNull ItemCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        /**
         * Populates the card views with data from the given {@link Item}.
         *
         * @param item the item whose data should be displayed
         */
        void bind(@NonNull Item item) {
            Context context = binding.getRoot().getContext();

            // ── Text fields ──────────────────────────────────────────
            binding.textItemTitle.setText(item.getTitle());
            binding.textItemCategory.setText(item.getCategory());
            binding.textItemLocation.setText(item.getLocation());

            // ── Type chip (Lost / Found) ─────────────────────────────
            String type = item.getType();
            binding.chipType.setText(type != null ? type : "");
            if ("Lost".equalsIgnoreCase(type)) {
                binding.chipType.setChipBackgroundColorResource(R.color.md_theme_error);
                binding.chipType.setTextColor(
                        context.getColor(R.color.md_theme_onError));
            } else {
                binding.chipType.setChipBackgroundColorResource(R.color.md_theme_primary);
                binding.chipType.setTextColor(
                        context.getColor(R.color.md_theme_onPrimary));
            }

            // ── Status chip (OPEN / CLOSED) ──────────────────────────
            String status = item.getStatus();
            if ("CLOSED".equalsIgnoreCase(status)) {
                binding.chipStatus.setText(R.string.status_closed);
                binding.chipStatus.setChipBackgroundColorResource(
                        R.color.md_theme_secondaryContainer);
                binding.chipStatus.setTextColor(
                        context.getColor(R.color.md_theme_onSecondaryContainer));
            } else {
                binding.chipStatus.setText(R.string.status_open);
                binding.chipStatus.setChipBackgroundColorResource(
                        R.color.md_theme_tertiaryContainer);
                binding.chipStatus.setTextColor(
                        context.getColor(R.color.md_theme_onTertiaryContainer));
            }

            // ── Image loading via Glide ──────────────────────────────
            Glide.with(context)
                    .load(item.getImageUrl())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .error(R.drawable.ic_launcher_foreground)
                    .centerCrop()
                    .transition(DrawableTransitionOptions.withCrossFade(200))
                    .into(binding.imageItem);

            // ── Click listener ───────────────────────────────────────
            binding.getRoot().setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
