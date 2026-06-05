package com.example.reclaim.ui.profile;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.reclaim.R;
import com.example.reclaim.adapter.ClaimSummaryAdapter;
import com.example.reclaim.adapter.ItemAdapter;
import com.example.reclaim.databinding.FragmentProfileBinding;
import com.example.reclaim.model.Claim;
import com.example.reclaim.model.ContactResponse;
import com.example.reclaim.model.Item;
import com.example.reclaim.model.User;
import com.example.reclaim.network.ReClaimApiService;
import com.example.reclaim.network.RetrofitClient;
import com.example.reclaim.network.TokenManager;
import com.example.reclaim.network.UpdateProfileRequest;
import com.example.reclaim.ui.auth.LoginActivity;
import com.example.reclaim.ui.dashboard.ItemNavigationHelper;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ReClaimApiService apiService;
    private ItemAdapter reportsAdapter;
    private ClaimSummaryAdapter claimsAdapter;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getApiService();

        reportsAdapter = new ItemAdapter(
                new java.util.ArrayList<>(),
                item -> startActivity(ItemNavigationHelper.createDetailsIntent(requireContext(), item)));
        claimsAdapter = new ClaimSummaryAdapter(this::loadContactForClaim);

        binding.recyclerMyReports.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMyReports.setAdapter(reportsAdapter);

        binding.recyclerMyClaims.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerMyClaims.setAdapter(claimsAdapter);

        setupClickListeners();
        loadProfileData();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileData();
    }

    private void setupClickListeners() {
        binding.btnEditProfile.setOnClickListener(v -> showEditProfileDialog());

        binding.btnReviewPendingClaims.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), PendingClaimsActivity.class);
            startActivity(intent);
        });

        binding.btnLogout.setOnClickListener(v -> {
            TokenManager.clearToken(requireContext());
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    private void loadProfileData() {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) {
            return;
        }

        apiService.getCurrentUser(authHeader).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    binding.textUserName.setText(currentUser.getName());
                    binding.textUserEmail.setText(currentUser.getEmail());
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        R.string.msg_profile_load_failed, Toast.LENGTH_SHORT).show();
            }
        });

        apiService.getMyItems(authHeader).enqueue(new Callback<List<Item>>() {
            @Override
            public void onResponse(@NonNull Call<List<Item>> call, @NonNull Response<List<Item>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    reportsAdapter.updateItems(response.body());
                    binding.textReportsCount.setText(String.valueOf(response.body().size()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Item>> call, @NonNull Throwable t) {
            }
        });

        apiService.getMyClaims(authHeader).enqueue(new Callback<List<Claim>>() {
            @Override
            public void onResponse(@NonNull Call<List<Claim>> call, @NonNull Response<List<Claim>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    claimsAdapter.updateClaims(response.body());
                    binding.textClaimsCount.setText(String.valueOf(response.body().size()));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Claim>> call, @NonNull Throwable t) {
            }
        });
    }

    private void showEditProfileDialog() {
        if (currentUser == null) {
            return;
        }

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_profile, null);
        TextInputLayout nameLayout = dialogView.findViewById(R.id.input_layout_name);
        TextInputLayout phoneLayout = dialogView.findViewById(R.id.input_layout_phone);
        TextInputEditText editName = dialogView.findViewById(R.id.edit_name);
        TextInputEditText editPhone = dialogView.findViewById(R.id.edit_phone);

        editName.setText(currentUser.getName());
        editPhone.setText(currentUser.getPhoneNumber());

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.btn_edit_profile)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save_profile, (dialog, which) -> {
                    String name = editName.getText() != null
                            ? editName.getText().toString().trim() : "";
                    String phone = editPhone.getText() != null
                            ? editPhone.getText().toString().trim() : "";

                    if (name.isEmpty()) {
                        nameLayout.setError(getString(R.string.msg_name_required));
                        return;
                    }

                    updateProfile(name, phone);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void updateProfile(String name, String phone) {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null || currentUser == null) {
            return;
        }

        UpdateProfileRequest request = new UpdateProfileRequest(
                name, currentUser.getEmail(), phone);
        apiService.updateCurrentUser(authHeader, request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(@NonNull Call<User> call, @NonNull Response<User> response) {
                if (response.isSuccessful() && response.body() != null) {
                    currentUser = response.body();
                    binding.textUserName.setText(currentUser.getName());
                    Toast.makeText(requireContext(),
                            R.string.msg_profile_updated, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(),
                            R.string.msg_profile_update_failed, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<User> call, @NonNull Throwable t) {
                Toast.makeText(requireContext(),
                        R.string.msg_profile_update_failed, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadContactForClaim(Claim claim) {
        String authHeader = TokenManager.getAuthHeader(requireContext());
        if (authHeader == null) {
            return;
        }

        apiService.getClaimContact(authHeader, claim.getId())
                .enqueue(new Callback<ContactResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ContactResponse> call,
                                           @NonNull Response<ContactResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ContactResponse contact = response.body();
                            new MaterialAlertDialogBuilder(requireContext())
                                    .setTitle(R.string.contact_details_title)
                                    .setMessage(getString(R.string.contact_details_message,
                                            contact.getName(),
                                            contact.getEmail(),
                                            contact.getPhoneNumber()))
                                    .setPositiveButton(android.R.string.ok, null)
                                    .show();
                        } else {
                            Toast.makeText(requireContext(),
                                    R.string.msg_contact_unavailable, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ContactResponse> call, @NonNull Throwable t) {
                        Toast.makeText(requireContext(),
                                R.string.msg_contact_unavailable, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
