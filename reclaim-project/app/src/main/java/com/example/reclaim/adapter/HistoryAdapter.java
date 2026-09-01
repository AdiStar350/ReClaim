package com.example.reclaim.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.reclaim.R;
import com.example.reclaim.databinding.ItemHistoryBinding;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the user's report history: items they reported and claims they made,
 * each tagged with its resolution status (pending / approved / declined).
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    public interface ContactClickListener {
        void onViewContact(@NonNull String claimId);
    }

    /** Resolution status of a history entry, with its badge styling. */
    public enum Status {
        PENDING(R.string.history_status_pending,
                R.color.status_pending_bg, R.color.status_pending_text),
        APPROVED(R.string.history_status_approved,
                R.color.status_open_bg, R.color.status_open_text),
        DECLINED(R.string.history_status_declined,
                R.color.status_closed_bg, R.color.status_closed_text);

        @StringRes final int labelRes;
        @ColorRes final int bgColorRes;
        @ColorRes final int textColorRes;

        Status(@StringRes int labelRes, @ColorRes int bgColorRes, @ColorRes int textColorRes) {
            this.labelRes = labelRes;
            this.bgColorRes = bgColorRes;
            this.textColorRes = textColorRes;
        }
    }

    /** A single row: a report the user made or a claim the user submitted. */
    public static class Entry {
        final String title;
        final String imageUrl;
        @StringRes
        final int roleLabelRes;
        final Status status;
        final String claimId;
        final boolean showContact;

        public Entry(String title, String imageUrl,
                     @StringRes int roleLabelRes, Status status) {
            this(title, imageUrl, roleLabelRes, status, null, false);
        }

        public Entry(String title, String imageUrl,
                     @StringRes int roleLabelRes, Status status,
                     String claimId, boolean showContact) {
            this.title = title;
            this.imageUrl = imageUrl;
            this.roleLabelRes = roleLabelRes;
            this.status = status;
            this.claimId = claimId;
            this.showContact = showContact;
        }
    }

    private final List<Entry> entries = new ArrayList<>();
    private final ContactClickListener contactClickListener;

    public HistoryAdapter(ContactClickListener contactClickListener) {
        this.contactClickListener = contactClickListener;
    }

    public void submit(List<Entry> newEntries) {
        entries.clear();
        if (newEntries != null) {
            entries.addAll(newEntries);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemHistoryBinding binding = ItemHistoryBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HistoryViewHolder(binding, contactClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(entries.get(position));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {

        private final ItemHistoryBinding binding;
        private final ContactClickListener listener;

        HistoryViewHolder(@NonNull ItemHistoryBinding binding,
                          ContactClickListener listener) {
            super(binding.getRoot());
            this.binding = binding;
            this.listener = listener;
        }

        void bind(Entry entry) {
            Context context = binding.getRoot().getContext();

            binding.textHistoryRole.setText(entry.roleLabelRes);
            binding.textHistoryTitle.setText(
                    !TextUtils.isEmpty(entry.title) ? entry.title : "Untitled report");

            Glide.with(context)
                    .load(!TextUtils.isEmpty(entry.imageUrl) ? entry.imageUrl : null)
                    .centerCrop()
                    .placeholder(R.drawable.ic_item_placeholder)
                    .error(R.drawable.ic_item_placeholder)
                    .into(binding.imageHistory);

            binding.textHistoryStatus.setText(entry.status.labelRes);
            binding.textHistoryStatus.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, entry.status.bgColorRes)));
            binding.textHistoryStatus.setTextColor(
                    ContextCompat.getColor(context, entry.status.textColorRes));

            boolean showContact = entry.showContact && !TextUtils.isEmpty(entry.claimId);
            binding.btnHistoryContact.setVisibility(showContact ? View.VISIBLE : View.GONE);
            if (showContact && listener != null) {
                binding.btnHistoryContact.setOnClickListener(
                        v -> listener.onViewContact(entry.claimId));
            } else {
                binding.btnHistoryContact.setOnClickListener(null);
            }
        }
    }
}
