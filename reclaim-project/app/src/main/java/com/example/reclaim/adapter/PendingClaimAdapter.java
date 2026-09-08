package com.example.reclaim.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.reclaim.R;
import com.example.reclaim.databinding.ItemPendingClaimBinding;
import com.example.reclaim.model.PendingClaimResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Cards for claims waiting on the item owner's review.
 */
public class PendingClaimAdapter extends RecyclerView.Adapter<PendingClaimAdapter.ViewHolder> {

    public interface ReviewListener {
        void onApprove(PendingClaimResponse claim);

        void onReject(PendingClaimResponse claim);
    }

    private final List<PendingClaimResponse> claims = new ArrayList<>();
    private final ReviewListener listener;
    @Nullable
    private String busyClaimId;

    public PendingClaimAdapter(ReviewListener listener) {
        this.listener = listener;
    }

    public void updateClaims(List<PendingClaimResponse> newClaims) {
        claims.clear();
        if (newClaims != null) {
            claims.addAll(newClaims);
        }
        notifyDataSetChanged();
    }

    @NonNull
    public List<PendingClaimResponse> getClaims() {
        return Collections.unmodifiableList(new ArrayList<>(claims));
    }

    public void setBusyClaimId(@Nullable String claimId) {
        this.busyClaimId = claimId;
        if (!claims.isEmpty()) {
            notifyItemRangeChanged(0, claims.size());
        }
    }

    /**
     * Removes one claim and animates the row out.
     */
    public boolean removeClaim(@Nullable String claimId) {
        if (claimId == null) {
            return false;
        }
        for (int i = 0; i < claims.size(); i++) {
            if (claimId.equals(claims.get(i).getClaimId())) {
                claims.remove(i);
                notifyItemRemoved(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes every pending claim for a report, including auto-rejected siblings.
     */
    public void removeClaimsForItem(@Nullable String itemId) {
        if (itemId == null) {
            return;
        }
        for (int i = claims.size() - 1; i >= 0; i--) {
            if (itemId.equals(claims.get(i).getItemId())) {
                claims.remove(i);
                notifyItemRemoved(i);
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPendingClaimBinding binding = ItemPendingClaimBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(claims.get(position));
    }

    @Override
    public int getItemCount() {
        return claims.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final ItemPendingClaimBinding binding;

        ViewHolder(ItemPendingClaimBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PendingClaimResponse claim) {
            Context context = binding.getRoot().getContext();

            String title = !TextUtils.isEmpty(claim.getItemTitle())
                    ? claim.getItemTitle()
                    : context.getString(R.string.placeholder_untitled_report);
            binding.textItemTitle.setText(title);

            String claimantName = !TextUtils.isEmpty(claim.getClaimantName())
                    ? claim.getClaimantName()
                    : context.getString(R.string.pending_claim_unknown_claimant);
            binding.textClaimant.setText(context.getString(R.string.label_claimant, claimantName));

            String answer = claim.getValidationAnswer() != null
                    ? claim.getValidationAnswer() : "";
            binding.textValidationAnswer.setText(
                    context.getString(R.string.label_claim_answer, answer));

            String imageUrl = claim.getItemImageUrl();
            Glide.with(context)
                    .load(!TextUtils.isEmpty(imageUrl) ? imageUrl : null)
                    .centerCrop()
                    .placeholder(R.drawable.ic_item_placeholder)
                    .error(R.drawable.ic_item_placeholder)
                    .into(binding.imageItemThumbnail);

            boolean enabled = busyClaimId == null;
            binding.btnApproveClaim.setEnabled(enabled);
            binding.btnRejectClaim.setEnabled(enabled);
            binding.btnApproveClaim.setOnClickListener(v -> listener.onApprove(claim));
            binding.btnRejectClaim.setOnClickListener(v -> listener.onReject(claim));
        }
    }
}
