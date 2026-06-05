package com.example.reclaim.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reclaim.databinding.ItemPendingClaimBinding;
import com.example.reclaim.model.PendingClaimResponse;

import java.util.ArrayList;
import java.util.List;

public class PendingClaimAdapter extends RecyclerView.Adapter<PendingClaimAdapter.ViewHolder> {

    public interface ReviewListener {
        void onApprove(PendingClaimResponse claim);

        void onReject(PendingClaimResponse claim);
    }

    private final List<PendingClaimResponse> claims = new ArrayList<>();
    private final ReviewListener listener;

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
            binding.textItemTitle.setText(claim.getItemTitle());
            binding.textValidationAnswer.setText(
                    "Answer: " + (claim.getValidationAnswer() != null
                            ? claim.getValidationAnswer() : ""));
            binding.btnApproveClaim.setOnClickListener(v -> listener.onApprove(claim));
            binding.btnRejectClaim.setOnClickListener(v -> listener.onReject(claim));
        }
    }
}
