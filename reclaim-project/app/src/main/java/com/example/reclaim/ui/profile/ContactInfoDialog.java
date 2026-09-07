package com.example.reclaim.ui.profile;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.example.reclaim.R;
import com.example.reclaim.data.AppDataRefresh;
import com.example.reclaim.databinding.DialogContactInfoBinding;
import com.example.reclaim.model.ContactResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Shows another person's name, email, and phone.
 */
public final class ContactInfoDialog {

    private ContactInfoDialog() {
    }

    public static void show(@NonNull Context context, @Nullable ContactResponse contact) {
        show(context, contact, null, null);
    }

    /**
     * Shows contact details. If {@code reloader} is set, it refreshes when app data updates.
     */
    public static void show(@NonNull Context context,
                            @Nullable ContactResponse contact,
                            @Nullable LifecycleOwner owner,
                            @Nullable ContactReloader reloader) {
        DialogContactInfoBinding binding = DialogContactInfoBinding.inflate(
                LayoutInflater.from(context));
        bindContact(context, binding, contact);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.contact_info_title)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, null)
                .create();

        if (owner != null && reloader != null) {
            final long baseline = AppDataRefresh.live().getValue() != null
                    ? AppDataRefresh.live().getValue() : 0L;
            Observer<Long> observer = tick -> {
                if (tick == null || tick <= baseline || !dialog.isShowing()) {
                    return;
                }
                reloader.enqueue(new Callback<ContactResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ContactResponse> call,
                                           @NonNull Response<ContactResponse> response) {
                        if (!dialog.isShowing()) {
                            return;
                        }
                        if (response.isSuccessful()) {
                            bindContact(context, binding, response.body());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ContactResponse> call,
                                          @NonNull Throwable t) {
                        // Keep what was already shown.
                    }
                });
            };
            AppDataRefresh.live().observe(owner, observer);
            dialog.setOnDismissListener(d -> AppDataRefresh.live().removeObserver(observer));
        }

        dialog.show();
    }

    @FunctionalInterface
    public interface ContactReloader {
        void enqueue(@NonNull Callback<ContactResponse> callback);
    }

    private static void bindContact(@NonNull Context context,
                                    @NonNull DialogContactInfoBinding binding,
                                    @Nullable ContactResponse contact) {
        String name = contact != null ? contact.getName() : null;
        String email = contact != null ? contact.getEmail() : null;
        String phone = contact != null ? contact.getPhoneNumber() : null;

        binding.textContactName.setText(dash(context, name));
        binding.textContactEmail.setText(dash(context, email));
        binding.textContactPhone.setText(dash(context, phone));

        bindCopyButton(context, binding.btnCopyEmail, email,
                context.getString(R.string.clip_label_email));
        bindCopyButton(context, binding.btnCopyPhone, phone,
                context.getString(R.string.clip_label_phone));
    }

    private static void bindCopyButton(@NonNull Context context,
                                       @NonNull View button,
                                       @Nullable String value,
                                       @NonNull String clipLabel) {
        if (TextUtils.isEmpty(value)) {
            button.setVisibility(View.GONE);
            button.setOnClickListener(null);
            return;
        }
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(v -> copyToClipboard(context, clipLabel, value));
    }

    private static void copyToClipboard(@NonNull Context context,
                                        @NonNull String label,
                                        @NonNull String value) {
        ClipboardManager clipboard = (ClipboardManager)
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.msg_copied, Toast.LENGTH_SHORT).show();
        }
    }

    private static String dash(@NonNull Context context, String value) {
        return TextUtils.isEmpty(value)
                ? context.getString(R.string.placeholder_em_dash) : value;
    }
}
