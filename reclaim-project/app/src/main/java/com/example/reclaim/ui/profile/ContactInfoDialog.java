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

import com.example.reclaim.R;
import com.example.reclaim.databinding.DialogContactInfoBinding;
import com.example.reclaim.model.ContactResponse;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Shared dialog for another user's name, email, and phone. Used after an
 * approved claim, from Report History, and from lost-item report details.
 */
public final class ContactInfoDialog {

    private ContactInfoDialog() {
    }

    public static void show(@NonNull Context context, @Nullable ContactResponse contact) {
        String name = contact != null ? contact.getName() : null;
        String email = contact != null ? contact.getEmail() : null;
        String phone = contact != null ? contact.getPhoneNumber() : null;

        DialogContactInfoBinding binding = DialogContactInfoBinding.inflate(
                LayoutInflater.from(context));
        binding.textContactName.setText(dash(name));
        binding.textContactEmail.setText(dash(email));
        binding.textContactPhone.setText(dash(phone));

        bindCopyButton(context, binding.btnCopyEmail, email, "email");
        bindCopyButton(context, binding.btnCopyPhone, phone, "phone");

        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.contact_info_title)
                .setView(binding.getRoot())
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private static void bindCopyButton(@NonNull Context context,
                                       @NonNull View button,
                                       @Nullable String value,
                                       @NonNull String clipLabel) {
        if (TextUtils.isEmpty(value)) {
            button.setVisibility(View.GONE);
            return;
        }
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

    private static String dash(String value) {
        return TextUtils.isEmpty(value) ? "—" : value;
    }
}
