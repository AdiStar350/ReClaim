# fragment_profile_settings.xml

## Overview
This layout is the Profile / Settings tab: a header, a profile card, and grouped setting
rows (report history, notifications, password, logout, delete account), plus an app
version line and a spinner. It is inflated by `ProfileSettingsFragment` through
`FragmentProfileSettingsBinding` (hosted by `ProfileActivity` via `activity_fragment_host.xml`).

Key idea: a settings screen built from tappable rows grouped into **MaterialCardViews**.
See `docs/GLOSSARY.md`.

## Concepts used here
- **NestedScrollView** — the scrolling root for this long settings page. See `docs/GLOSSARY.md`.
- **LinearLayout** — vertical stacks and each horizontal setting row. See `docs/GLOSSARY.md`.
- **FrameLayout** — the colored header block. See `docs/GLOSSARY.md`.
- **MaterialCardView** — groups rows into cards; also the round avatar. See `docs/GLOSSARY.md`.
- **MaterialDivider** — the line between logout and delete rows. See `docs/GLOSSARY.md`.
- **ProgressBar** — the loading spinner. See `docs/GLOSSARY.md`.
- **ViewBinding** — `FragmentProfileSettingsBinding`. See `docs/GLOSSARY.md`.

## Screen structure

```
NestedScrollView (root)
└── LinearLayout (vertical)
    ├── FrameLayout (header: "Settings" title, text_greeting)
    └── LinearLayout (padded)
        ├── Card card_profile (avatar + name/email + chevron)
        ├── "Account" section title
        ├── Card: row_report_history
        ├── "Preferences" section title
        ├── Card: row_notifications
        ├── Card: row_password_update
        ├── "Other" section title
        ├── Card: row_logout + divider + row_delete_account
        ├── text_app_version
        └── ProgressBar progress
```

The root is a `NestedScrollView` because the page is a single long, scrolling list of
settings with no collapsing app bar.

## Components

### Header
```xml
<FrameLayout android:background="@drawable/bg_dashboard_header">
    <LinearLayout android:orientation="vertical">
        <com.google.android.material.textview.MaterialTextView
            android:id="@+id/text_greeting"
            android:text="@string/profile_settings_title" ... />
    </LinearLayout>
</FrameLayout>
```
- **What it shows:** the colored header with the screen title.
- **Why it's set up this way:** it reuses the shared dashboard header background for
  consistency with the other tabs.
- **How the code uses it:** the id `text_greeting` exists but the current code does not
  change it here (it stays the static "Settings" title).

### Profile card
```xml
<com.google.android.material.card.MaterialCardView
    android:id="@+id/card_profile"
    android:clickable="true"
    android:foreground="?attr/selectableItemBackground">
    <LinearLayout android:orientation="horizontal">
        <com.google.android.material.card.MaterialCardView> <!-- round avatar -->
            <ImageView android:id="@+id/image_settings_avatar" ... />
        </com.google.android.material.card.MaterialCardView>
        <LinearLayout android:layout_weight="1" android:orientation="vertical">
            <com.google.android.material.textview.MaterialTextView android:id="@+id/text_user_display_name" ... />
            <com.google.android.material.textview.MaterialTextView android:id="@+id/text_user_display_email" ... />
        </LinearLayout>
        <ImageView android:src="@drawable/ic_chevron_right" ... />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** a tappable card with the user's round avatar, name, email, and a
  right chevron — it opens the edit-profile screen.
- **Why it's set up this way:** the whole card is `clickable` with a ripple `foreground`
  so it behaves like a button; the text block uses `layout_weight="1"` so the name/email
  fill the middle and the chevron stays on the right. Name/email use `ellipsize`/`maxLines`
  so long values don't wrap.
- **How the code uses it:** `binding.cardProfile` opens edit profile; the code fills
  `binding.textUserDisplayName`, `binding.textUserDisplayEmail`, and loads the photo into
  `binding.imageSettingsAvatar`.

### Setting rows (report history / notifications / password)
```xml
<com.google.android.material.card.MaterialCardView style="@style/Widget.ReClaim.SettingsCard">
    <LinearLayout
        android:id="@+id/row_report_history"
        android:background="?attr/selectableItemBackground"
        android:clickable="true"
        android:orientation="horizontal">
        <ImageView android:src="@drawable/ic_settings_history" ... />
        <com.google.android.material.textview.MaterialTextView android:layout_weight="1" android:text="@string/settings_report_history" ... />
        <ImageView android:src="@drawable/ic_chevron_right" ... />
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
<!-- same shape for row_notifications and row_password_update -->
```
- **What it shows:** rows that each have a leading icon, a label, and a right chevron:
  Report history, Notifications, and Password update.
- **Why it's set up this way:** every row is one horizontal `LinearLayout` with the label
  taking `layout_weight="1"` so the icon and chevron pin to the edges. Making the row
  itself the clickable element (with a ripple background) is a common, lightweight
  settings pattern.
- **How the code uses it:** `binding.rowReportHistory`, `binding.rowNotifications`, and
  `binding.rowPasswordUpdate` each get a click listener that opens the matching screen or
  action.

### Logout and Delete account rows
```xml
<com.google.android.material.card.MaterialCardView style="@style/Widget.ReClaim.SettingsCard">
    <LinearLayout android:orientation="vertical">
        <LinearLayout android:id="@+id/row_logout" ...> ... </LinearLayout>
        <com.google.android.material.divider.MaterialDivider ... />
        <LinearLayout android:id="@+id/row_delete_account" ...> ... </LinearLayout>
    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```
- **What it shows:** two rows in one card — "Log out" and, below a divider, "Delete
  account" (drawn in a warning/red color).
- **Why it's set up this way:** grouping them in one card with a `MaterialDivider` keeps
  related "danger zone" actions together; the delete row uses the `type_lost` (red) tint
  to signal it is destructive.
- **How the code uses it:** `binding.rowLogout` logs out; `binding.rowDeleteAccount`
  opens the delete-account dialog (see `dialog_delete_account.xml`).

### App version and spinner
```xml
<com.google.android.material.textview.MaterialTextView android:id="@+id/text_app_version" ... />
<ProgressBar android:id="@+id/progress" android:visibility="gone" />
```
- **What it shows:** a centered app version line and a hidden loading spinner.
- **Why it's set up this way:** the version sits at the bottom as a quiet footer; the
  spinner is `gone` until an action (like delete) runs.
- **How the code uses it:** the code sets `binding.textAppVersion` (e.g. "Version 1.0")
  and toggles `binding.progress` during network actions.
