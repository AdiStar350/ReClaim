```java
private ContactInfoDialog() {
}
```

What that function does: I stop anyone from making an instance of this helper.
What the function is responsible for: I keep the dialog as static show methods.
Where it works: I am not called. Java uses me when someone tries `new ContactInfoDialog()`.
Why I chose to write the code of the function the way its written: I made the constructor private so screens call show.
More: I use a private constructor. The class is final.

```java
public static void show(@NonNull Context context, @Nullable ContactResponse contact) {
    show(context, contact, null, null, R.string.contact_info_title);
}
```

What that function does: I show contact details with the default title and no live refresh.
What the function is responsible for: I am the short show helper.
Where it works: I did not find an app caller of this two-arg overload. Other screens use the longer ones.
Why I chose to write the code of the function the way its written: I forward to the full method so title and reloader stay in one place.
More: I pass null owner and reloader.

```java
public static void show(@NonNull Context context,
                        @Nullable ContactResponse contact,
                        @Nullable LifecycleOwner owner,
                        @Nullable ContactReloader reloader) {
    show(context, contact, owner, reloader, R.string.contact_info_title);
}
```

What that function does: I show contact details and refresh them when app data updates.
What the function is responsible for: I am the usual show helper with the default title.
Where it works: I am called by ItemDetailsActivity, ReportHistoryActivity, HomeFragment, and PendingClaimsSheet.
Why I chose to write the code of the function the way its written: I forward the default title so callers do not repeat the string.
More: I use R.string.contact_info_title.

```java
public static void show(@NonNull Context context,
                        @Nullable ContactResponse contact,
                        @Nullable LifecycleOwner owner,
                        @Nullable ContactReloader reloader,
                        int titleRes) {
    DialogContactInfoBinding binding = DialogContactInfoBinding.inflate(
            LayoutInflater.from(context));
    bindContact(context, binding, contact);

    AlertDialog dialog = new MaterialAlertDialogBuilder(context)
            .setTitle(titleRes)
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
```

What that function does: I inflate the contact layout, fill it, and optionally reload when data changes.
What the function is responsible for: I am the full contact dialog.
Where it works: I inflate dialog_contact_info.xml. Callers pass a ContactReloader that enqueues getClaimContact or getLostItemOwnerContact.
Why I chose to write the code of the function the way its written: I skip the current AppDataRefresh tick so opening the dialog does not refetch at once. I remove the observer on dismiss.
More: I use DialogContactInfoBinding, MaterialAlertDialogBuilder, and AppDataRefresh.live.

```java
public void onResponse(@NonNull Call<ContactResponse> call,
                       @NonNull Response<ContactResponse> response) {
    if (!dialog.isShowing()) {
        return;
    }
    if (response.isSuccessful()) {
        bindContact(context, binding, response.body());
    }
}
```

What that function does: I refill the dialog if it is still open.
What the function is responsible for: I am the Retrofit success path for a live reload.
Where it works: Retrofit calls me from the observer inside show.
Why I chose to write the code of the function the way its written: A closed dialog should not be updated.
More: I call bindContact.

```java
public void onFailure(@NonNull Call<ContactResponse> call,
                      @NonNull Throwable t) {
    // Keep what was already shown.
}
```

What that function does: I ignore a failed reload.
What the function is responsible for: I am the Retrofit failure path for a live reload.
Where it works: Retrofit calls me from the observer inside show.
Why I chose to write the code of the function the way its written: The first contact values stay on screen.
More: I leave the body empty.

```java
void enqueue(@NonNull Callback<ContactResponse> callback);
```

What that function does: I am the reload hook the host must implement.
What the function is responsible for: I let the dialog ask the screen to fetch contact again.
Where it works: show calls me when AppDataRefresh ticks. ItemDetailsActivity, ReportHistoryActivity, HomeFragment, and PendingClaimsSheet implement me.
Why I chose to write the code of the function the way its written: The dialog does not know which API path to call.
More: I take a Retrofit Callback<ContactResponse>.

```java
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
```

What that function does: I fill name, email, and phone, and I wire the copy buttons.
What the function is responsible for: I am the dialog content bind.
Where it works: I am called from show and from a successful reload.
Why I chose to write the code of the function the way its written: dash() shows an em dash when a field is empty.
More: I use ContactResponse getters and bindCopyButton.

```java
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
```

What that function does: I show a copy button only when there is text to copy.
What the function is responsible for: I hide empty copy actions.
Where it works: I am called from bindContact for email and phone.
Why I chose to write the code of the function the way its written: A gone button is cleaner than a dead tap.
More: I use TextUtils.isEmpty.

```java
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
```

What that function does: I copy the text. On older Android I also toast "copied".
What the function is responsible for: I am the copy action.
Where it works: I am called from bindCopyButton clicks.
Why I chose to write the code of the function the way its written: Android 13 already shows a system copied banner, so I skip the toast there.
More: I use ClipboardManager and ClipData.newPlainText.

```java
private static String dash(@NonNull Context context, String value) {
    return TextUtils.isEmpty(value)
            ? context.getString(R.string.placeholder_em_dash) : value;
}
```

What that function does: I return the value, or an em dash if it is empty.
What the function is responsible for: I keep empty contact rows from looking blank.
Where it works: I am called by bindContact.
Why I chose to write the code of the function the way its written: A dash is quieter than the word "none".
More: I use placeholder_em_dash.
