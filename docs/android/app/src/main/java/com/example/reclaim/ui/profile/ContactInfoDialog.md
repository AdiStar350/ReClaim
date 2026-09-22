# ContactInfoDialog.java

## Overview
This is a small utility class (not an Activity) that shows a Material **AlertDialog** with another person’s name, email, and phone, plus copy buttons. Callers can optionally pass a **LifecycleOwner** and a `ContactReloader` so the dialog refreshes when `AppDataRefresh` fires.

## Concepts used here
- **AlertDialog / MaterialAlertDialogBuilder** — popup dialog with a custom view.
- **ViewBinding** — `DialogContactInfoBinding` for the dialog layout. See `docs/GLOSSARY.md`.
- **LiveData / Observer** — watches `AppDataRefresh.live()` for refresh ticks. See `docs/GLOSSARY.md`.
- **Retrofit Callback** — the reloader enqueues a new contact fetch when data refreshes. See `docs/GLOSSARY.md`.
- **Clipboard** — copies email/phone for the user.

## Libraries and imports
- **Android framework**: ClipboardManager, ClipData, Toast, LayoutInflater, Build version checks.
- **AndroidX**: AlertDialog, LifecycleOwner, Observer, annotations.
- **Material**: `MaterialAlertDialogBuilder`.
- **Retrofit**: `Call`, `Callback`, `Response` for the optional reload path.
- **Project**: `ContactResponse`, `AppDataRefresh`, `DialogContactInfoBinding`.

## Fields
None — the class is a `final` holder of static helpers; the private constructor prevents instantiation.

## Functions

### `private ContactInfoDialog()`
```java
private ContactInfoDialog() {
}
```
- **What it does:** Empty private constructor so nothing can `new` this class.
- **Where it's called:** Never from outside; blocks accidental construction.
- **Why it's written this way:** All API methods are static utility entry points.
- **Step by step:**
  1. Declares a private no-arg constructor with an empty body.

### `private static String dash(Context context, String value)`
```java
private static String dash(@NonNull Context context, String value) {
    return TextUtils.isEmpty(value)
            ? context.getString(R.string.placeholder_em_dash) : value;
}
```
- **What it does:** Returns the value, or an em-dash placeholder string if blank.
- **Where it's called:** From `bindContact` for name, email, and phone texts.
- **Why it's written this way:** Empty contact fields still show something readable instead of a blank line.
- **Step by step:**
  1. If value is empty, return the placeholder string resource.
  2. Otherwise return the value as-is.

### `private static void copyToClipboard(Context context, String label, String value)`
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
- **What it does:** Puts text on the clipboard; toasts “copied” only on Android versions below 13 (Tiramisu), because newer OS already shows a system copy confirmation.
- **Where it's called:** From `bindCopyButton`’s click listener.
- **Why it's written this way:** Avoids double “copied” feedback on Android 13+.
- **Step by step:**
  1. Get `ClipboardManager`; return if null.
  2. Set primary clip with the label and value.
  3. If SDK &lt; Tiramisu, show a Toast.

### `private static void bindCopyButton(Context context, View button, String value, String clipLabel)`
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
- **What it does:** Hides the copy button when there is nothing to copy; otherwise shows it and copies on tap.
- **Where it's called:** From `bindContact` for email and phone buttons.
- **Why it's written this way:** No copy affordance for missing fields.
- **Step by step:**
  1. If value empty: hide button, clear listener, return.
  2. Otherwise show button and set click to `copyToClipboard`.

### `private static void bindContact(Context context, DialogContactInfoBinding binding, ContactResponse contact)`
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
- **What it does:** Fills the three text fields and wires the two copy buttons from a `ContactResponse` (or dashes if null).
- **Where it's called:** From `show` initially, and again when a reload succeeds.
- **Why it's written this way:** One place updates both first paint and live refresh.
- **Step by step:**
  1. Read name/email/phone from contact (or null).
  2. Set texts via `dash`.
  3. Bind copy buttons for email and phone.

### `public interface ContactReloader` — `void enqueue(Callback<ContactResponse> callback)`
```java
@FunctionalInterface
public interface ContactReloader {
    void enqueue(@NonNull Callback<ContactResponse> callback);
}
```
- **What it does:** Lets the caller supply “how to fetch contact again” without the dialog knowing which API method or claim id to use.
- **Where it's called:** Implemented as lambdas in `ReportHistoryActivity`, `PendingClaimsSheet`, `HomeFragment`, and `ItemDetailsActivity` when they call `show` with a reloader; invoked from the `AppDataRefresh` observer inside `show`.
- **Why it's written this way:** Different screens fetch contact with different claim ids; the dialog only needs a way to enqueue a Callback.
- **Step by step:**
  1. Caller implements `enqueue` to start a Retrofit call with the given Callback.
  2. Dialog calls it when a refresh tick arrives.

### `public static void show(...)` overloads
```java
public static void show(@NonNull Context context, @Nullable ContactResponse contact) {
    show(context, contact, null, null, R.string.contact_info_title);
}

public static void show(@NonNull Context context,
                        @Nullable ContactResponse contact,
                        @Nullable LifecycleOwner owner,
                        @Nullable ContactReloader reloader) {
    show(context, contact, owner, reloader, R.string.contact_info_title);
}

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
- **What it does:** Builds and shows the contact dialog; optionally observes app-data refresh and reloads contact while the dialog is open.
- **Where it's called:** `ReportHistoryActivity.showOwnerContact`, `PendingClaimsSheet.revealClaimantContact`, `HomeFragment` (claimant/reporter contact), `ItemDetailsActivity` (contact after claim approval path). Shorter overloads default the title and skip reload.
- **Why it's written this way:** Overloads keep simple callers short. Refresh only runs for ticks after the dialog opened (`baseline`), and the observer is removed on dismiss so it does not leak. Failed reload keeps the old text.
- **Step by step:**
  1. Inflate binding and `bindContact` once.
  2. Create Material dialog with title, custom view, OK button.
  3. If owner + reloader are set: remember current refresh tick, observe newer ticks, enqueue reload, re-bind on success.
  4. On dismiss, remove the observer.
  5. `dialog.show()`.
