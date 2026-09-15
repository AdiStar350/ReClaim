```xml
<androidx.core.widget.NestedScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="@color/md_theme_background"
    android:clipToPadding="false"
    android:fillViewport="true"
    android:overScrollMode="never">
```

What that function does: I am the profile settings tab.
What the function is responsible for: I am the root of fragment_profile_settings.
Where it works: ProfileSettingsFragment inflates me. DashboardActivity and ProfileActivity host the fragment.
Why I chose to write the code of the function the way its written: NestedScrollView matches home's header plus list of rows.
More: I use overScrollMode never.

```xml
        <FrameLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@drawable/bg_dashboard_header"
            android:clipChildren="false"
            android:clipToPadding="false"
            android:paddingTop="@dimen/screen_padding_top"
            android:paddingBottom="@dimen/screen_padding_bottom"
            android:paddingEnd="@dimen/screen_padding_horizontal"
            android:paddingStart="@dimen/screen_padding_horizontal">
```

What that function does: I am the dark settings title bar.
What the function is responsible for: I show Profile.
Where it works: text_greeting in this layout holds profile_settings_title. ProfileSettingsFragment does not replace it with a name.
Why I chose to write the code of the function the way its written: Same header drawable as home so the tabs match.
More: I use bg_dashboard_header.

```xml
            <com.google.android.material.card.MaterialCardView
                android:id="@+id/card_profile"
                style="@style/Widget.ReClaim.SettingsCard"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:clickable="true"
                android:focusable="true"
                android:foreground="?attr/selectableItemBackground">
```

What that function does: I show avatar, name, and email, and I open edit profile.
What the function is responsible for: I am the profile row.
Where it works: ProfileSettingsFragment loads image_settings_avatar, text_user_display_name, and text_user_display_email. Click starts EditProfileActivity.
Why I chose to write the code of the function the way its written: Name and email sit beside the photo so this card is both a summary and a button.
More: I use Widget.ReClaim.SettingsCard.

```xml
                    <LinearLayout
                        android:id="@+id/row_report_history"
                        android:layout_width="match_parent"
                        android:layout_height="@dimen/space_56"
                        android:background="?attr/selectableItemBackground"
                        android:clickable="true"
                        android:focusable="true"
                        android:gravity="center_vertical"
                        android:orientation="horizontal"
                        android:paddingStart="@dimen/space_16"
                        android:paddingEnd="@dimen/space_12">
```

What that function does: I am the report-history row.
What the function is responsible for: I open closed-report history.
Where it works: ProfileSettingsFragment starts ReportHistoryActivity from row_report_history.
Why I chose to write the code of the function the way its written: A 56dp row matches the other settings lines.
More: I use ic_settings_history.

```xml
                <LinearLayout
                    android:id="@+id/row_notifications"
                    android:layout_width="match_parent"
                    android:layout_height="@dimen/space_56"
                    android:background="?attr/selectableItemBackground"
                    android:clickable="true"
                    android:focusable="true"
                    android:gravity="center_vertical"
                    android:orientation="horizontal"
                    android:paddingStart="@dimen/space_16"
                    android:paddingEnd="@dimen/space_12">
```

What that function does: I am the notifications row.
What the function is responsible for: I open system notification settings.
Where it works: ProfileSettingsFragment wires row_notifications.
Why I chose to write the code of the function the way its written: The app does not host its own toggle screen. The system page is enough.
More: I use ic_settings_notifications.

```xml
                <LinearLayout
                    android:id="@+id/row_password_update"
                    android:layout_width="match_parent"
                    android:layout_height="@dimen/space_56"
                    android:background="?attr/selectableItemBackground"
                    android:clickable="true"
                    android:focusable="true"
                    android:gravity="center_vertical"
                    android:orientation="horizontal"
                    android:paddingStart="@dimen/space_16"
                    android:paddingEnd="@dimen/space_12">
```

What that function does: I am the change-password row.
What the function is responsible for: I start a reset email for the signed-in user.
Where it works: ProfileSettingsFragment wires row_password_update.
Why I chose to write the code of the function the way its written: Reusing forgot-password avoids a second password form.
More: I use ic_settings_password.

```xml
                    <LinearLayout
                        android:id="@+id/row_logout"
                        android:layout_width="match_parent"
                        android:layout_height="@dimen/space_56"
                        android:background="?attr/selectableItemBackground"
                        android:clickable="true"
                        android:focusable="true"
                        android:gravity="center_vertical"
                        android:orientation="horizontal"
                        android:paddingStart="@dimen/space_16"
                        android:paddingEnd="@dimen/space_12">
```

What that function does: I am the log-out row.
What the function is responsible for: I sign the user out.
Where it works: ProfileSettingsFragment wires row_logout.
Why I chose to write the code of the function the way its written: Logout stays in the Other card above delete.
More: I use ic_settings_logout.

```xml
                    <LinearLayout
                        android:id="@+id/row_delete_account"
                        android:layout_width="match_parent"
                        android:layout_height="@dimen/space_56"
                        android:background="?attr/selectableItemBackground"
                        android:clickable="true"
                        android:focusable="true"
                        android:gravity="center_vertical"
                        android:orientation="horizontal"
                        android:paddingStart="@dimen/space_16"
                        android:paddingEnd="@dimen/space_12">
```

What that function does: I am the delete-account row in warning color.
What the function is responsible for: I open dialog_delete_account.xml.
Where it works: ProfileSettingsFragment.showDeleteAccountDialog inflates that dialog.
Why I chose to write the code of the function the way its written: type_lost red marks this as destructive.
More: I use ic_settings_delete.

```xml
            <com.google.android.material.textview.MaterialTextView
                android:id="@+id/text_app_version"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="@dimen/space_28"
                android:gravity="center"
                android:textAppearance="?attr/textAppearanceBodySmall"
                android:textColor="@color/text_hint" />

            <ProgressBar
                android:id="@+id/progress"
                android:layout_width="wrap_content"
                android:layout_height="wrap_content"
                android:layout_gravity="center_horizontal"
                android:layout_marginTop="@dimen/space_12"
                android:indeterminateTint="@color/brand_accent"
                android:visibility="gone" />
```

What that function does: I show the app version and a spinner for profile loads.
What the function is responsible for: Version text and loading.
Where it works: ProfileSettingsFragment sets text_app_version and shows progress.
Why I chose to write the code of the function the way its written: Version is a quiet footer. Spinner starts gone.
More: I use text_hint.
