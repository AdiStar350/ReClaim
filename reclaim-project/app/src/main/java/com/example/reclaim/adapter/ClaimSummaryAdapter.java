package com.example.reclaim.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.reclaim.databinding.ItemClaimSummaryBinding;
import com.example.reclaim.model.Claim;

import java.util.ArrayList;
import java.util.List;

public class ClaimSummaryAdapter extends RecyclerView.Adapter<ClaimSummaryAdapter.ViewHolder> {

    public interface ContactListener {
        void onViewContact(Claim claim);
    }

    private final List<Claim> claims = new ArrayList<>();
    private final ContactListener contactListener;

    public ClaimSummaryAdapter(ContactListener contactListener) {
        this.contactListener = contactListener;
    }

    public void updateClaims(List<Claim> newClaims) {
        claims.clear();
        if (newClaims != null) {
            claims.addAll(newClaims);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemClaimSummaryBinding binding = ItemClaimSummaryBinding.inflate(
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

        private final ItemClaimSummaryBinding binding;

        ViewHolder(ItemClaimSummaryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Claim claim) {
            binding.textClaimItemId.setText("Item: " + claim.getItemId());
            binding.textClaimStatus.setText("Status: " + claim.getStatus());

            boolean approved = "APPROVED".equalsIgnoreCase(claim.getStatus());
            binding.btnViewContact.setVisibility(approved ? View.VISIBLE : View.GONE);
            binding.btnViewContact.setOnClickListener(v -> contactListener.onViewContact(claim));
        }
    }
}
